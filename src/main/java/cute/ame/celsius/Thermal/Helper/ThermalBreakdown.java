package cute.ame.celsius.Thermal.Helper;

import cute.ame.celsius.Core.Event.ThermalBreakdownEvent;
import cute.ame.celsius.Core.Thermal.Residency;
import cute.ame.celsius.Thermal.Data.MaterialTable;
import cute.ame.celsius.Thermal.Level.ThermalLevelData;
import cute.ame.celsius.Thermal.Registry.ThermalMaterials;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

public final class ThermalBreakdown
{
    public static boolean exceeded(float kelvin, float breakdownK)
    {
        return kelvin > breakdownK;
    }

    public static int apply(ServerLevel level, ThermalLevelData data, LongArrayList candidates)
    {
        MaterialTable table = ThermalMaterials.table();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int broken = 0;

        for (int i = 0, n = candidates.size(); i < n; i++)
        {
            long packed = candidates.getLong(i);
            cursor.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (!Residency.isLoaded(level, cursor)) continue;

            float kelvin = data.kelvinAt(packed);
            if (Float.isNaN(kelvin)) continue;

            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) continue;

            int material = ThermalMaterials.indexOf(state);
            if (!table.isValid(material) || !exceeded(kelvin, table.breakdownK(material))) continue;

            BlockState into = ThermalMaterials.breakdownInto(material);
            if (state.getBlock() == into.getBlock())
            {
                data.detach(cursor);
                continue;
            }

            BlockPos pos = cursor.immutable();
            NeoForge.EVENT_BUS.post(new ThermalBreakdownEvent(level, pos, state, into, kelvin));

            if (level.getBlockState(pos) == state)
            {
                data.detach(pos);
                continue;
            }

            broken++;
        }

        return broken;
    }
}
