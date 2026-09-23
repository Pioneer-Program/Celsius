package cute.ame.celsius.Thermal.Loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import cute.ame.celsius.Celsius;
import cute.ame.celsius.Thermal.Data.ThermalMaterial;
import cute.ame.celsius.Thermal.Registry.ThermalMaterialRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class ThermalMaterialLoader extends SimplePreparableReloadListener<Map<ResourceLocation, ThermalMaterial>>
{
    public static final ThermalMaterialLoader INSTANCE = new ThermalMaterialLoader();

    private static final String FOLDER = "thermal_materials";
    private static final String EXT = ".json";

    @Override
    protected Map<ResourceLocation, ThermalMaterial> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, ThermalMaterial> out = new HashMap<>();
        final int prefix = FOLDER.length() + 1;

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, rl -> rl.getPath().endsWith(EXT)).entrySet())
        {
            ResourceLocation fileRl = entry.getKey();
            String path = fileRl.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRl.getNamespace(), path.substring(prefix, path.length() - EXT.length()));

            try (var reader = new InputStreamReader(entry.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                ThermalMaterial.CODEC.parse(JsonOps.INSTANCE, json).ifSuccess(value -> out.put(id, value)).ifError(err -> Celsius.LOGGER.error("[Celsius] Failed to parse {}/{}: {}", FOLDER, id, err.message()));
            }
            catch (Exception e) { Celsius.LOGGER.error("[Celsius] Error reading '{}'", fileRl, e); }
        }

        return out;
    }

    @Override
    protected void apply(Map<ResourceLocation, ThermalMaterial> data, ResourceManager manager, ProfilerFiller profiler)
    {
        ThermalMaterialRegistry.replaceAll(data);
        Celsius.LOGGER.info("[Celsius] Applied {} thermal material(s)", data.size());
    }
}
