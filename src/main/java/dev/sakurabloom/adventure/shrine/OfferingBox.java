package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class OfferingBox extends Block {
    private static final VoxelShape SHAPE = Block.box(0,0,1,16,13,15);
    private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE,SakuraBloom.id("gameplay/shrine_prayer"));
    OfferingBox(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!stack.is(ShrineAdventure.TALISMAN.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            var data = Commissions.journal(player);
            if (!data.getBoolean("PrayerPaid")) {
                stack.consume(1,player);
                data.putBoolean("PrayerPaid",true);
                level.playSound(null,pos,SoundEvents.ITEM_FRAME_ADD_ITEM,SoundSource.BLOCKS, .85F,1.35F);
            }
            player.displayClientMessage(Component.translatable("shrine.sakura_bloom.pray"),true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            var data = Commissions.journal(player);
            if (data.getBoolean("PrayerPaid")) {
                data.remove("PrayerPaid");
                var params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.GIFT);
                server.getServer().reloadableRegistries().getLootTable(REWARD).getRandomItems(params,stack -> Commissions.give(player,stack));
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,12,.35,.2,.35,.02);
                server.playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1, .9F);
                player.displayClientMessage(Component.translatable("shrine.sakura_bloom.blessing"),true);
            } else player.displayClientMessage(Component.translatable("shrine.sakura_bloom.deposit"),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
