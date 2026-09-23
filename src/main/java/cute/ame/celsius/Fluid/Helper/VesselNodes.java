package cute.ame.celsius.Fluid.Helper;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;

import cute.ame.celsius.Config;
import cute.ame.celsius.Core.Event.NodeLifecycleEvent;
import cute.ame.celsius.Core.Thermal.Ambient;
import cute.ame.celsius.Core.Thermal.Residency;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class VesselNodes
{
    private static final Direction[] DIRECTIONS = Direction.values();

    public static void onLoaded(ServerLevel level, BlockPos pos, FluidVesselBlockEntity be)
    {
        FluidLevelData data = FluidLevelData.get(level);

        data.graph().track(pos);
        if (VesselRelocation.arrive(level, pos)) return;

        settle(level, data, pos, be);
    }

    static void settle(ServerLevel level, FluidLevelData data, BlockPos pos, FluidVesselBlockEntity be)
    {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidVesselBlock block)) return;

        BlockPos mouth = block.roomMouth(pos, state);
        if (mouth != null)
        {
            RoomLevelData.get(level).attach(level, mouth);
            data.graph().invalidate();
        }

        FluidNodeStore store = data.store();
        if (store.resolve(be.getNodeHandle()) != FluidNodeStore.INVALID) return;

        if (!block.merges())
        {
            int nodeId = store.create(block.getVolumeLitres(), ambientTemperature(level));
            be.setNodeHandle(store.handle(nodeId));
            data.setDirty();
            return;
        }

        adopt(level, data, pos, block);
    }

    public static void onUnloaded(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data != null) data.graph().forget(pos);
    }

    public static void onRemoved(net.minecraft.world.level.Level rawLevel, BlockPos pos, BlockState state)
    {
        if (!(rawLevel instanceof ServerLevel level)) return;
        if (!(state.getBlock() instanceof FluidVesselBlock block)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        if (VesselRelocation.depart(level, pos))
        {
            data.graph().forget(pos);
            return;
        }

        FluidNodeStore store = data.store();
        data.graph().forget(pos);

        BlockPos mouth = block.roomMouth(pos, state);
        if (mouth != null) detachRoom(level, data, pos, mouth);

        FluidVesselBlockEntity be = vesselAt(level, pos);
        int nodeId = be == null ? FluidNodeStore.INVALID : store.resolve(be.getNodeHandle());
        if (nodeId == FluidNodeStore.INVALID) return;

        float volume = store.volume(nodeId);
        float removed = Math.min(block.getVolumeLitres(), volume);

        scaleMatter(store, nodeId, (volume - removed) / volume);

        if (volume - removed <= 1.0f)
        {
            store.destroy(nodeId);
            data.setDirty();
            return;
        }

        store.setVolume(nodeId, volume - removed);

        if (block.merges()) splitIfDisconnected(level, data, pos, block, nodeId);

        data.graph().invalidate();
        data.setDirty();
    }

    private static void detachRoom(ServerLevel level, FluidLevelData data, BlockPos pos, BlockPos mouth)
    {
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms == null) return;

        int nodeId = rooms.nodeAt(mouth);
        if (nodeId == FluidNodeStore.INVALID) return;

        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (Direction direction : DIRECTIONS)
        {
            side.setWithOffset(mouth, direction);
            if (side.equals(pos)) continue;

            BlockState neighbour = level.getBlockState(side);
            if (!(neighbour.getBlock() instanceof FluidVesselBlock other)) continue;

            BlockPos otherMouth = other.roomMouth(side, neighbour);
            if (otherMouth != null && rooms.nodeAt(otherMouth) == nodeId) return;
        }

        rooms.remove(level, nodeId);
        data.graph().invalidate();
    }

    private static void adopt(ServerLevel level, FluidLevelData data, BlockPos pos, FluidVesselBlock block)
    {
        FluidNodeStore store = data.store();

        IntOpenHashSet neighbours = new IntOpenHashSet();
        for (Direction direction : Direction.values())
        {
            BlockPos side = pos.relative(direction);
            if (!sameCluster(level, side, block)) continue;

            FluidVesselBlockEntity be = vesselAt(level, side);
            if (be == null) continue;

            int nodeId = store.resolve(be.getNodeHandle());
            if (nodeId != FluidNodeStore.INVALID) neighbours.add(nodeId);
        }

        int nodeId;
        if (neighbours.isEmpty())
        {
            nodeId = store.create(block.getVolumeLitres(), ambientTemperature(level));
        }
        else
        {
            nodeId = fold(level, store, neighbours);
            store.setVolume(nodeId, store.volume(nodeId) + block.getVolumeLitres());
        }

        long handle = store.handle(nodeId);
        for (long packed : cluster(level, pos, block))
        {
            FluidVesselBlockEntity be = vesselAt(level, BlockPos.of(packed));
            if (be != null) be.setNodeHandle(handle);
        }

        data.graph().invalidate();
        data.setDirty();
    }

    private static int fold(ServerLevel level, FluidNodeStore store, IntOpenHashSet nodes)
    {
        int survivor = FluidNodeStore.INVALID;
        SpeciesTable table = FluidSpecies.active();

        for (int nodeId : nodes)
        {
            if (survivor == FluidNodeStore.INVALID)
            {
                survivor = nodeId;
                continue;
            }

            double capacityA = heatCapacity(store, survivor, table);
            double capacityB = heatCapacity(store, nodeId, table);
            double total = capacityA + capacityB;

            if (total > 0.0)
            {
                store.setTemperature(survivor, (float) ((store.temperature(survivor) * capacityA + store.temperature(nodeId) * capacityB) / total));
            }

            int stride = store.getStride();
            for (int s = 0; s < stride; s++)
            {
                float mol = store.amount(nodeId, s);
                if (mol > 0.0f) store.add(survivor, s, mol);
            }

            store.setVolume(survivor, store.volume(survivor) + store.volume(nodeId));
            NeoForge.EVENT_BUS.post(new NodeLifecycleEvent.Merged(level, nodeId, store.handle(nodeId), survivor, store.handle(survivor)));
            store.destroy(nodeId);
        }

        return survivor;
    }

    private static void splitIfDisconnected(ServerLevel level, FluidLevelData data, BlockPos pos, FluidVesselBlock block, int nodeId)
    {
        FluidNodeStore store = data.store();

        List<LongOpenHashSet> pieces = new ArrayList<>(6);
        LongOpenHashSet seen = new LongOpenHashSet();

        for (Direction direction : Direction.values())
        {
            BlockPos side = pos.relative(direction);
            if (!sameCluster(level, side, block)) continue;
            if (seen.contains(side.asLong())) continue;

            LongOpenHashSet piece = cluster(level, side, block);
            seen.addAll(piece);
            pieces.add(piece);
        }

        if (pieces.size() <= 1) return;

        double totalVolume = 0.0;
        for (LongOpenHashSet piece : pieces) totalVolume += piece.size() * (double) block.getVolumeLitres();
        if (totalVolume <= 0.0) return;

        float temperature = store.temperature(nodeId);
        int stride = store.getStride();
        int[] parts = new int[pieces.size() - 1];
        float[] amounts = new float[stride];
        for (int s = 0; s < stride; s++) amounts[s] = store.amount(nodeId, s);

        for (int i = 0; i < pieces.size(); i++)
        {
            LongOpenHashSet piece = pieces.get(i);
            float pieceVolume = (float) (piece.size() * (double) block.getVolumeLitres());
            double share = pieceVolume / totalVolume;

            int target;
            if (i == 0)
            {
                target = nodeId;
                store.setVolume(target, pieceVolume);
            }
            else
            {
                target = store.create(pieceVolume, temperature);
                parts[i - 1] = target;
            }

            for (int s = 0; s < stride; s++) store.setAmount(target, s, (float) (amounts[s] * share));

            long handle = store.handle(target);
            for (long packed : piece)
            {
                FluidVesselBlockEntity be = vesselAt(level, BlockPos.of(packed));
                if (be != null) be.setNodeHandle(handle);
            }
        }

        NeoForge.EVENT_BUS.post(new NodeLifecycleEvent.Split(level, nodeId, store.handle(nodeId), parts));
    }

    private static LongOpenHashSet cluster(ServerLevel level, BlockPos origin, FluidVesselBlock block)
    {
        LongOpenHashSet visited = new LongOpenHashSet();
        if (!sameCluster(level, origin, block)) return visited;

        int cap = Config.VESSEL_MAX_CLUSTER.get();
        LongArrayList frontier = new LongArrayList();

        visited.add(origin.asLong());
        frontier.add(origin.asLong());

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!frontier.isEmpty() && visited.size() < cap)
        {
            BlockPos current = BlockPos.of(frontier.removeLong(frontier.size() - 1));

            for (Direction direction : Direction.values())
            {
                cursor.setWithOffset(current, direction);
                long packed = cursor.asLong();

                if (visited.contains(packed)) continue;
                if (!sameCluster(level, cursor, block)) continue;

                visited.add(packed);
                frontier.add(packed);

                if (visited.size() >= cap) break;
            }
        }

        return visited;
    }

    private static boolean sameCluster(ServerLevel level, BlockPos pos, FluidVesselBlock block)
    {
        if (!block.merges()) return false;
        if (!Residency.isLoaded(level, pos)) return false;

        return level.getBlockState(pos).is(block);
    }

    private static @Nullable FluidVesselBlockEntity vesselAt(ServerLevel level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel ? vessel : null;
    }

    private static double heatCapacity(FluidNodeStore store, int nodeId, SpeciesTable table)
    {
        int stride = Math.min(store.getStride(), table.size());
        double sum = 0.0;

        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol <= 0.0f) continue;

            sum += mol * table.specificHeat(s) * table.molarMass(s) * 0.001;
        }

        return sum;
    }

    private static void scaleMatter(FluidNodeStore store, int nodeId, float scale)
    {
        if (scale >= 1.0f) return;

        int stride = store.getStride();
        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol > 0.0f) store.setAmount(nodeId, s, scale <= 0.0f ? 0.0f : mol * scale);
        }
    }

    private static float ambientTemperature(ServerLevel level)
    {
        return Ambient.of(level).temperatureK();
    }
}