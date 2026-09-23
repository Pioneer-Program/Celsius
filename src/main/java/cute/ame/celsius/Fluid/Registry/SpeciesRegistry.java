package cute.ame.celsius.Fluid.Registry;

import cute.ame.celsius.Fluid.Data.SpeciesDefinition;
import cute.ame.celsius.Celsius;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SpeciesRegistry
{
    private static volatile Map<String, SpeciesDefinition> entries = Map.of();

    private static final Map<String, SpeciesDefinition> BUILTIN = Map.ofEntries(
        Map.entry("n2", new SpeciesDefinition(28.014, 1040.0f, 77.36f, 199_000.0f)),
        Map.entry("o2", new SpeciesDefinition(31.998, 918.0f, 90.19f, 213_000.0f)),
        Map.entry("co2", new SpeciesDefinition(44.009, 844.0f, 194.65f, 571_000.0f)),
        Map.entry("ar", new SpeciesDefinition(39.948, 520.0f, 87.30f, 161_000.0f)),
        Map.entry("h2", new SpeciesDefinition(2.016, 14300.0f, 20.28f, 449_000.0f)),
        Map.entry("he", new SpeciesDefinition(4.003, 5193.0f, 4.22f, 20_900.0f)),
        Map.entry("ch4", new SpeciesDefinition(16.043, 2220.0f, 111.65f, 510_000.0f)),
        Map.entry("h2o", new SpeciesDefinition(18.015, 1996.0f, 373.15f, 2_256_000.0f)),
        Map.entry("nh3", new SpeciesDefinition(17.031, 2190.0f, 239.81f, 1_371_000.0f)),
        Map.entry("so2", new SpeciesDefinition(64.066, 640.0f, 263.13f, 389_000.0f)),
        Map.entry("ne", new SpeciesDefinition(20.180, 1030.0f, 27.10f, 85_900.0f)),
        Map.entry("n2o", new SpeciesDefinition(44.013, 880.0f, 184.67f, 376_000.0f))
    );

    public static void replaceAll(Map<ResourceLocation, SpeciesDefinition> loaded)
    {
        if (loaded.isEmpty()) return;

        Map<String, SpeciesDefinition> next = new HashMap<>(loaded.size() * 2);
        for (Map.Entry<ResourceLocation, SpeciesDefinition> e : loaded.entrySet())
        {
            next.put(key(e.getKey().getPath()), e.getValue());
        }
        entries = Collections.unmodifiableMap(next);
    }

    public static SpeciesDefinition get(String name)
    {
        String k = key(name);
        SpeciesDefinition fromPack = entries.get(k);
        if (fromPack != null) return fromPack;
        return BUILTIN.getOrDefault(k, SpeciesDefinition.FALLBACK);
    }

    public static boolean isKnown(String name)
    {
        String k = key(name);
        return entries.containsKey(k) || BUILTIN.containsKey(k);
    }

    public static Map<ResourceLocation, SpeciesDefinition> snapshot()
    {
        Map<ResourceLocation, SpeciesDefinition> out = new HashMap<>();
        Map<String, SpeciesDefinition> source = entries.isEmpty() ? BUILTIN : entries;
        source.forEach((k, v) -> out.put(ResourceLocation.fromNamespaceAndPath(Celsius.MODID, k), v));
        return out;
    }

    public static int loadedCount()
    {
        return entries.size();
    }

    public static String key(String raw)
    {
        int colon = raw.indexOf(':');
        String tail = (colon >= 0) ? raw.substring(colon + 1) : raw;
        int slash = tail.lastIndexOf('/');
        if (slash >= 0) tail = tail.substring(slash + 1);

        return tail.toLowerCase(Locale.ROOT);
    }
}
