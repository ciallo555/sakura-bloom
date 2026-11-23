package dev.sakurabloom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Set;

final class TravelCharmItem extends Item {
    private static final boolean CURIOS = ModList.get().isLoaded("curios");
    final String slot;
    private final List<Bonus> bonuses;

    TravelCharmItem(String slot, Bonus... bonuses) {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.slot = slot;
        this.bonuses = List.of(bonuses);
    }

    record Bonus(Holder<Attribute> attribute, AttributeModifier modifier) {}

    static Bonus percent(String id, Holder<Attribute> attribute, double amount) {
        return new Bonus(attribute, new AttributeModifier(SakuraBloom.id(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    static Bonus add(String id, Holder<Attribute> attribute, double amount) {
        return new Bonus(attribute, new AttributeModifier(SakuraBloom.id(id), amount, AttributeModifier.Operation.ADD_VALUE));
    }

    static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onFall);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onFinishEating);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onBreathe);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onPlayerTick);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".hint").withStyle(ChatFormatting.GRAY));
        tooltip.add((CURIOS ? Component.translatable("tooltip.sakura_bloom.charm.curios", Component.translatable("curios.identifier." + slot))
            : Component.translatable("tooltip.sakura_bloom.charm.offhand"))
            .withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) refreshBonuses(event.getEntity());
    }

    static void refreshBonuses(Player player) {
        Set<Item> curios = CURIOS ? CuriosTravelCharms.equipped(player) : Set.of();
        for (var entry : BloomContent.TRAVEL_CHARMS) {
            var charm = entry.get();
            boolean active = player.getOffhandItem().is(charm) || curios.contains(charm);
            for (var bonus : charm.bonuses) {
                var attribute = player.getAttribute(bonus.attribute());
                if (attribute == null || active == attribute.hasModifier(bonus.modifier().id())) continue;
                if (active) {
                    attribute.addTransientModifier(bonus.modifier());
                } else {
                    attribute.removeModifier(bonus.modifier().id());
                    if (bonus.attribute().equals(Attributes.MAX_HEALTH) && player.getHealth() > player.getMaxHealth())
                        player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    private static boolean worn(Player player, Item item) {
        return player.getOffhandItem().is(item) || (CURIOS && CuriosTravelCharms.isEquipped(player, item));
    }

    private static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
            || event.getDistance() <= 3
            || !worn(player, BloomContent.WIND_BELL.get())) return;

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.4F);
        level.sendParticles(BloomContent.SHIDARE.fallingPetals().get(), player.getX(), player.getY() + 0.2, player.getZ(),
            5, 0.3, 0.1, 0.3, 0.02);
    }

    private static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
            || player.getHealth() >= player.getMaxHealth() || event.getItem().getFoodProperties(player) == null) return;
        Item knot = BloomContent.KNOT_CHARM.get();
        if (player.getCooldowns().isOnCooldown(knot) || !worn(player, knot)) return;

        player.heal(4);
        player.getCooldowns().addCooldown(knot, 20 * 20);
        level.sendParticles(BloomContent.YAE.fallingPetals().get(), player.getX(), player.getY() + 1, player.getZ(),
            4, 0.25, 0.2, 0.25, 0.02);
    }

    private static void onBreathe(LivingBreatheEvent event) {
        if (event.canBreathe() || event.getConsumeAirAmount() == 0 || !(event.getEntity() instanceof Player player)
            || (player.tickCount & 1) != 0 || !player.isEyeInFluid(FluidTags.WATER)) return;
        if (worn(player, BloomContent.FISH_PENDANT.get())) event.setConsumeAirAmount(0);
    }
}
