package dev.sakurabloom.cuisine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class RicePanicleBlock extends BushBlock {
    private static final MapCodec<RicePanicleBlock> CODEC = simpleCodec(RicePanicleBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);

    RicePanicleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RicePanicleBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return KitchenContent.RICE.seeds().toStack();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var rice = KitchenContent.RICE.block().get();
        BlockState below = level.getBlockState(pos.below());
        return below.is(rice) && rice.isMaxAge(below);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !replacement.is(this)) {
            BlockPos root = pos.below();
            BlockState below = level.getBlockState(root);
            var rice = KitchenContent.RICE.block().get();
            if (below.is(rice) && rice.isMaxAge(below)) {
                var fluid = below.getFluidState();
                level.setBlockAndUpdate(root, fluid.isEmpty() ? Blocks.AIR.defaultBlockState() : fluid.createLegacyBlock());
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
