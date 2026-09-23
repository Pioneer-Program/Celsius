package cute.ame.celsius;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Celsius.MODID)
public class Celsius
{
    public static final String MODID = "celsius";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Celsius(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
