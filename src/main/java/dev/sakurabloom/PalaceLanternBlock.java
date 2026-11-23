package dev.sakurabloom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class PalaceLanternBlock extends LanternBlock {
    private static final MapCodec<LanternBlock> CODEC = simpleCodec(PalaceLanternBlock::new);
    private static final VoxelShape STANDING_SHAPE = Shapes.or(box(4, 0, 4, 12, 2, 12),
        box(3, 2, 3, 13, 12, 13), box(4, 12, 4, 12, 13, 12), box(6, 13, 7, 10, 15, 9));
    private static final VoxelShape HANGING_SHAPE = Shapes.or(STANDING_SHAPE.move(0, 1.0 / 16.0, 0),
        box(7, 0, 7, 9, 2, 9));

    PalaceLanternBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<LanternBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING_SHAPE;
    }
}
