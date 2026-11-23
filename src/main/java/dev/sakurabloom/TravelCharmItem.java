package dev.sakurabloom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

final class TravelCharmItem extends Item {
    private static final TagKey<Block> CHERRY_LOGS = TagKey.create(Registries.BLOCK, SakuraBloom.id("flowering_cherry_logs"));
    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    TravelCharmItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onFall);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onFinishEating);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onBreathe);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onBreakSpeed);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(CURIOS ? "tooltip.sakura_bloom.charm.curios" : "tooltip.sakura_bloom.charm.offhand")
            .withStyle(ChatFormatting.DARK_GREEN));
    }

    private static boolean worn(Player player, Item item) {
        return player.getOffhandItem().is(item) || (CURIOS && CuriosTravelCharms.isEquipped(player, item));
    }

    private static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
            || event.getDistance() <= player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)
            || !worn(player, BloomContent.WIND_BELL.get())) return;

        event.setDistance(Math.max(0, event.getDistance() - 3));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.4F);
        level.sendParticles(BloomContent.SHIDARE.fallingPetals().get(), player.getX(), player.getY() + 0.2, player.getZ(),
            5, 0.3, 0.1, 0.3, 0.02);
    }

    private static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
            || player.getHealth() >= player.getMaxHealth() || event.getItem().getFoodProperties(player) == null) return;
        Item knot = BloomContent.KNOT_CHARM.get();
        if (player.getCooldowns().isOnCooldown(knot) || !worn(player, knot)) return;

        player.heal(2);
        player.getCooldowns().addCooldown(knot, 45 * 20);
        level.sendParticles(BloomContent.YAE.fallingPetals().get(), player.getX(), player.getY() + 1, player.getZ(),
            4, 0.25, 0.2, 0.25, 0.02);
    }

    private static void onBreathe(LivingBreatheEvent event) {
        if (event.canBreathe() || event.getConsumeAirAmount() == 0 || !(event.getEntity() instanceof Player player)
            || (player.tickCount & 1) != 0 || !player.isEyeInFluid(FluidTags.WATER)) return;
        if (worn(player, BloomContent.FISH_PENDANT.get())) event.setConsumeAirAmount(0);
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!event.getState().is(BlockTags.MINEABLE_WITH_AXE) || !player.getMainHandItem().canPerformAction(ItemAbilities.AXE_DIG)
            || !worn(player, BloomContent.INK_BRACELET.get())) return;
        float multiplier = event.getState().is(CHERRY_LOGS) ? 1.35F : 1.2F;
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }
}
