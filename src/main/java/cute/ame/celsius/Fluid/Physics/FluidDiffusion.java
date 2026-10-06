package cute.ame.celsius.Fluid.Physics;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;

public final class FluidDiffusion
{
    private static final double MIN_GAP = 1.0e-7;

    public static double mix(FluidNodeStore store, int a, int b, float rate, float[] molarHeat)
    {
        if (rate <= 0.0f || store.isLiquid(a) || store.isLiquid(b)) return 0.0;

        float[] moles = store.getMolesRaw();
        float na = moles[a];
        float nb = moles[b];
        if (na <= 0.0f || nb <= 0.0f) return 0.0;

        float[] amount = store.getAmountsRaw();
        int stride = store.getStride();
        int baseA = a * stride;
        int baseB = b * stride;
        double invA = 1.0 / na;
        double invB = 1.0 / nb;

        double gap = 0.0;
        for (int s = 0; s < stride; s++)
        {
            double d = Math.abs(amount[baseA + s] * invA - amount[baseB + s] * invB);
            if (d > gap) gap = d;
        }

        if (gap <= MIN_GAP) return gap;

        double k = Math.min(rate, 1.0f) * ((double) na * nb / ((double) na + nb));
        float ta = store.temperature(a);
        float tb = store.temperature(b);
        int heated = ta == tb ? 0 : Math.min(stride, molarHeat.length);

        double energyA = 0.0, energyB = 0.0, capacityA = 0.0, capacityB = 0.0;

        for (int s = 0; s < stride; s++)
        {
            int ia = baseA + s;
            int ib = baseB + s;
            float beforeA = amount[ia];
            float beforeB = amount[ib];

            float move = (float) (k * (beforeA * invA - beforeB * invB));
            float afterA = Math.max(beforeA - move, 0.0f);
            float afterB = Math.max(beforeB + move, 0.0f);

            amount[ia] = afterA;
            amount[ib] = afterB;

            if (s >= heated) continue;

            double c = molarHeat[s];
            if (move >= 0.0f)
            {
                energyA += c * afterA * ta;
                energyB += c * (beforeB * (double) tb + move * (double) ta);
            }
            else
            {
                energyA += c * (beforeA * (double) ta - move * (double) tb);
                energyB += c * afterB * tb;
            }

            capacityA += c * afterA;
            capacityB += c * afterB;
        }

        store.recomputeMoles(a);
        store.recomputeMoles(b);

        if (heated > 0)
        {
            if (capacityA > 0.0) store.setTemperature(a, (float) (energyA / capacityA));
            if (capacityB > 0.0) store.setTemperature(b, (float) (energyB / capacityB));
        }

        return gap;
    }
}
