package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Fluid.Graph.FluidGraph;

import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Celsius;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class FluidGraphTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidGraph graph = data.graph();
        if (!graph.isDirty()) return;

        graph.rebuildIfDirty(level, data.store());
    }
}
