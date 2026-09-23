package cute.ame.celsius.Core.Event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public final class PhaseChangeEvent extends Event
{
    private final ServerLevel level;
    private final int node;
    private final boolean liquid;

    public PhaseChangeEvent(ServerLevel level, int node, boolean liquid)
    {
        this.level = level;
        this.node = node;
        this.liquid = liquid;
    }

    public ServerLevel level()
    {
        return level;
    }

    public int node()
    {
        return node;
    }

    public boolean liquid()
    {
        return liquid;
    }
}
