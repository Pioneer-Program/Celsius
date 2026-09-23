package cute.ame.celsius.Fluid.Helper;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Graph.FluidGraph;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.celsius.Celsius;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.function.UnaryOperator;

public final class VesselRelocation implements AutoCloseable
{
    private static final long NO_HANDLE = -1L;

    private static VesselRelocation active;

    private final ServerLevel level;
    private final FluidLevelData data;

    private final LongOpenHashSet departures;
    private final Long2LongOpenHashMap inbound;
    private final Long2IntOpenHashMap cabins;

    private long[] arrivals;
    private int arrivalCount;

    private VesselRelocation(ServerLevel level, FluidLevelData data, int expected)
    {
        this.level = level;
        this.data = data;
        this.departures = new LongOpenHashSet(expected);
        this.inbound = new Long2LongOpenHashMap(expected);
        this.cabins = new Long2IntOpenHashMap(8);
        this.arrivals = new long[Math.max(expected, 16)];

        this.inbound.defaultReturnValue(NO_HANDLE);
        this.cabins.defaultReturnValue(FluidNodeStore.INVALID);
    }

    public static VesselRelocation open(ServerLevel level, Iterable<BlockPos> blocks, UnaryOperator<BlockPos> route, int expected)
    {
        FluidLevelData data = FluidLevelData.get(level);
        FluidNodeStore store = data.store();
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);

        VesselRelocation scope = new VesselRelocation(level, data, expected);

        for (BlockPos pos : blocks)
        {
            if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel)) continue;

            long handle = vessel.getNodeHandle();
            long target = route.apply(pos).asLong();

            scope.departures.add(pos.asLong());
            scope.inbound.put(target, store.resolve(handle) == FluidNodeStore.INVALID ? NO_HANDLE : handle);

            if (rooms == null) continue;

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FluidVesselBlock block)) continue;

            BlockPos mouth = block.roomMouth(pos, state);
            if (mouth == null) continue;

            int room = rooms.nodeAt(mouth);
            if (room != FluidNodeStore.INVALID) scope.cabins.put(target, room);
        }

        active = scope;
        return scope;
    }

    public static boolean depart(ServerLevel level, BlockPos pos)
    {
        VesselRelocation scope = active;
        return scope != null && scope.level == level && scope.departures.contains(pos.asLong());
    }

    public static boolean arrive(ServerLevel level, BlockPos pos)
    {
        VesselRelocation scope = active;
        if (scope == null || scope.level != level) return false;

        if (scope.arrivalCount == scope.arrivals.length) scope.arrivals = Arrays.copyOf(scope.arrivals, scope.arrivalCount << 1);
        scope.arrivals[scope.arrivalCount++] = pos.asLong();
        return true;
    }

    @Override
    public void close()
    {
        active = null;

        FluidNodeStore store = data.store();
        FluidGraph graph = data.graph();

        ObjectIterator<Long2LongMap.Entry> owners = inbound.long2LongEntrySet().fastIterator();
        while (owners.hasNext())
        {
            Long2LongMap.Entry entry = owners.next();

            long handle = entry.getLongValue();
            if (handle == NO_HANDLE || store.resolve(handle) == FluidNodeStore.INVALID) continue;

            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel)) continue;

            vessel.setNodeHandle(handle);
            graph.track(pos);
        }

        if (!cabins.isEmpty())
        {
            RoomLevelData rooms = RoomLevelData.get(level);
            ObjectIterator<Long2IntMap.Entry> it = cabins.long2IntEntrySet().fastIterator();

            while (it.hasNext())
            {
                Long2IntMap.Entry entry = it.next();

                BlockPos pos = BlockPos.of(entry.getLongKey());
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof FluidVesselBlock block)) continue;

                BlockPos mouth = block.roomMouth(pos, state);
                if (mouth != null) rooms.relocate(level, entry.getIntValue(), mouth);
            }
        }

        Celsius.LOGGER.debug("Relocation closed in {}: {} departed, {} handles carried, {} room(s), {} arrived in scope", level.dimension().location(), departures.size(), inbound.size(), cabins.size(), arrivalCount);

        for (int i = 0; i < arrivalCount; i++)
        {
            BlockPos pos = BlockPos.of(arrivals[i]);
            if (level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel) VesselNodes.settle(level, data, pos, vessel);
        }

        graph.invalidate();
        data.setDirty();
    }
}