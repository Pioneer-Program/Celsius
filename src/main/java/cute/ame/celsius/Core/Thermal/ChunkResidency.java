package cute.ame.celsius.Core.Thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface ChunkResidency
{
    ChunkResidency VANILLA = (level, pos) -> level.hasChunkAt(pos);

    boolean isLoaded(ServerLevel level, BlockPos pos);

    default BlockPos locate(ServerLevel level, Vec3 point)
    {
        return BlockPos.containing(point);
    }
}
