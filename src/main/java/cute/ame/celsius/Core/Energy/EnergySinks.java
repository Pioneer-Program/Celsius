package cute.ame.celsius.Core.Energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

public final class EnergySinks
{
    @FunctionalInterface
    public interface Resolver
    {
        EnergySink resolve(ServerLevel level, BlockPos pos);
    }

    public static final Resolver UNLIMITED = (level, pos) -> EnergySink.UNLIMITED;
    private static volatile Resolver resolver = UNLIMITED;

    public static EnergySink at(ServerLevel level, BlockPos pos)
    {
        return resolver.resolve(level, pos);
    }

    public static void install(Resolver next)
    {
        resolver = Objects.requireNonNull(next);
    }

    public static Resolver installed()
    {
        return resolver;
    }
}
