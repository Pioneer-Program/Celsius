package cute.ame.celsius.Thermal.Helper;

import cute.ame.celsius.Config;
import cute.ame.celsius.Core.Thermal.BlockHeatSink;
import cute.ame.celsius.Core.Thermal.BlockTemperature;
import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Physics.FluidHeat;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.celsius.Thermal.Data.MaterialTable;
import cute.ame.celsius.Thermal.Level.ThermalLevelData;
import cute.ame.celsius.Thermal.Registry.ThermalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class ThermalActuator
{
    private static final Direction[] DIRECTIONS = Direction.values();

    public static float heat(ServerLevel level, BlockPos pos, BlockState state, float setpointK, float joules)
    {
        float capacity = capacityOf(state);
        if (capacity <= 0.0f || joules <= 0.0f) return 0.0f;

        ThermalLevelData data = ThermalLevelData.get(level);

        float current = data.kelvinAt(pos);
        if (Float.isNaN(current)) current = BlockTemperature.dimensionDefault(level);

        float owed = (setpointK - current) * capacity;
        if (owed <= 0.0f) return 0.0f;

        float spent = Math.min(owed, joules);
        float change = spent / capacity;
        if (change < Config.THERMAL_EPSILON_K.get()) return 0.0f;

        data.force(pos, current + change);
        return spent;
    }

    public static double pump(ServerLevel level, BlockPos pos, int node, float setpointK, float joules)
    {
        if (node == FluidNodeStore.INVALID || joules <= 0.0f) return 0.0;

        FluidLevelData fluids = FluidLevelData.getIfPresent(level);
        if (fluids == null) return 0.0;

        FluidNodeStore store = fluids.store();
        if (!store.alive(node)) return 0.0;

        float[] molarHeat = FluidSpecies.active().molarHeatRaw();
        double fluidCapacity = FluidHeat.capacity(store, node, molarHeat);
        if (fluidCapacity <= 0.0) return 0.0;

        double owed = (setpointK - store.temperature(node)) * fluidCapacity;
        if (owed >= 0.0) return 0.0;

        float moved = FluidHeat.addJoules(store, node, Math.max(owed, -joules), molarHeat);
        if (moved == 0.0f) return 0.0;

        fluids.touch(node);

        double removed = -moved * fluidCapacity;
        if (removed <= 0.0) return 0.0;

        BlockHeatSink.inject(level, pos, removed);
        return removed;
    }

    public static int adjacentNode(ServerLevel level, BlockPos pos)
    {
        FluidLevelData fluids = FluidLevelData.getIfPresent(level);
        if (fluids == null) return FluidNodeStore.INVALID;

        FluidNodeStore store = fluids.store();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Direction face : DIRECTIONS)
        {
            cursor.setWithOffset(pos, face);
            if (!(level.getBlockEntity(cursor) instanceof FluidVesselBlockEntity vessel)) continue;

            int node = store.resolve(vessel.getNodeHandle());
            if (node != FluidNodeStore.INVALID && store.moles(node) > 0.0f) return node;
        }

        return FluidNodeStore.INVALID;
    }

    private static float capacityOf(BlockState state)
    {
        MaterialTable table = ThermalMaterials.table();
        int material = ThermalMaterials.indexOf(state);

        return table.isValid(material) ? table.volumetricHeat(material) : 0.0f;
    }
}