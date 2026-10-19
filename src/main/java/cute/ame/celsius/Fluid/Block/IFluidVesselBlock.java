package cute.ame.celsius.Fluid.Block;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IFluidVesselBlock
{
    int NO_PORTS = 0;
    int ALL_PORTS = 0b111111;

    int BRIDGE_BOTH = 0;
    int BRIDGE_TO_ROOM = 1;
    int BRIDGE_FROM_ROOM = 2;

    static int port(Direction direction)
    {
        return 1 << direction.get3DDataValue();
    }

    float getVolumeLitres();

    boolean merges();

    // value between (0, 1]
    float getConductance();

    float getNominalBurstPressure();

    default float conductance(Level level, BlockPos pos, BlockState state) {
        return getConductance();
    }

    default int ports(BlockState state) {
        return ALL_PORTS;
    }

    default int filterPorts(BlockState state) {
        return NO_PORTS;
    }

    default @Nullable String filterSpecies(Level level, BlockPos pos, BlockState state) {
        return null;
    }

    default float filterRate(Level level, BlockPos pos, BlockState state) {
        return 0.0f;
    }

    default @Nullable Direction outlet(BlockState state) {
        return null;
    }

    default @Nullable Direction inlet(BlockState state) {
        Direction out = outlet(state);
        return out == null ? null : out.getOpposite();
    }

    default float boost(Level level, BlockPos pos, BlockState state) {
        return 0.0f;
    }

    default @Nullable BlockPos roomMouth(BlockPos pos, BlockState state) {
        return null;
    }

    default int bridgeFlow(BlockState state) {
        return BRIDGE_BOTH;
    }

    default int bridgeNode(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos mouth = roomMouth(pos, state);
        if (mouth == null) return FluidNodeStore.INVALID;

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        return rooms == null ? FluidNodeStore.INVALID : rooms.nodeAt(mouth);
    }
}
