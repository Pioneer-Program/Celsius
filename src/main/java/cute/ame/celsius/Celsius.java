package cute.ame.celsius;

import com.mojang.logging.LogUtils;
import cute.ame.celsius.Command.CelsiusDebugCommand;
import cute.ame.celsius.Core.Thermal.BlockHeatSink;
import cute.ame.celsius.Core.Thermal.BlockTemperature;
import cute.ame.celsius.Thermal.Helper.ThermalBlockSink;
import cute.ame.celsius.Thermal.Level.ThermalLevelData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Celsius.MODID)
public class Celsius
{
    public static final String MODID = "celsius";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Celsius(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BlockTemperature.register(ThermalLevelData::temperatureAt);
        BlockHeatSink.register(ThermalBlockSink.INSTANCE);

        NeoForge.EVENT_BUS.addListener(Celsius::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CelsiusDebugCommand.register(event.getDispatcher());
    }
}
