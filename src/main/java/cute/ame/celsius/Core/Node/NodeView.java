package cute.ame.celsius.Core.Node;

public interface NodeView
{
    int id();

    long handle();

    float volume();

    float temperature();

    double pressure();

    float moles();

    float amount(int species);

    float fraction(int species);

    boolean isLiquid();

    boolean isOpen();

    boolean isRoom();

    double heatCapacity();
}
