package cute.ame.celsius.Core.Thermal;

import cute.ame.celsius.Fluid.Data.AmbientState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public interface AmbientProvider
{
    @Nullable AmbientState of(ServerLevel level);

    default @Nullable AmbientState at(ServerLevel level, BlockPos pos)
    {
        return of(level);
    }

    default void invalidate()
    {

    }
}
