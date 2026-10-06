package cute.ame.celsius.Core.Energy;

@FunctionalInterface
public interface EnergySink
{
    EnergySink UNLIMITED = joules -> Math.max(joules, 0.0);

    double draw(double joules);

    default double share(double joules)
    {
        if (joules <= 0.0) return 1.0;
        return Math.clamp(draw(joules) / joules, 0.0, 1.0);
    }
}
