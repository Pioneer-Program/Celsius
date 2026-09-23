package cute.ame.celsius.Fluid.Helper;

import cute.ame.celsius.Core.Node.NodeEditor;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.celsius.Fluid.Physics.FluidHeat;
import org.jetbrains.annotations.Nullable;

public final class NodeCursor implements NodeEditor
{
    private FluidLevelData data;
    private FluidNodeStore store;
    private @Nullable RoomLevelData rooms;
    private float[] molarHeat;
    private int id = FluidNodeStore.INVALID;

    public void bind(FluidLevelData data, @Nullable RoomLevelData rooms, float[] molarHeat)
    {
        this.data = data;
        this.store = data.store();
        this.rooms = rooms;
        this.molarHeat = molarHeat;
    }

    public NodeCursor at(int id)
    {
        this.id = id;
        return this;
    }

    public void release()
    {
        data = null;
        store = null;
        rooms = null;
        molarHeat = null;
        id = FluidNodeStore.INVALID;
    }

    private boolean validSpecies(int species)
    {
        return species >= 0 && species < store.getStride();
    }

    @Override
    public int id()
    {
        return id;
    }

    @Override
    public long handle()
    {
        return store.handle(id);
    }

    @Override
    public float volume()
    {
        return store.volume(id);
    }

    @Override
    public float temperature()
    {
        return store.temperature(id);
    }

    @Override
    public double pressure()
    {
        return store.pressure(id);
    }

    @Override
    public float moles()
    {
        return store.moles(id);
    }

    @Override
    public float amount(int species)
    {
        return validSpecies(species) ? store.amount(id, species) : 0.0f;
    }

    @Override
    public float fraction(int species)
    {
        return validSpecies(species) ? store.fraction(id, species) : 0.0f;
    }

    @Override
    public boolean isLiquid()
    {
        return store.isLiquid(id);
    }

    @Override
    public boolean isOpen()
    {
        return store.hasFlag(id, FluidNodeStore.FLAG_OPEN);
    }

    @Override
    public boolean isRoom()
    {
        return rooms != null && rooms.room(id) != null;
    }

    @Override
    public double heatCapacity()
    {
        return FluidHeat.capacity(store, id, molarHeat);
    }

    @Override
    public float add(int species, float mol)
    {
        if (!validSpecies(species) || mol == 0.0f) return 0.0f;

        float before = store.amount(id, species);
        store.add(id, species, mol);
        float delta = store.amount(id, species) - before;

        if (delta != 0.0f) data.touch(id);
        return delta;
    }

    @Override
    public float addHeat(double joules)
    {
        if (joules == 0.0) return 0.0f;

        float change = FluidHeat.addJoules(store, id, joules, molarHeat);
        if (change != 0.0f) data.touch(id);
        return change;
    }

    @Override
    public float transfer(int species, float mol, int otherNode)
    {
        if (!validSpecies(species) || mol <= 0.0f || otherNode == id || !store.alive(otherNode)) return 0.0f;

        float moved = Math.min(mol, store.amount(id, species));
        if (moved <= 0.0f) return 0.0f;

        double targetCapacity = FluidHeat.capacity(store, otherNode, molarHeat);
        double carriedCapacity = species < molarHeat.length ? moved * (double) molarHeat[species] : 0.0;
        float sourceTemperature = store.temperature(id);
        float targetTemperature = store.temperature(otherNode);

        store.add(id, species, -moved);
        store.add(otherNode, species, moved);

        double total = targetCapacity + carriedCapacity;
        if (total > 0.0) store.setTemperature(otherNode, (float) ((targetCapacity * targetTemperature + carriedCapacity * sourceTemperature) / total));

        data.touch(id);
        data.touch(otherNode);
        return moved;
    }
}
