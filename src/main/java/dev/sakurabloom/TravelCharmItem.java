package dev.sakurabloom;

import dev.sakurabloom.adventure.pouch.PouchItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TravelCharmItem extends Item {
    private static final boolean CURIOS = ModList.get().isLoaded("curios");
    private static final EffectCure UNEQUIP = EffectCure.get("sakura_bloom:unequip");
    private final int tooltipLines;
    private final List<Bonus> bonuses;

    TravelCharmItem(int tooltipLines, Bonus... bonuses) {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.tooltipLines = tooltipLines;
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
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onFinishEating);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TravelCharmItem::yieldToOtherEffect);
        NeoForge.EVENT_BUS.addListener(TravelCharmItem::onPlayerTick);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int line = 1; line <= tooltipLines; line++)
            tooltip.add(Component.translatable(getDescriptionId() + ".effect." + line).withStyle(ChatFormatting.GRAY));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) refreshBonuses(event.getEntity());
    }

    static void refreshBonuses(Player player) {
        Set<Item> equipped = equippedCharms(player);
        for (var entry : BloomContent.TRAVEL_CHARMS) {
            var charm = entry.get();
            boolean active = equipped.contains(charm);
            if (charm == BloomContent.FISH_PENDANT.get()) {
                active &= player.isInWater();
                updateWornEffect(player, MobEffects.WATER_BREATHING, 0, active);
            } else if (charm == BloomContent.INK_BRACELET.get()) {
                updateWornEffect(player, MobEffects.DIG_SPEED, 1, active);
            }
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
        return equippedCharms(player).contains(item);
    }

    private static Set<Item> equippedCharms(Player player) {
        var result = new HashSet<Item>();
        var offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof TravelCharmItem) result.add(offhand.getItem());
        if (offhand.getItem() instanceof PouchItem) PouchItem.collect(offhand, result);
        if (CURIOS) CuriosTravelCharms.collect(player, result, !(offhand.getItem() instanceof PouchItem));
        return result;
    }

    private static void updateWornEffect(Player player, Holder<MobEffect> effect, int amplifier, boolean active) {
        var current = player.getEffect(effect);
        if (!active) {
            if (current != null && current.getCures().contains(UNEQUIP)) player.removeEffect(effect);
        } else if (current == null) {
            var granted = new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true);
            granted.getCures().add(UNEQUIP);
            player.addEffect(granted);
        } else if (current.getAmplifier() < amplifier) {
            // Vanilla keeps the weaker potion underneath and ticks its remaining duration.
            player.addEffect(new MobEffectInstance(effect, 2, amplifier, true, false, true));
        }
    }

    private static void yieldToOtherEffect(MobEffectEvent.Applicable event) {
        var incoming = event.getEffectInstance();
        var effect = incoming.getEffect();
        if ((!effect.equals(MobEffects.WATER_BREATHING) && !effect.equals(MobEffects.DIG_SPEED))
            || !(event.getEntity() instanceof Player player) || player.level().isClientSide()
            || incoming.getCures().contains(UNEQUIP)) return;
        var current = player.getEffect(effect);
        // Release only our unlimited grant before vanilla merges a potion, beacon or other mod's effect.
        if (current != null && current.getCures().contains(UNEQUIP) && event.getApplicationResult())
            player.removeEffect(effect);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && event.getEntity() instanceof Player player
            && worn(player, BloomContent.WIND_BELL.get()))
            event.setCanceled(true);
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
            || event.getItem().getFoodProperties(player) == null || !worn(player, BloomContent.KNOT_CHARM.get())) return;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5 * 20));
        level.sendParticles(BloomContent.YAE.fallingPetals().get(), player.getX(), player.getY() + 1, player.getZ(),
            4, 0.25, 0.2, 0.25, 0.02);
    }
}
