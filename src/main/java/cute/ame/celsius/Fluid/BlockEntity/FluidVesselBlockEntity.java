package cute.ame.celsius.Fluid.BlockEntity;

import cute.ame.celsius.Config;
import cute.ame.celsius.Fluid.Block.IFluidVesselBlock;
import cute.ame.celsius.Fluid.Helper.VesselNodes;
import cute.ame.celsius.Fluid.Physics.BurstRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FluidVesselBlockEntity extends BlockEntity implements IFluidVesselBlockEntity
{
    private static final String K_NODE = "node";
    private static final String K_BURST = "burst";

    private long node = -1L;
    private float burst;

    protected FluidVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override public long getNodeHandle()
    {
        return node;
    }

    @Override public void setNodeHandle(long handle)
    {
        this.node = handle;
        setChanged();
    }

    @Override public float getBurstPressure()
    {
        return burst;
    }

    @Override public void setBurstPressure(float pressure)
    {
        this.burst = pressure;
        setChanged();
    }

    @Override public void rollBurstPressure(ServerLevel level)
    {
        if (burst > 0.0f) return;
        if (!(getBlockState().getBlock() instanceof IFluidVesselBlock vessel)) return;

        float amplitude = Config.BURST_JITTER.get().floatValue();
        setBurstPressure(BurstRule.jitter(vessel.getNominalBurstPressure(), amplitude, level.getRandom().nextFloat()));
    }

    @Override
    public void onLoad()
    {
        super.onLoad();

        if (level instanceof ServerLevel serverLevel)
        {
            rollBurstPressure(serverLevel);
            VesselNodes.onLoaded(serverLevel, worldPosition, this);
        }
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel serverLevel) VesselNodes.onUnloaded(serverLevel, worldPosition);

        super.setRemoved();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        node = tag.getLong(K_NODE);
        burst = tag.getFloat(K_BURST);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putLong(K_NODE, node);
        tag.putFloat(K_BURST, burst);
    }
}
