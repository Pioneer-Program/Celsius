package cute.ame.celsius.Fluid.Level;

import cute.ame.celsius.Fluid.Physics.FluidHeat;
import cute.ame.celsius.Fluid.Physics.RoomScanner;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;

import cute.ame.celsius.Config;
import cute.ame.celsius.Core.Event.NodeLifecycleEvent;
import cute.ame.celsius.Core.Event.RoomSealEvent;
import cute.ame.celsius.Core.Thermal.Residency;
import cute.ame.celsius.Fluid.Data.FluidConstants;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Helper.Airtight;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RoomLevelData extends SavedData
{
    public static final String FILE_ID = "pioneer_rooms";

    private static final String K_NODES = "nodes";
    private static final String K_ORIGINS = "origins";

    private static final SavedData.Factory<RoomLevelData> FACTORY = new SavedData.Factory<>(RoomLevelData::new, RoomLevelData::load, null);

    private static RoomScanner scanner;

    public record Room(int nodeId, long origin, long[] cells, boolean sealed) {}

    private final Int2ObjectOpenHashMap<Room> rooms = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap cellToNode = new Long2IntOpenHashMap();
    private final IntArrayFIFOQueue dirty = new IntArrayFIFOQueue();
    private final IntOpenHashSet queued = new IntOpenHashSet();

    private final IntOpenHashSet unindexed = new IntOpenHashSet();

    private RoomLevelData()
    {
        cellToNode.defaultReturnValue(FluidNodeStore.INVALID);
    }

    public static RoomLevelData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static @Nullable RoomLevelData getIfPresent(ServerLevel level)
    {
        return level.getDataStorage().get(FACTORY, FILE_ID);
    }

    private static RoomScanner scanner()
    {
        int cap = Config.ROOM_MAX_BLOCKS.get();
        if (scanner == null || scanner.capacity() != cap) scanner = new RoomScanner(cap);

        return scanner;
    }

    public int roomCount()
    {
        return rooms.size();
    }

    public @Nullable Room room(int nodeId)
    {
        return rooms.get(nodeId);
    }

    public Iterable<Room> rooms()
    {
        return rooms.values();
    }

    public int nodeAt(BlockPos pos)
    {
        return cellToNode.get(RoomScanner.pack(pos.getX(), pos.getY(), pos.getZ()));
    }

    public int attach(ServerLevel level, BlockPos origin)
    {
        int existing = nodeAt(origin);
        if (existing != FluidNodeStore.INVALID) return existing;

        if (!unindexed.isEmpty())
        {
            indexLoaded(level);

            existing = nodeAt(origin);
            if (existing != FluidNodeStore.INVALID) return existing;
        }

        RoomScanner.Result scan = scan(level, origin);
        if (scan.isEmpty()) return FluidNodeStore.INVALID;

        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();

        int nodeId = store.create((float) scan.volumeLitres(Config.ROOM_LITRES_PER_BLOCK.get()), FluidConstants.DEFAULT_TEMPERATURE_K);
        store.setFlag(nodeId, FluidNodeStore.FLAG_OPEN, !scan.sealed());

        Room room = new Room(nodeId, RoomScanner.pack(origin.getX(), origin.getY(), origin.getZ()), scan.cells(), scan.sealed());
        rooms.put(nodeId, room);
        indexCells(room);

        fluids.setDirty();
        setDirty();
        return nodeId;
    }

    public boolean relocate(ServerLevel level, int nodeId, BlockPos origin)
    {
        Room room = rooms.get(nodeId);
        if (room == null) return false;

        if (nodeAt(origin) == nodeId) return true;

        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();

        if (!store.alive(nodeId))
        {
            rooms.remove(nodeId);
            unindexCells(room);
            queued.remove(nodeId);
            unindexed.remove(nodeId);
            setDirty();
            return false;
        }

        return rebind(level, fluids, store, room, origin);
    }

    public boolean remove(ServerLevel level, int nodeId)
    {
        Room room = rooms.remove(nodeId);
        if (room == null) return false;

        unindexCells(room);
        queued.remove(nodeId);
        unindexed.remove(nodeId);

        FluidLevelData fluids = FluidLevelData.get(level);
        fluids.store().destroy(nodeId);
        fluids.setDirty();
        setDirty();
        return true;
    }

    public void onBlockChanged(BlockPos pos)
    {
        if (cellToNode.isEmpty()) return;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        markDirtyAt(RoomScanner.pack(x, y, z));
        markDirtyAt(RoomScanner.pack(x + 1, y, z));
        markDirtyAt(RoomScanner.pack(x - 1, y, z));
        markDirtyAt(RoomScanner.pack(x, y + 1, z));
        markDirtyAt(RoomScanner.pack(x, y - 1, z));
        markDirtyAt(RoomScanner.pack(x, y, z + 1));
        markDirtyAt(RoomScanner.pack(x, y, z - 1));
    }

    private void markDirtyAt(long packed)
    {
        int nodeId = cellToNode.get(packed);
        if (nodeId == FluidNodeStore.INVALID) return;

        markDirty(nodeId);
    }

    public void markDirty(int nodeId)
    {
        if (!rooms.containsKey(nodeId)) return;
        if (queued.add(nodeId)) dirty.enqueue(nodeId);
    }

    public int pendingRescans() { return queued.size(); }

    public void tick(ServerLevel level)
    {
        if (queued.isEmpty())
        {
            if (!dirty.isEmpty()) dirty.clear();
            return;
        }

        int budget = Config.ROOM_RESCANS_PER_TICK.get();
        while (budget > 0 && !dirty.isEmpty())
        {
            int nodeId = dirty.dequeueInt();
            if (!queued.remove(nodeId)) continue;

            rescan(level, nodeId);
            budget--;
        }
    }

    private void rescan(ServerLevel level, int nodeId)
    {
        Room room = rooms.get(nodeId);
        if (room == null) return;

        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();

        if (!store.alive(nodeId))
        {
            rooms.remove(nodeId);
            unindexCells(room);
            unindexed.remove(nodeId);
            setDirty();
            return;
        }

        BlockPos origin = new BlockPos(RoomScanner.unpackX(room.origin()), RoomScanner.unpackY(room.origin()), RoomScanner.unpackZ(room.origin()));
        if (!Residency.isLoaded(level, origin))
        {
            markDirty(nodeId);
            return;
        }

        if (foldIfOwned(level, store, nodeId, origin)) return;

        rebind(level, fluids, store, room, origin);
    }

    private boolean rebind(ServerLevel level, FluidLevelData fluids, FluidNodeStore store, Room room, BlockPos origin)
    {
        int nodeId = room.nodeId();

        RoomScanner.Result scan = scan(level, origin);
        if (scan.isEmpty())
        {
            remove(level, nodeId);
            return false;
        }

        unindexCells(room);
        unindexed.remove(nodeId);

        Room next = new Room(nodeId, RoomScanner.pack(origin.getX(), origin.getY(), origin.getZ()), scan.cells(), scan.sealed());
        rooms.put(nodeId, next);
        indexCells(next);

        float oldVolume = store.volume(nodeId);
        float newVolume = (float) scan.volumeLitres(Config.ROOM_LITRES_PER_BLOCK.get());

        if (oldVolume > 0.0f && newVolume != oldVolume && store.moles(nodeId) > 0.0f)
        {
            float scale = newVolume / oldVolume;
            for (int s = 0; s < store.getStride(); s++)
            {
                float mol = store.amount(nodeId, s);
                if (mol > 0.0f) store.setAmount(nodeId, s, mol * scale);
            }
        }

        store.setVolume(nodeId, newVolume);
        store.setFlag(nodeId, FluidNodeStore.FLAG_OPEN, !scan.sealed());

        fluids.setDirty();
        setDirty();

        if (room.sealed() != scan.sealed()) NeoForge.EVENT_BUS.post(new RoomSealEvent(level, nodeId, origin, scan.sealed()));
        return true;
    }

    private void indexLoaded(ServerLevel level)
    {
        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();
        BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos();

        for (int nodeId : unindexed.toIntArray())
        {
            Room room = rooms.get(nodeId);
            if (room == null || !store.alive(nodeId))
            {
                if (room != null) rooms.remove(nodeId);
                unindexed.remove(nodeId);
                queued.remove(nodeId);
                setDirty();
                continue;
            }

            origin.set(RoomScanner.unpackX(room.origin()), RoomScanner.unpackY(room.origin()), RoomScanner.unpackZ(room.origin()));
            if (!Residency.isLoaded(level, origin)) continue;

            queued.remove(nodeId);
            if (foldIfOwned(level, store, nodeId, origin)) continue;

            rebind(level, fluids, store, room, origin.immutable());
        }
    }

    private boolean foldIfOwned(ServerLevel level, FluidNodeStore store, int nodeId, BlockPos origin)
    {
        int owner = nodeAt(origin);
        if (owner == FluidNodeStore.INVALID || owner == nodeId) return false;

        if (store.alive(owner) && store.moles(nodeId) > 0.0f)
        {
            float[] molarHeat = FluidSpecies.active().molarHeatRaw();
            double capacityOwner = FluidHeat.capacity(store, owner, molarHeat);
            double capacityFolded = FluidHeat.capacity(store, nodeId, molarHeat);
            double total = capacityOwner + capacityFolded;

            if (total > 0.0) store.setTemperature(owner, (float) ((store.temperature(owner) * capacityOwner + store.temperature(nodeId) * capacityFolded) / total));

            int stride = store.getStride();
            for (int s = 0; s < stride; s++)
            {
                float mol = store.amount(nodeId, s);
                if (mol > 0.0f) store.add(owner, s, mol);
            }
        }

        if (store.alive(owner) && store.alive(nodeId)) NeoForge.EVENT_BUS.post(new NodeLifecycleEvent.Merged(level, nodeId, store.handle(nodeId), owner, store.handle(owner)));
        remove(level, nodeId);
        FluidLevelData.get(level).graph().invalidate();
        return true;
    }

    private RoomScanner.Result scan(ServerLevel level, BlockPos origin)
    {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        return scanner().scan(origin.getX(), origin.getY(), origin.getZ(),
        (x, y, z) ->
        {
            cursor.set(x, y, z);
            if (!Residency.isLoaded(level, cursor)) return false;

            return !Airtight.seals(level, cursor, level.getBlockState(cursor));
        },
        level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
    }

    private void indexCells(Room room)
    {
        for (long cell : room.cells()) cellToNode.put(cell, room.nodeId());
    }

    private void unindexCells(Room room)
    {
        for (long cell : room.cells()) if (cellToNode.get(cell) == room.nodeId()) cellToNode.remove(cell);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        int[] nodes = new int[rooms.size()];
        long[] origins = new long[rooms.size()];

        int i = 0;
        for (Room room : rooms.values())
        {
            nodes[i] = room.nodeId();
            origins[i] = room.origin();
            i++;
        }

        tag.putIntArray(K_NODES, nodes);
        tag.putLongArray(K_ORIGINS, origins);
        return tag;
    }

    private static RoomLevelData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        RoomLevelData data = new RoomLevelData();

        int[] nodes = tag.getIntArray(K_NODES);
        long[] origins = tag.getLongArray(K_ORIGINS);

        int count = Math.min(nodes.length, origins.length);
        for (int i = 0; i < count; i++)
        {
            data.rooms.put(nodes[i], new Room(nodes[i], origins[i], new long[0], false));
            data.queued.add(nodes[i]);
            data.unindexed.add(nodes[i]);
            data.dirty.enqueue(nodes[i]);
        }

        return data;
    }
}