package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Core.Thermal.Ambient;
import cute.ame.celsius.Fluid.Data.AmbientState;
import cute.ame.celsius.Fluid.Physics.AmbientEqualizer;
import cute.ame.celsius.Config;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Celsius;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class AmbientTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        int open = store.getOpenCount();
        if (open == 0) return;

        AmbientState ambient = Ambient.of(level);
        float rate = Config.AMBIENT_EQUALIZE_RATE.get().floatValue();
        float epsilon = Config.AMBIENT_EQUALIZE_EPSILON.get().floatValue();

        int[] openNodes = store.getOpenNodesRaw();

        for (int i = 0; i < open; i++)
        {
            int id = openNodes[i];

            if (AmbientEqualizer.equalize(store, id, ambient, rate, epsilon)) data.touch(id);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        Ambient.invalidate();
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;
        Ambient.invalidate();
    }
}
