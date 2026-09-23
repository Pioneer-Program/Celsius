package cute.ame.celsius.Core.Event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class VesselBurstEvent extends Event
{
    private final ServerLevel level;
    private final BlockPos pos;
    private final int node;
    private final double pressureP;
    private final float limitP;

    protected VesselBurstEvent(ServerLevel level, BlockPos pos, int node, double pressureP, float limitP)
    {
        this.level = level;
        this.pos = pos;
        this.node = node;
        this.pressureP = pressureP;
        this.limitP = limitP;
    }

    public ServerLevel level()
    {
        return level;
    }

    public BlockPos pos()
    {
        return pos;
    }

    public int node()
    {
        return node;
    }

    public double pressureP()
    {
        return pressureP;
    }

    public float limitP()
    {
        return limitP;
    }

    public double overload()
    {
        return pressureP / Math.max(limitP, 0.1f);
    }

    public static final class Pre extends VesselBurstEvent implements ICancellableEvent
    {
        public Pre(ServerLevel level, BlockPos pos, int node, double pressureP, float limitP)
        {
            super(level, pos, node, pressureP, limitP);
        }
    }

    public static final class Post extends VesselBurstEvent
    {
        private final float releasedMoles;

        public Post(ServerLevel level, BlockPos pos, int node, double pressureP, float limitP, float releasedMoles)
        {
            super(level, pos, node, pressureP, limitP);
            this.releasedMoles = releasedMoles;
        }

        public float releasedMoles()
        {
            return releasedMoles;
        }
    }
}
