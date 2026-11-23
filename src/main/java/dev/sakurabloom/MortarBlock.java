package dev.sakurabloom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MortarBlock extends Block {
    public static final MapCodec<MortarBlock> CODEC = simpleCodec(MortarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 3, 13), box(2, 3, 2, 14, 9, 14),
        box(7, 9, 7, 11, 12, 10));

    public MortarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MortarBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() && player.getOffhandItem().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        var recipe = level.getRecipeManager().getRecipeFor(BloomContent.GRINDING.get(), input, level);
        if (recipe.isEmpty()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            ItemStack result = recipe.get().value().assemble(input, server.registryAccess());
            stack.consume(1, player);
            player.getInventory().placeItemBackInInventory(result);
            server.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 0.8F, 1.15F);
            server.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("block.sakura_bloom.flower_mortar.hint"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
