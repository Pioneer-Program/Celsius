package cute.ame.celsius.Fluid.BlockEntity;

import cute.ame.celsius.Config;
import cute.ame.celsius.Fluid.Block.IFluidVesselBlock;
import cute.ame.celsius.Fluid.Physics.BurstRule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface IFluidVesselBlockEntity
{
    BlockState getBlockState();

    long getNodeHandle();

    void setNodeHandle(long handle);

    float getBurstPressure();

    void setBurstPressure(float pressure);

    default void rollBurstPressure(ServerLevel level) {
        if (getBurstPressure() > 0.0f) return;
        if (!(getBlockState().getBlock() instanceof IFluidVesselBlock vessel)) return;

        float amplitude = Config.BURST_JITTER.get().floatValue();
        setBurstPressure(BurstRule.jitter(vessel.getNominalBurstPressure(), amplitude, level.getRandom().nextFloat()));
    }
}
