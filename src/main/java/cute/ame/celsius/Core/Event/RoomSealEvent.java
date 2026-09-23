package cute.ame.celsius.Core.Event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public final class RoomSealEvent extends Event
{
    private final ServerLevel level;
    private final int node;
    private final BlockPos origin;
    private final boolean sealed;

    public RoomSealEvent(ServerLevel level, int node, BlockPos origin, boolean sealed)
    {
        this.level = level;
        this.node = node;
        this.origin = origin;
        this.sealed = sealed;
    }

    public ServerLevel level()
    {
        return level;
    }

    public int node()
    {
        return node;
    }

    public BlockPos origin()
    {
        return origin;
    }

    public boolean sealed()
    {
        return sealed;
    }
}
