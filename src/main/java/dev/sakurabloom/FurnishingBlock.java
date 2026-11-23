package dev.sakurabloom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

final class FurnishingBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<FurnishingBlock> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
        StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter(block -> block.style),
        propertiesCodec()).apply(builder, FurnishingBlock::new));
    private final Style style;

    enum Style implements StringRepresentable {
        MAT(box(0, 0, 0, 16, 1, 16), Direction.DOWN),
        SCREEN(box(0, 0, 6, 16, 16, 10), box(6, 0, 0, 10, 16, 16), null),
        CURTAIN(box(0, 1, 7, 16, 16, 9), box(7, 1, 0, 9, 16, 16), Direction.UP),
        STONE_LANTERN(Shapes.or(box(3, 0, 3, 13, 10, 13), box(1, 10, 1, 15, 12, 15),
            box(3, 12, 3, 13, 13, 13), box(5, 13, 5, 11, 14, 11), box(7, 14, 7, 9, 16, 9)), Direction.DOWN),
        VASE(Shapes.or(box(5, 0, 5, 11, 2, 11), box(3, 2, 3, 13, 8, 13),
            box(4, 8, 4, 12, 10, 12), box(6, 10, 6, 10, 12, 10), box(5, 12, 5, 11, 13, 11)), Direction.DOWN),
        SLENDER_VASE(Shapes.or(box(5, 0, 5, 11, 3, 11), box(4, 3, 4, 12, 10, 12),
            box(5, 10, 5, 11, 12, 11), box(6, 12, 6, 10, 15, 10), box(5, 15, 5, 11, 16, 11)), Direction.DOWN),
        SHOULDERED_VASE(Shapes.or(box(5, 0, 5, 11, 3, 11), box(4, 3, 4, 12, 6, 12),
            box(3, 6, 3, 13, 11, 13), box(4, 11, 4, 12, 12, 12), box(6, 12, 6, 10, 14, 10)), Direction.DOWN),
        TRUMPET_VASE(Shapes.or(box(5, 0, 5, 11, 3, 11), box(4, 3, 4, 12, 9, 12),
            box(5, 9, 5, 11, 11, 11), box(6, 11, 6, 10, 13, 10), box(5, 13, 5, 11, 14, 11),
            box(4, 14, 4, 12, 15, 12)), Direction.DOWN),
        ROUND_VASE(Shapes.or(box(5, 0, 5, 11, 2, 11), box(3, 2, 3, 13, 4, 13),
            box(2, 4, 2, 14, 8, 14), box(3, 8, 3, 13, 9, 13), box(5, 9, 5, 11, 11, 11)), Direction.DOWN),
        TILES(Shapes.block(), null);

        final VoxelShape northSouth;
        final VoxelShape eastWest;
        final Direction support;

        Style(VoxelShape shape, Direction support) {
            this(shape, shape, support);
        }

        Style(VoxelShape northSouth, VoxelShape eastWest, Direction support) {
            this.northSouth = northSouth;
            this.eastWest = eastWest;
            this.support = support;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    FurnishingBlock(Style style, Properties properties) {
        super(properties);
        this.style = style;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<FurnishingBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? style.northSouth : style.eastWest;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return style.support == null || Block.canSupportCenter(level, pos.relative(style.support), style.support.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == style.support && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
