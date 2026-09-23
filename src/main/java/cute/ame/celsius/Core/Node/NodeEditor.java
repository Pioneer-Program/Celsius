package cute.ame.celsius.Core.Node;

public interface NodeEditor extends NodeView
{
    float add(int species, float mol);

    float addHeat(double joules);

    float transfer(int species, float mol, int otherNode);
}
