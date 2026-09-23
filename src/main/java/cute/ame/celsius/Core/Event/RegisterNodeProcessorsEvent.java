package cute.ame.celsius.Core.Event;

import cute.ame.celsius.Core.Node.NodeProcessor;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

public final class RegisterNodeProcessorsEvent extends Event
{
    private final List<NodeProcessor> processors = new ArrayList<>();

    public void register(NodeProcessor processor)
    {
        if (processor != null) processors.add(processor);
    }

    public List<NodeProcessor> processors()
    {
        return processors;
    }
}
