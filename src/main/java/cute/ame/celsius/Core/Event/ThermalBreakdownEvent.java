package cute.ame.celsius.Core.Event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

public final class ThermalBreakdownEvent extends Event
{
    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState state;
    private final BlockState into;
    private final float kelvin;

    public ThermalBreakdownEvent(ServerLevel level, BlockPos pos, BlockState state, BlockState into, float kelvin)
    {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.into = into;
        this.kelvin = kelvin;
    }

    public ServerLevel level()
    {
        return level;
    }

    public BlockPos pos()
    {
        return pos;
    }

    public BlockState state()
    {
        return state;
    }

    public BlockState into()
    {
        return into;
    }

    public float kelvin()
    {
        return kelvin;
    }
}
