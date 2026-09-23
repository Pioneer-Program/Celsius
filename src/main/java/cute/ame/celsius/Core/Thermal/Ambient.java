package cute.ame.celsius.Core.Thermal;

import cute.ame.celsius.Fluid.Data.AmbientState;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public final class Ambient
{
    private static final double DEFAULT_PRESSURE_P = 0.986923;
    private static final float DEFAULT_TEMPERATURE_K = 288.15f;

    private static final String[] DEFAULT_SPECIES = { "n2", "o2", "ar" };
    private static final float[] DEFAULT_FRACTIONS = { 0.78f, 0.21f, 0.01f };

    private static volatile AmbientProvider[] PROVIDERS = new AmbientProvider[0];

    private static AmbientState fallback;

    public static synchronized void register(AmbientProvider provider)
    {
        if (provider == null) return;

        AmbientProvider[] next = Arrays.copyOf(PROVIDERS, PROVIDERS.length + 1);
        next[next.length - 1] = provider;
        PROVIDERS = next;
    }

    public static AmbientState of(ServerLevel level)
    {
        AmbientProvider[] providers = PROVIDERS;
        for (AmbientProvider provider : providers)
        {
            AmbientState answer = provider.of(level);
            if (answer != null) return answer;
        }

        return fallback();
    }

    public static AmbientState at(ServerLevel level, BlockPos pos)
    {
        AmbientProvider[] providers = PROVIDERS;
        for (AmbientProvider provider : providers)
        {
            AmbientState answer = provider.at(level, pos);
            if (answer != null) return answer;
        }

        return fallback();
    }

    public static void invalidate()
    {
        fallback = null;

        AmbientProvider[] providers = PROVIDERS;
        for (AmbientProvider provider : providers) provider.invalidate();
    }

    private static AmbientState fallback()
    {
        AmbientState cached = fallback;
        if (cached != null) return cached;

        SpeciesTable table = FluidSpecies.active();
        float[] fractions = new float[table.size()];

        for (int i = 0; i < DEFAULT_SPECIES.length; i++)
        {
            int species = table.indexOf(DEFAULT_SPECIES[i]);
            if (table.isValid(species)) fractions[species] = DEFAULT_FRACTIONS[i];
        }

        cached = new AmbientState(DEFAULT_PRESSURE_P, DEFAULT_TEMPERATURE_K, fractions, false);
        fallback = cached;
        return cached;
    }
}
