package dev.sakurabloom;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public final class MortarRecipe extends SingleItemRecipe {
    public MortarRecipe(String group, Ingredient ingredient, ItemStack result) {
        super(BloomContent.GRINDING.get(), BloomContent.GRINDING_SERIALIZER.get(), group, ingredient, result);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return BloomContent.MORTAR_ITEM.toStack();
    }

    public static final class Serializer extends SingleItemRecipe.Serializer<MortarRecipe> {
        public Serializer() {
            super(MortarRecipe::new);
        }
    }
}
