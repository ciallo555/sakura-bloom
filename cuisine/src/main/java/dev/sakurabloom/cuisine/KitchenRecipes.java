package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.SimpleInput;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityEvaluator;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;
import vectorwing.farmersdelight.common.block.SkilletBlock;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

final class KitchenRecipes {
    private KitchenRecipes() {}

    record Batch(ItemStack meal, Ingredient serving, int ticks) {}

    record Entry(ResourceLocation id, List<Ingredient> ingredients, ItemStack preview, Ingredient serving, int ticks,
                 Function<CraftingInput, ItemStack> cook) {}

    static Stream<Entry> entries(Level level, KitchenRecipe.Station station) {
        Stream<Entry> recipes = level.getRecipeManager().getAllRecipesFor(KitchenContent.COOKING.get()).stream()
            .filter(holder -> holder.value().station() == station)
            .map(holder -> adapt(level, holder, Function.identity(), holder.value().serving(), holder.value().cookingTime()));
        boolean kaleidoscope = ModList.get().isLoaded("kaleidoscope_cookery");
        if (kaleidoscope) {
            recipes = Stream.concat(recipes, Kaleidoscope.entries(level, station));
        }
        if (station == KitchenRecipe.Station.WOK) {
            boolean farmersDelight = ModList.get().isLoaded("farmersdelight");
            if (farmersDelight) {
                recipes = Stream.concat(recipes, FarmersDelight.cookingPot(level));
            }
            if (kaleidoscope) {
                recipes = Stream.concat(recipes, Kaleidoscope.flex(level));
            }
            if (farmersDelight) {
                recipes = Stream.concat(recipes, FarmersDelight.skillet(level));
            }
        }
        return recipes.filter(recipe -> !recipe.preview().isEmpty() && !recipe.ingredients().isEmpty()
            && recipe.ingredients().size() <= KitchenRecipe.MAX_INGREDIENTS);
    }

    static Optional<Batch> match(Level level, KitchenRecipe.Station station, CraftingInput input) {
        return entries(level, station).map(recipe -> new Batch(recipe.cook().apply(input), recipe.serving(), recipe.ticks()))
            .filter(batch -> !batch.meal().isEmpty()).findFirst();
    }

    private static <I extends RecipeInput> Entry adapt(Level level, RecipeHolder<? extends Recipe<I>> holder,
                                                       Function<CraftingInput, @Nullable I> adaptInput,
                                                       Ingredient serving, int ticks) {
        var recipe = holder.value();
        return new Entry(holder.id(), recipe.getIngredients(), recipe.getResultItem(level.registryAccess()),
            serving, Math.max(1, ticks), input -> {
                I adapted = adaptInput.apply(input);
                return adapted != null && recipe.matches(adapted, level)
                    ? recipe.assemble(adapted, level.registryAccess()).copy() : ItemStack.EMPTY;
            });
    }

    // Keep optional API calls in their own classes so either dependency can be absent.
    private static final class Kaleidoscope {
        static Stream<Entry> entries(Level level, KitchenRecipe.Station station) {
            var recipes = level.getRecipeManager();
            if (station == KitchenRecipe.Station.STEAMER) {
                return recipes.getAllRecipesFor(ModRecipes.STEAMER_RECIPE).stream()
                    .filter(holder -> !holder.id().getNamespace().equals(SakuraBloom.ID)
                        || !holder.id().getPath().startsWith(KaleidoscopeServing.RECIPE_PATH))
                    .map(holder -> adapt(level, holder, KitchenRecipes::single, Ingredient.EMPTY, holder.value().getCookTick()));
            }
            return recipes.getAllRecipesFor(ModRecipes.POT_RECIPE).stream().map(holder -> {
                var recipe = holder.value();
                return adapt(level, holder, input -> new SimpleInput(input.items()), recipe.carrier(), recipe.time());
            });
        }

        static Stream<Entry> flex(Level level) {
            return level.getRecipeManager().getAllRecipesFor(ModRecipes.FLEX_POT_RECIPE).stream().map(holder -> {
                var recipe = holder.value();
                var entry = adapt(level, holder, input -> new SimpleInput(input.items()), recipe.carrier(), recipe.time());
                return new Entry(entry.id(), entry.ingredients(), entry.preview(), entry.serving(), entry.ticks(), input -> {
                    ItemStack meal = entry.cook().apply(input);
                    if (!meal.isEmpty() && level instanceof ServerLevel server) {
                        QualityUtils.setQuality(meal, QualityEvaluator.evaluate(input.items(), recipe.ingredients(), holder.id(), server.getSeed()));
                    }
                    return meal;
                });
            });
        }
    }

    private static final class FarmersDelight {
        static Stream<Entry> cookingPot(Level level) {
            return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get()).stream()
                .filter(holder -> !holder.id().getNamespace().equals(SakuraBloom.ID)
                    || !holder.id().getPath().startsWith("compat/farmersdelight/cooking/"))
                .map(holder -> {
                    var recipe = holder.value();
                    ItemStack container = recipe.getOutputContainer();
                    return adapt(level, holder, FarmersDelight::potInput,
                        container.isEmpty() ? Ingredient.EMPTY : Ingredient.of(container), recipe.getCookTime());
                });
        }

        static Stream<Entry> skillet(Level level) {
            return level.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING).stream().map(holder ->
                adapt(level, holder, KitchenRecipes::single, Ingredient.EMPTY,
                    SkilletBlock.getSkilletCookingTime(holder.value().getCookingTime(), 0)));
        }

        @Nullable
        private static RecipeWrapper potInput(CraftingInput input) {
            if (input.ingredientCount() > CookingPotRecipe.INPUT_SLOTS) {
                return null;
            }
            var items = new ItemStackHandler(CookingPotRecipe.INPUT_SLOTS);
            int slot = 0;
            for (ItemStack stack : input.items()) {
                if (!stack.isEmpty()) {
                    items.setStackInSlot(slot++, stack);
                }
            }
            return new RecipeWrapper(items);
        }
    }

    @Nullable
    private static SingleRecipeInput single(CraftingInput input) {
        return input.ingredientCount() == 1 ? new SingleRecipeInput(input.getItem(0)) : null;
    }
}
