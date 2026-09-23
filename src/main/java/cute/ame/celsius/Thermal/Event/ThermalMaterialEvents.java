package cute.ame.celsius.Thermal.Event;

import cute.ame.celsius.Celsius;
import cute.ame.celsius.Thermal.Level.ThermalLevelData;
import cute.ame.celsius.Thermal.Loader.ThermalDeviceLoader;
import cute.ame.celsius.Thermal.Loader.ThermalMaterialLoader;
import cute.ame.celsius.Thermal.Registry.ThermalDevices;
import cute.ame.celsius.Thermal.Registry.ThermalMaterials;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Celsius.MODID)
public final class ThermalMaterialEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(ThermalMaterialLoader.INSTANCE);
        event.addListener(ThermalDeviceLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ThermalMaterials.refresh(event.getServer());
        ThermalDevices.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;

        MinecraftServer server = event.getPlayerList().getServer();
        ThermalMaterials.refresh(server);
        ThermalDevices.refresh(server);
        ThermalLevelData.onMaterialsReloaded(server);
    }
}
