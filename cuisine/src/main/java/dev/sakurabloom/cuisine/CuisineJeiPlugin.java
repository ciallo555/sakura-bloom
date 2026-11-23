package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import dev.sakurabloom.StationCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public final class CuisineJeiPlugin implements IModPlugin {
    private static final RecipeType<TeaKettleBlock.Stage> STEEPING =
        RecipeType.create(SakuraBloom.ID, "tea_kettle", TeaKettleBlock.Stage.class);
    private static final RecipeType<KitchenRecipes.Entry> STEAMING =
        RecipeType.create(SakuraBloom.ID, "bamboo_steamer", KitchenRecipes.Entry.class);
    private static final RecipeType<KitchenRecipes.Entry> WOK_COOKING =
        RecipeType.create(SakuraBloom.ID, "iron_wok", KitchenRecipes.Entry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(SakuraCuisine.ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new SteepingCategory(gui),
            new CookingCategory(gui, STEAMING, KitchenContent.BAMBOO_STEAMER_ITEM.get()),
            new CookingCategory(gui, WOK_COOKING, KitchenContent.IRON_WOK_ITEM.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(STEAMING, KitchenRecipes.entries(level, KitchenRecipe.Station.STEAMER).toList());
            registration.addRecipes(WOK_COOKING, KitchenRecipes.entries(level, KitchenRecipe.Station.WOK).toList());
        }
        registration.addRecipes(STEEPING, TeaKettleBlock.Stage.BREWS);
        registration.addIngredientInfo(KitchenContent.KETTLE_ITEM.get(),
            Component.translatable("jei.sakura_bloom.tea_kettle.instructions"));
        registration.addIngredientInfo(KitchenContent.CABBAGE.seeds().get(),
            Component.translatable("jei.sakura_bloom.cabbage_seeds"));
        registration.addIngredientInfo(KitchenContent.TOMATO.seeds().get(),
            Component.translatable("jei.sakura_bloom.tomato_seeds"));
        registration.addIngredientInfo(KitchenContent.RICE.seeds().get(),
            Component.translatable("jei.sakura_bloom.rice_seeds"));
        registration.addIngredientInfo(KitchenContent.BAMBOO_STEAMER_ITEM.get(),
            Component.translatable("block.sakura_bloom.bamboo_steamer.hint"));
        registration.addIngredientInfo(KitchenContent.IRON_WOK_ITEM.get(),
            Component.translatable("block.sakura_bloom.iron_wok.hint"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(STEEPING, KitchenContent.KETTLE_ITEM.get());
        registration.addRecipeCatalysts(STEAMING, KitchenContent.BAMBOO_STEAMER_ITEM.get());
        registration.addRecipeCatalysts(WOK_COOKING, KitchenContent.IRON_WOK_ITEM.get());
    }

    private static final class CookingCategory extends StationCategory<KitchenRecipes.Entry> {
        CookingCategory(IGuiHelper gui, RecipeType<KitchenRecipes.Entry> type, ItemLike cookware) {
            super(gui, type, cookware, false);
        }

        @Override
        public int getHeight() {
            return 64;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, KitchenRecipes.Entry recipe, IFocusGroup focuses) {
            var ingredients = recipe.ingredients();
            for (int slot = 0; slot < ingredients.size(); slot++) {
                builder.addInputSlot(5 + 20 * (slot % 3), 5 + 19 * (slot / 3))
                    .setStandardSlotBackground().addIngredients(ingredients.get(slot));
            }
            builder.addOutputSlot(100, 23).setOutputSlotBackground().addItemStack(recipe.preview());
            if (!recipe.serving().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 100, 43)
                    .setStandardSlotBackground().addIngredients(recipe.serving())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.sakura_bloom.serving")));
            }
        }

        @Override
        public ResourceLocation getRegistryName(KitchenRecipes.Entry recipe) {
            return recipe.id();
        }
    }

    private static final class SteepingCategory extends StationCategory<TeaKettleBlock.Stage> {
        SteepingCategory(IGuiHelper gui) {
            super(gui, STEEPING, KitchenContent.KETTLE_ITEM.get(), true);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, TeaKettleBlock.Stage recipe, IFocusGroup focuses) {
            builder.addInputSlot(5, 6).setStandardSlotBackground().addItemStack(TeaKettleBlock.waterBottle());
            builder.addInputSlot(41, 6).setStandardSlotBackground().addItemStack(recipe.petalsStack());
            builder.addOutputSlot(100, 6).setOutputSlotBackground().addItemStack(recipe.brewedTea());
        }

        @Override
        public ResourceLocation getRegistryName(TeaKettleBlock.Stage recipe) {
            return SakuraBloom.id(recipe.getSerializedName());
        }
    }
}
