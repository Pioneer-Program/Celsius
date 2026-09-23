package cute.ame.celsius.Core.Event;

import cute.ame.celsius.Fluid.Data.SpeciesTable;
import net.neoforged.bus.api.Event;

public final class SpeciesReloadEvent extends Event
{
    private final SpeciesTable table;

    public SpeciesReloadEvent(SpeciesTable table)
    {
        this.table = table;
    }

    public SpeciesTable table()
    {
        return table;
    }
}
