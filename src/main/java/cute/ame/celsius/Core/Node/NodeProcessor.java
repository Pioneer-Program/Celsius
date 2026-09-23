package cute.ame.celsius.Core.Node;

import net.minecraft.server.level.ServerLevel;

public interface NodeProcessor
{
    String[] requiredSpecies();

    int period();

    void process(ServerLevel level, NodeEditor node);
}
