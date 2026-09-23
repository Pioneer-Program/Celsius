package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Core.Event.PhaseChangeEvent;
import cute.ame.celsius.Core.Thermal.Ambient;
import cute.ame.celsius.Fluid.Data.AmbientState;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Fluid.Graph.FluidGraph;
import cute.ame.celsius.Fluid.Helper.Containment;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Physics.ComponentPartition;
import cute.ame.celsius.Fluid.Physics.ContainmentCache;
import cute.ame.celsius.Fluid.Physics.FluidPhase;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.celsius.Celsius;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class FluidPhaseTickEvents
{
    private static final ContainmentCache CONTAINMENT = new ContainmentCache();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        if (store.getLiveCount() == 0) return;

        SpeciesTable table = FluidSpecies.active();
        FluidGraph graph = data.graph();
        ComponentPartition.Result partition = graph.partition();
        AmbientState ambient = Ambient.of(level);

        CONTAINMENT.rebuild(store, graph, partition);

        int[] nodeOrder = partition.nodeOrder();
        int[] nodeOffsets = partition.nodeOffsets();

        for (int c = 0, components = partition.count(); c < components; c++)
        {
            if (graph.isAsleep(c)) continue;

            for (int i = nodeOffsets[c], to = nodeOffsets[c + 1]; i < to; i++)
            {
                update(level, data, store, table, ambient, nodeOrder[i], c);
            }
        }

        sweepOrphans(level, data, store, table, ambient, graph, partition, nodeOrder);
    }

    private static void sweepOrphans(ServerLevel level, FluidLevelData data, FluidNodeStore store, SpeciesTable table, AmbientState ambient, FluidGraph graph, ComponentPartition.Result partition, int[] nodeOrder)
    {
        if (!graph.isDirty() && nodeOrder.length == store.getLiveCount()) return;

        int high = store.getHighWater();
        for (int id = 0; id < high; id++)
        {
            if (!store.alive(id) || partition.componentOf(id) >= 0) continue;

            update(level, data, store, table, ambient, id, -1);
        }
    }

    private static void update(ServerLevel level, FluidLevelData data, FluidNodeStore store, SpeciesTable table, AmbientState ambient, int id, int component)
    {
        if (!store.alive(id)) return;

        double containment = store.isLiquid(id) ? Containment.of(store, table, ambient, id, component, CONTAINMENT) : FluidPhase.DEFAULT_CONTAINMENT_P;
        if (!FluidPhase.update(store, id, table, containment)) return;

        data.touch(id);
        NeoForge.EVENT_BUS.post(new PhaseChangeEvent(level, id, store.isLiquid(id)));
    }
}
