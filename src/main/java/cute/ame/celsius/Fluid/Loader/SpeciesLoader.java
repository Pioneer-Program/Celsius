package cute.ame.celsius.Fluid.Loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import cute.ame.celsius.Fluid.Data.SpeciesDefinition;
import cute.ame.celsius.Fluid.Registry.SpeciesRegistry;
import cute.ame.celsius.Celsius;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class SpeciesLoader extends SimplePreparableReloadListener<Map<ResourceLocation, SpeciesDefinition>>
{
    public static final SpeciesLoader INSTANCE = new SpeciesLoader();
    private static final String FOLDER = "species";
    private static final String EXT = ".json";

    @Override
    protected Map<ResourceLocation, SpeciesDefinition> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, SpeciesDefinition> out = new HashMap<>();
        final int prefix = FOLDER.length() + 1;

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, rl -> rl.getPath().endsWith(EXT)).entrySet())
        {
            ResourceLocation fileRl = entry.getKey();
            String path = fileRl.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRl.getNamespace(), path.substring(prefix, path.length() - EXT.length()));

            try (var reader = new InputStreamReader(entry.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                SpeciesDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifSuccess(value -> out.put(id, value))
                    .ifError(err -> Celsius.LOGGER.error("[Celsius] Failed to parse {}/{}: {}", FOLDER, id, err.message()));
            }
            catch (Exception e)
            {
                Celsius.LOGGER.error("[Celsius] Error reading '{}'", fileRl, e);
            }
        }

        return out;
    }

    @Override
    protected void apply(Map<ResourceLocation, SpeciesDefinition> data, ResourceManager manager, ProfilerFiller profiler)
    {
        SpeciesRegistry.replaceAll(data);
        Celsius.LOGGER.info("[Celsius] Applied {} species definition(s)", data.size());
    }
}
