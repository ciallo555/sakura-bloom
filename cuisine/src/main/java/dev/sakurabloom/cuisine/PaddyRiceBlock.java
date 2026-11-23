package dev.sakurabloom.cuisine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

final class PaddyRiceBlock extends KitchenCropBlock implements SimpleWaterloggedBlock {
    PaddyRiceBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties, seed);
        // Older saves omitted this property. Dry rice can wait on farmland for water.
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.getFluidState(pos).isSourceOfType(Fluids.WATER)) {
            return null;
        }
        return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return flooded(state) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForAge(int age) {
        return super.getStateForAge(age).setValue(BlockStateProperties.WATERLOGGED, true);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return flooded(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isMaxAge(state)) {
            growPanicle(level, pos);
        } else if (canAdvance(state, level, pos)) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return canAdvance(state, level, pos) && (!isMaxAge(state) || level.isEmptyBlock(pos.above()));
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        if (!canAdvance(state, level, pos)) {
            return;
        }
        if (level.isEmptyBlock(pos.above())) {
            super.growCrops(level, pos, state);
            growPanicle(level, pos);
        } else {
            int age = Math.min(getAge(state) + getBonemealAgeIncrease(level), getMaxAge() - 1);
            level.setBlock(pos, getStateForAge(age), UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (flooded(state) && isMaxAge(state)) {
            growPanicle(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        BlockPos top = pos.above();
        if (!level.isClientSide && !replacement.is(this) && level.getBlockState(top).is(KitchenContent.RICE_PANICLE.get())) {
            level.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (flooded(state)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    private boolean canAdvance(BlockState state, LevelReader level, BlockPos pos) {
        return flooded(state) && (getAge(state) < getMaxAge() - 1 || level.isEmptyBlock(pos.above()));
    }

    private static boolean flooded(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private void growPanicle(Level level, BlockPos pos) {
        BlockState root = level.getBlockState(pos);
        if (!level.isClientSide && root.is(this) && flooded(root) && isMaxAge(root) && level.isEmptyBlock(pos.above())) {
            level.setBlockAndUpdate(pos.above(), KitchenContent.RICE_PANICLE.get().defaultBlockState());
        }
    }
}
