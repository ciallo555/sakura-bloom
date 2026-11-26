package dev.sakurabloom.adventure.combat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import java.util.List;

final class RelicBlade extends SwordItem {
    private final Sanctum theme;
    RelicBlade(Sanctum theme) {
        super(Tiers.NETHERITE, new Item.Properties().fireResistant().rarity(Rarity.EPIC)
            .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.4F)));
        this.theme = theme;
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity wielder) {
        if (!wielder.level().isClientSide && wielder.getRandom().nextFloat() < 0.35F) {
            switch (theme) {
                case LANTERN -> { target.igniteForSeconds(4); wielder.heal(1.0F); }
                case MOONLIT -> { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 65, 1)); wielder.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 65)); }
                case VERMILION -> { target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80)); wielder.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40)); }
                case ASHEN -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            }
        }
        return super.hurtEnemy(stack, target, wielder);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId()+".effect").withStyle(ChatFormatting.GRAY));
    }
}
