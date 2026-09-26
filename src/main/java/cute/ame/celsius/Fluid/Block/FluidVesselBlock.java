package cute.ame.celsius.Fluid.Block;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Helper.VesselNodes;
import cute.ame.celsius.Fluid.Level.RoomLevelData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FluidVesselBlock extends Block implements EntityBlock
{
    public static final int NO_PORTS = 0;
    public static final int ALL_PORTS = 0b111111;

    protected FluidVesselBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    public abstract float getVolumeLitres();

    public abstract boolean merges();

    // value between (0, 1]
    public abstract float getConductance();

    public abstract float getNominalBurstPressure();

    public float conductance(Level level, BlockPos pos, BlockState state)
    {
        return getConductance();
    }

    public static int port(Direction direction)
    {
        return 1 << direction.get3DDataValue();
    }

    public int ports(BlockState state)
    {
        return ALL_PORTS;
    }

    public int filterPorts(BlockState state)
    {
        return NO_PORTS;
    }

    public @Nullable String filterSpecies(Level level, BlockPos pos, BlockState state)
    {
        return null;
    }

    public float filterRate(Level level, BlockPos pos, BlockState state)
    {
        return 0.0f;
    }

    public @Nullable Direction outlet(BlockState state)
    {
        return null;
    }

    public float boost(Level level, BlockPos pos, BlockState state)
    {
        return 0.0f;
    }

    public @Nullable BlockPos roomMouth(BlockPos pos, BlockState state)
    {
        return null;
    }

    public int bridgeNode(ServerLevel level, BlockPos pos, BlockState state)
    {
        BlockPos mouth = roomMouth(pos, state);
        if (mouth == null) return FluidNodeStore.INVALID;

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        return rooms == null ? FluidNodeStore.INVALID : rooms.nodeAt(mouth);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            VesselNodes.onRemoved(level, pos, state);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
