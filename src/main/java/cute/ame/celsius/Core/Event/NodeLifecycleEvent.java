package cute.ame.celsius.Core.Event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public abstract class NodeLifecycleEvent extends Event
{
    private final ServerLevel level;
    private final int node;
    private final long handle;

    protected NodeLifecycleEvent(ServerLevel level, int node, long handle)
    {
        this.level = level;
        this.node = node;
        this.handle = handle;
    }

    public ServerLevel level()
    {
        return level;
    }

    public int node()
    {
        return node;
    }

    public long handle()
    {
        return handle;
    }

    public static final class Created extends NodeLifecycleEvent
    {
        public Created(ServerLevel level, int node, long handle)
        {
            super(level, node, handle);
        }
    }

    public static final class Destroyed extends NodeLifecycleEvent
    {
        public Destroyed(ServerLevel level, int node, long handle)
        {
            super(level, node, handle);
        }
    }

    public static final class Merged extends NodeLifecycleEvent
    {
        private final int into;
        private final long intoHandle;

        public Merged(ServerLevel level, int from, long fromHandle, int into, long intoHandle)
        {
            super(level, from, fromHandle);
            this.into = into;
            this.intoHandle = intoHandle;
        }

        public int into()
        {
            return into;
        }

        public long intoHandle()
        {
            return intoHandle;
        }
    }

    public static final class Split extends NodeLifecycleEvent
    {
        private final int[] parts;

        public Split(ServerLevel level, int from, long fromHandle, int[] parts)
        {
            super(level, from, fromHandle);
            this.parts = parts;
        }

        public int[] parts()
        {
            return parts;
        }
    }
}
