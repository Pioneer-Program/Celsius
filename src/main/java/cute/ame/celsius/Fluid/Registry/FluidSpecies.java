package cute.ame.celsius.Fluid.Registry;

import cute.ame.celsius.Fluid.Data.SpeciesTable;

import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.SpeciesOrder;
import cute.ame.celsius.Celsius;
import cute.ame.celsius.Core.Event.SpeciesReloadEvent;
import cute.ame.celsius.Fluid.Data.SpeciesDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class FluidSpecies
{
    private static volatile SpeciesTable active = SpeciesTable.EMPTY;

    public static int O2 = SpeciesTable.UNKNOWN;
    public static int CO2 = SpeciesTable.UNKNOWN;
    public static int N2 = SpeciesTable.UNKNOWN;
    public static int H2O = SpeciesTable.UNKNOWN;

    public static SpeciesTable active()
    {
        return active;
    }

    public static int stride()
    {
        return active.size();
    }

    public static void refresh(MinecraftServer server)
    {
        ServerLevel anchor = server.overworld();
        SpeciesOrder order = SpeciesOrder.get(anchor);

        List<String> keys = order.merge(registryKeys());
        SpeciesTable next = build(keys);

        if (active.sameOrderAs(keys) && active.size() == next.size())
        {
            active = next;
            resolveCommon();
            NeoForge.EVENT_BUS.post(new SpeciesReloadEvent(next));
            return;
        }

        SpeciesTable previous = active;
        active = next;
        resolveCommon();

        remapLoadedLevels(server, previous, next);
        NeoForge.EVENT_BUS.post(new SpeciesReloadEvent(next));

        Celsius.LOGGER.info("[Celsius] Fluid species table: {} specie(s), {}", next.size(), String.join(", ", next.keys()));
    }

    private static void remapLoadedLevels(MinecraftServer server, SpeciesTable previous, SpeciesTable next)
    {
        if (previous.size() == 0) return;

        int[] remap = next.remapFrom(List.of(previous.keys()));
        for (ServerLevel level : server.getAllLevels())
        {
            FluidLevelData data = FluidLevelData.getIfPresent(level);
            if (data == null) continue;

            data.store().remapSpecies(remap, next.size());
            data.graph().invalidate();
            data.setDirty();
        }
    }

    private static SpeciesTable build(List<String> keys)
    {
        int n = keys.size();
        String[] array = keys.toArray(new String[0]);
        float[] molarMass = new float[n];
        float[] specificHeat = new float[n];
        float[] boilingPoint = new float[n];
        float[] latentHeat = new float[n];

        for (int i = 0; i < n; i++)
        {
            SpeciesDefinition species = SpeciesRegistry.get(array[i]);
            molarMass[i] = (float) species.molarMassGPerMol();
            specificHeat[i] = species.specificHeat();
            boilingPoint[i] = species.boilingPointK();
            latentHeat[i] = species.latentHeatJPerKg();
        }

        return new SpeciesTable(array, molarMass, specificHeat, boilingPoint, latentHeat);
    }

    private static List<String> registryKeys()
    {
        Map<ResourceLocation, SpeciesDefinition> snapshot = SpeciesRegistry.snapshot();
        TreeSet<String> sorted = new TreeSet<>();
        for (ResourceLocation id : snapshot.keySet()) sorted.add(id.getPath());
        return new ArrayList<>(sorted);
    }

    private static void resolveCommon()
    {
        O2 = active.indexOf("o2");
        CO2 = active.indexOf("co2");
        N2 = active.indexOf("n2");
        H2O = active.indexOf("h2o");

        if (O2 == SpeciesTable.UNKNOWN) Celsius.LOGGER.error("[Celsius] No specie 'o2' in Gas registries");
        if (CO2 == SpeciesTable.UNKNOWN) Celsius.LOGGER.error("[Celsius] No specie 'co2' in Gas registries");
    }
}
