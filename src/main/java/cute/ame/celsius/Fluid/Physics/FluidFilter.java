package cute.ame.celsius.Fluid.Physics;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;

public final class FluidFilter
{
    public static float pass(FluidNodeStore store, int source, int target, int species, float rate, float[] molarHeat)
    {
        if (species < 0 || rate <= 0.0f) return 0.0f;
        if (!store.alive(source) || !store.alive(target)) return 0.0f;

        float held = store.amount(source, species);
        if (held <= 0.0f) return 0.0f;

        double upstream = store.fraction(source, species) * store.pressure(source);
        double downstream = store.fraction(target, species) * store.pressure(target);
        if (upstream <= downstream) return 0.0f;

        float moved = (float) (held * Math.min(rate, 1.0f) * (1.0 - downstream / upstream));
        if (moved <= 0.0f) return 0.0f;

        double targetCapacity = FluidHeat.capacity(store, target, molarHeat);
        double carriedCapacity = species < molarHeat.length ? moved * (double) molarHeat[species] : 0.0;
        float sourceTemperature = store.temperature(source);
        float targetTemperature = store.temperature(target);

        store.add(source, species, -moved);
        store.add(target, species, moved);

        double total = targetCapacity + carriedCapacity;
        if (total > 0.0) store.setTemperature(target, (float) ((targetCapacity * targetTemperature + carriedCapacity * sourceTemperature) / total));

        return moved;
    }
}
