package cute.ame.celsius.Fluid.Event;

import cute.ame.celsius.Fluid.Loader.SpeciesLoader;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;

import cute.ame.celsius.Celsius;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class FluidSpeciesEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(SpeciesLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        FluidSpecies.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;
        FluidSpecies.refresh(event.getPlayerList().getServer());
    }
}
