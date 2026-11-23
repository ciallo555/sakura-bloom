package dev.sakurabloom.cuisine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class WildCabbageBlock extends BushBlock {
    private static final MapCodec<WildCabbageBlock> CODEC = simpleCodec(WildCabbageBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 9, 14);

    WildCabbageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WildCabbageBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SAND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return KitchenContent.CABBAGE.seeds().toStack();
    }
}
