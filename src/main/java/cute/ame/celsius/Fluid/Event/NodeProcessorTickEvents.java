package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Config;
import cute.ame.celsius.Core.Event.SpeciesReloadEvent;
import cute.ame.celsius.Core.Node.NodeProcessor;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Graph.FluidGraph;
import cute.ame.celsius.Fluid.Helper.NodeCursor;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.celsius.Fluid.Physics.ComponentPartition;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.celsius.Fluid.Registry.NodeProcessors;
import cute.ame.celsius.Celsius;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class NodeProcessorTickEvents
{
    private static final NodeCursor CURSOR = new NodeCursor();

    private static boolean warned;

    @SubscribeEvent
    public static void onSpeciesReload(SpeciesReloadEvent event)
    {
        NodeProcessors.rebuild(event.table());
        warned = false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        int count = NodeProcessors.count();
        if (count == 0) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidGraph graph = data.graph();
        if (graph.awakeCount() == 0) return;

        ComponentPartition.Result partition = graph.partition();
        int components = partition.count();
        if (components == 0) return;

        FluidNodeStore store = data.store();
        int[] nodeOrder = partition.nodeOrder();
        int[] nodeOffsets = partition.nodeOffsets();
        long time = level.getGameTime();
        int budget = Config.NODE_PROCESSOR_BUDGET.get();
        CURSOR.bind(data, RoomLevelData.getIfPresent(level), FluidSpecies.active().molarHeatRaw());

        try
        {
            for (int p = 0; p < count; p++)
            {
                if (time % NodeProcessors.period(p) != 0) continue;

                NodeProcessor processor = NodeProcessors.get(p);
                int[] required = NodeProcessors.required(p);

                for (int c = 0; c < components; c++)
                {
                    if (graph.isAsleep(c)) continue;

                    for (int i = nodeOffsets[c], to = nodeOffsets[c + 1]; i < to; i++)
                    {
                        int id = nodeOrder[i];
                        if (!store.alive(id) || !contains(store, id, required)) continue;

                        if (budget-- <= 0)
                        {
                            if (!warned)
                            {
                                warned = true;
                                Celsius.LOGGER.warn("[Celsius] node processor budget of {} reached in {}, remaining nodes skipped this tick", Config.NODE_PROCESSOR_BUDGET.get(), level.dimension().location());
                            }
                            return;
                        }

                        processor.process(level, CURSOR.at(id));
                    }
                }
            }
        }
        finally
        {
            CURSOR.release();
        }
    }

    private static boolean contains(FluidNodeStore store, int id, int[] required)
    {
        for (int s : required)
        {
            if (store.amount(id, s) <= 0.0f) return false;
        }
        return true;
    }
}
