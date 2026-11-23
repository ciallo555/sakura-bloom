package dev.sakurabloom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CherryChestBlock extends ChestBlock {
    private static final MapCodec<CherryChestBlock> CODEC = simpleCodec(CherryChestBlock::new);

    CherryChestBlock(Properties properties) {
        super(properties, BloomContent.CHERRY_CHEST::get);
    }

    @Override
    public MapCodec<CherryChestBlock> codec() { return CODEC; }

    @Override
    public Entity newBlockEntity(BlockPos pos, BlockState state) { return new Entity(pos, state); }

    static final class Entity extends ChestBlockEntity {
        Entity(BlockPos pos, BlockState state) { super(BloomContent.CHERRY_CHEST.get(), pos, state); }

        @Override
        protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    }
}
