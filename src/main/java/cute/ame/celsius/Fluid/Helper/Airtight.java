package cute.ame.celsius.Fluid.Helper;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class Airtight
{
    public static final TagKey<Block> AIRTIGHT = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("celsius", "airtight"));
    public static final TagKey<Block> LEAKY = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("celsius", "leaky"));

    public static boolean seals(BlockGetter level, BlockPos pos, BlockState state)
    {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return true;
        if (state.getBlock() instanceof FluidVesselBlock) return true;
        if (state.is(LEAKY)) return false;
        if (state.is(AIRTIGHT)) return true;

        return state.isCollisionShapeFullBlock(level, pos);
    }
}
