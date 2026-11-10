package cute.ame.celsius.Fluid.Physics;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import static cute.ame.celsius.Fluid.Data.FluidConstants.R_JOULES;

public final class FluidHeat
{
    //Constant volume
    public static double isochoricCapacity(FluidNodeStore store, int nodeId, float[] molarHeat)
    {
        int limit = Math.min(store.getStride(), molarHeat.length);
        double sum = 0.0;

        for (int s = 0; s < limit; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol > 0.0f) sum += mol * molarHeat[s];
        }

        return sum;
    }

    //Constant pressure (Ideal approximation)
    public static double isobaricCapacity(FluidNodeStore store, int nodeId, float[] molarHeat)
    {
        if (store.isLiquid(nodeId)) return isochoricCapacity(store, nodeId, molarHeat);
        double totalMol = store.moles(nodeId);
        //C_v + moles * R
        return isochoricCapacity(store, nodeId, molarHeat) + totalMol * R_JOULES;
    }

    public static double gamma(FluidNodeStore store, int nodeId, float[] molarHeat)
    {
        double cv = isochoricCapacity(store, nodeId, molarHeat);
        if (cv <= 1.0e-6) return 1.4; // default to diatomic ideal gas ratio
        return isobaricCapacity(store, nodeId, molarHeat) / cv;
    }

    public static float dischargeTemperature(float sourceTemp, double pSource, double pTarget, double gamma)
    {
        if (gamma <= 1.0f) return sourceTemp;
        if (pSource <= 0.001f) return sourceTemp; //Makes sure we don't divide by 0 or something too small :)
        if (pTarget <= pSource) return sourceTemp;
        double pressureRatio = Math.min(pTarget / pSource, 20.0f);
        return (float) (sourceTemp * Math.pow(pressureRatio, (gamma - 1.0) / gamma));

    }

    public static float addJoules(FluidNodeStore store, int nodeId, double joules, float[] molarHeat)
    {
        double heatCapacity = isochoricCapacity(store, nodeId, molarHeat);
        if (heatCapacity <= 0.0) return 0.0f;

        float before = store.temperature(nodeId);
        float next = before + (float) (joules / heatCapacity);

        if (next < 0.1f) next = 0.1f;

        store.setTemperature(nodeId, next);
        return next - before;
    }
}
