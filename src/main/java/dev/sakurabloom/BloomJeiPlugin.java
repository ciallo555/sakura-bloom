package dev.sakurabloom;

import dev.sakurabloom.adventure.shrine.PrayerPreviewClient;
import dev.sakurabloom.adventure.shrine.ShrineAdventure;
import mezz.jei.api.runtime.IJeiRuntime;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public final class BloomJeiPlugin implements IModPlugin {
    private static final RecipeType<RecipeHolder<MortarRecipe>> GRINDING =
        RecipeType.createRecipeHolderType(SakuraBloom.id("grinding"));

    private List<PrayerCategory.Preview> visiblePrayers = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return SakuraBloom.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GrindingCategory(registration.getJeiHelpers().getGuiHelper()),
            new PrayerCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(GRINDING, level.getRecipeManager().getAllRecipesFor(BloomContent.GRINDING.get()));
        }
        visiblePrayers = PrayerCategory.previews(PrayerPreviewClient.current());
        registration.addRecipes(PrayerCategory.TYPE,visiblePrayers);
        registration.addIngredientInfo(BloomContent.MORTAR_ITEM.get(),
            Component.translatable("block.sakura_bloom.flower_mortar.hint"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(PrayerCategory.TYPE,ShrineAdventure.BOX_ITEM.get());
        registration.addRecipeCatalysts(GRINDING, BloomContent.MORTAR_ITEM.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var manager = runtime.getRecipeManager();
        java.util.function.Consumer<List<dev.sakurabloom.adventure.shrine.PrayerRewards.Prize>> update = prizes -> {
            var fresh = PrayerCategory.previews(prizes);
            if (fresh.equals(visiblePrayers)) return;
            manager.hideRecipes(PrayerCategory.TYPE,visiblePrayers);
            manager.addRecipes(PrayerCategory.TYPE,fresh);
            manager.unhideRecipes(PrayerCategory.TYPE,fresh);
            visiblePrayers = fresh;
        };
        PrayerPreviewClient.setListener(update);
        update.accept(PrayerPreviewClient.current());
    }

    @Override
    public void onRuntimeUnavailable() { PrayerPreviewClient.setListener(null); }

    private static final class GrindingCategory extends StationCategory<RecipeHolder<MortarRecipe>> {
        GrindingCategory(IGuiHelper gui) {
            super(gui, GRINDING, BloomContent.MORTAR_ITEM.get(), false);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MortarRecipe> recipe, IFocusGroup focuses) {
            builder.addInputSlot(41, 6).setStandardSlotBackground().addIngredients(recipe.value().getIngredients().getFirst());
            var level = Minecraft.getInstance().level;
            if (level != null) {
                builder.addOutputSlot(100, 6).setOutputSlotBackground().addItemStack(recipe.value().getResultItem(level.registryAccess()));
            }
        }

        @Override
        public ResourceLocation getRegistryName(RecipeHolder<MortarRecipe> recipe) {
            return recipe.id();
        }
    }
}
