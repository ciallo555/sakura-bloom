package dev.sakurabloom.cuisine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.function.Supplier;

public final class TeaItem extends Item implements CuisineItem {
    private final Supplier<MobEffectInstance> effect;

    TeaItem(Supplier<MobEffectInstance> effect) {
        super(new Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.2F).alwaysEdible()
            .usingConvertsTo(Items.GLASS_BOTTLE).effect(effect, 1).build()));
        this.effect = effect;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PotionContents.addPotionTooltip(List.of(effect.get()), tooltip::add, 1, context.tickRate());
    }
}
