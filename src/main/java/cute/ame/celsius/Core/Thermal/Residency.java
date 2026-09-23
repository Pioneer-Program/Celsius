package cute.ame.celsius.Core.Thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class Residency
{
    private static ChunkResidency active = ChunkResidency.VANILLA;

    public static void install(ChunkResidency residency)
    {
        if (residency != null) active = residency;
    }

    public static boolean isLoaded(ServerLevel level, BlockPos pos)
    {
        return active.isLoaded(level, pos);
    }

    public static BlockPos locate(ServerLevel level, Vec3 point)
    {
        return active.locate(level, point);
    }
}
