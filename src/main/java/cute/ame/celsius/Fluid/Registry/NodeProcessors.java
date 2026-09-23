package cute.ame.celsius.Fluid.Registry;

import cute.ame.celsius.Core.Event.RegisterNodeProcessorsEvent;
import cute.ame.celsius.Core.Node.NodeProcessor;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Celsius;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public final class NodeProcessors
{
    private static volatile NodeProcessor[] processors = new NodeProcessor[0];
    private static volatile int[][] required = new int[0][];
    private static volatile int[] periods = new int[0];

    public static int count()
    {
        return processors.length;
    }

    public static NodeProcessor get(int index)
    {
        return processors[index];
    }

    public static int[] required(int index)
    {
        return required[index];
    }

    public static int period(int index)
    {
        return periods[index];
    }

    public static void rebuild(SpeciesTable table)
    {
        List<NodeProcessor> registered = NeoForge.EVENT_BUS.post(new RegisterNodeProcessorsEvent()).processors();

        List<NodeProcessor> active = new ArrayList<>(registered.size());
        List<int[]> species = new ArrayList<>(registered.size());

        for (NodeProcessor processor : registered)
        {
            String[] keys = processor.requiredSpecies();
            int[] indices = new int[keys == null ? 0 : keys.length];
            boolean resolved = true;

            for (int i = 0; i < indices.length; i++)
            {
                indices[i] = table.indexOf(keys[i]);
                if (indices[i] == SpeciesTable.UNKNOWN)
                {
                    Celsius.LOGGER.warn("[Celsius] node processor {} needs unknown species '{}', disabled", processor.getClass().getName(), keys[i]);
                    resolved = false;
                    break;
                }
            }

            if (!resolved) continue;

            active.add(processor);
            species.add(indices);
        }

        int n = active.size();
        int[] nextPeriods = new int[n];
        for (int i = 0; i < n; i++) nextPeriods[i] = Math.max(active.get(i).period(), 1);

        required = species.toArray(new int[0][]);
        periods = nextPeriods;
        processors = active.toArray(new NodeProcessor[0]);

        if (!registered.isEmpty()) Celsius.LOGGER.info("[Celsius] Node processors: {} active of {} registered", n, registered.size());
    }
}
