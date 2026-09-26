package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Fluid.Physics.FluidFilter;
import cute.ame.celsius.Fluid.Physics.FluidSolver;

import cute.ame.celsius.Config;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.celsius.Fluid.Physics.ComponentPartition;
import cute.ame.celsius.Fluid.Graph.FluidGraph;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Celsius;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class FluidSolverTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        FluidGraph graph = data.graph();

        graph.rebuildIfDirty(level, store);
        if (graph.awakeCount() == 0) return;

        ComponentPartition.Result partition = graph.partition();
        if (partition.count() == 0) return;

        float[] molarHeat = FluidSpecies.active().molarHeatRaw();
        int sweeps = Config.FLUID_SWEEPS_PER_TICK.get();
        int patience = Config.FLUID_SLEEP_TICKS.get();
        float thermal = Config.FLUID_THERMAL_CONDUCTANCE.get().floatValue();
        double potentialEpsilon = Config.FLUID_SLEEP_EPSILON.get();
        double temperatureEpsilon = Config.FLUID_SLEEP_TEMPERATURE_EPSILON.get();

        int[] edgeOrder = partition.edgeOrder();
        int[] edgeOffsets = partition.edgeOffsets();
        int[] edgeA = graph.edgeARaw();
        int[] edgeB = graph.edgeBRaw();
        float[] conductance = graph.conductanceRaw();
        float[] boost = graph.boostRaw();

        boolean moved = filter(store, graph, partition, molarHeat);

        for (int c = 0; c < partition.count(); c++)
        {
            if (graph.isAsleep(c)) continue;

            int from = edgeOffsets[c];
            int to = edgeOffsets[c + 1];

            if (from == to)
            {
                graph.settle(c, 0.0, patience);
                continue;
            }

            double activity = 0.0;
            for (int pass = 0; pass < sweeps; pass++)
            {
                activity = FluidSolver.sweep(store, edgeOrder, from, to, edgeA, edgeB, conductance, boost, molarHeat, thermal, potentialEpsilon, temperatureEpsilon);
            }

            graph.settle(c, activity, patience);
            if (activity >= FluidGraph.SETTLED) moved = true;
        }

        if (moved) data.setDirty();
    }

    private static boolean filter(FluidNodeStore store, FluidGraph graph, ComponentPartition.Result partition, float[] molarHeat)
    {
        int links = graph.linkCount();
        if (links == 0) return false;

        int[] linkA = graph.linkARaw();
        int[] linkB = graph.linkBRaw();
        int[] linkSpecies = graph.linkSpeciesRaw();
        float[] linkRate = graph.linkRateRaw();
        float minimum = Config.FLUID_FILTER_MIN_MOL.get().floatValue();

        boolean moved = false;
        for (int l = 0; l < links; l++)
        {
            int source = linkA[l];
            if (graph.isAsleep(partition.componentOf(source))) continue;

            float passed = FluidFilter.pass(store, source, linkB[l], linkSpecies[l], linkRate[l], molarHeat);
            if (passed < minimum) continue;

            graph.wakeNode(source);
            graph.wakeNode(linkB[l]);
            moved = true;
        }
        return moved;
    }
}
