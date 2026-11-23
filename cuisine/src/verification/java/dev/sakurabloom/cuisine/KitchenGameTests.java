package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class KitchenGameTests {
    private static final BlockPos STATION = new BlockPos(3, 3, 3);

    @GameTest(template = "clearing")
    public static void grassDoesNotDropKitchenSeeds(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(STATION);
        test.setBlock(STATION.below(), Blocks.DIRT);
        for (Block block : List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS)) {
            var grass = block.defaultBlockState();
            if (block == Blocks.TALL_GRASS) {
                DoublePlantBlock.placeAt(level, grass, pos, Block.UPDATE_ALL);
            } else {
                test.setBlock(STATION, grass);
            }
            var table = level.getServer().reloadableRegistries().getLootTable(block.getLootTable());
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, grass)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).create(LootContextParamSets.BLOCK);
            var drops = new HashSet<Item>();
            var lootSeeds = new Random(42);
            for (int roll = 0; roll < 256; roll++) {
                table.getRandomItems(params, lootSeeds.nextLong()).forEach(stack -> drops.add(stack.getItem()));
            }
            test.assertTrue(drops.contains(Items.WHEAT_SEEDS), block + " should still drop vanilla wheat seeds");
            for (var crop : List.of(KitchenContent.CABBAGE, KitchenContent.TOMATO, KitchenContent.RICE)) {
                test.assertFalse(drops.contains(crop.seeds().get()), block + " dropped " + crop.seeds().get());
            }
        }
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void seedsGrowAndReturnOnHarvest(GameTestHelper test) {
        var level = test.getLevel();
        var crops = List.of(KitchenContent.CABBAGE, KitchenContent.TOMATO, KitchenContent.RICE);
        var food = List.of(KitchenContent.CABBAGE_ITEM.get(), KitchenContent.TOMATO_ITEM.get(), KitchenContent.RICE_ITEM.get());
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        for (int index = 0; index < crops.size(); index++) {
            var crop = crops.get(index);
            BlockPos soil = STATION.east(index * 2);
            BlockPos plant = soil.above();
            BlockPos absolute = test.absolutePos(plant);
            var block = crop.block().get();
            var seeds = crop.seeds().toStack();
            test.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
            var target = new UseOnContext(player, InteractionHand.MAIN_HAND, hit(test.absolutePos(soil)));
            if (crop == KitchenContent.RICE) {
                test.assertFalse(seeds.getItem().useOn(target).consumesAction(), "Hydrated soil alone should not plant paddy rice");
                test.setBlock(plant, Blocks.WATER);
            }
            test.assertTrue(seeds.getItem().useOn(target).consumesAction(), "Planting the seed failed");
            test.assertTrue(seeds.isEmpty(), "Planting must consume one seed in survival");
            test.assertBlockPresent(block, plant);
            test.assertTrue(block.isValidBonemealTarget(level, absolute, test.getBlockState(plant)), "Crop should grow on hydrated soil");
            for (int step = 0; step < 4 && !block.isMaxAge(test.getBlockState(plant)); step++) {
                block.performBonemeal(level, level.random, absolute, test.getBlockState(plant));
            }
            var grown = test.getBlockState(plant);
            test.assertTrue(block.isMaxAge(grown), "Crop did not mature");
            var drops = Block.getDrops(grown, level, absolute, null);
            Item seed = crop.seeds().get();
            Item produce = food.get(index);
            test.assertTrue(drops.stream().anyMatch(stack -> stack.is(seed)) && drops.stream().anyMatch(stack -> stack.is(produce)),
                "Mature crop must return a seed and the produce");
        }
        test.succeed();
    }

    @GameTest(template = "clearing", timeoutTicks = 145)
    public static void cookwareRunsWithHeatAndRetainsItsContents(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos stove = STATION.below();
        BlockPos wok = STATION.east(3);
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        test.setBlock(STATION, KitchenContent.BAMBOO_STEAMER.get());
        test.setBlock(wok, KitchenContent.IRON_WOK.get());
        var campfire = Blocks.CAMPFIRE.defaultBlockState();
        test.setBlock(stove, campfire.setValue(BlockStateProperties.LIT, false));
        test.setBlock(wok.below(), Blocks.MAGMA_BLOCK);
        ItemStack rice = KitchenContent.RICE_ITEM.toStack(2);
        use(test, STATION, player, rice);
        test.assertTrue(rice.getCount() == 1, "Cookware should insert one item per interaction");
        ItemStack unusedBowl = new ItemStack(Items.BOWL);
        use(test, STATION, player, unusedBowl);
        test.assertTrue(unusedBowl.getCount() == 1, "A bowl is only consumed when serving");
        var display = new KitchenBlockEntity(test.absolutePos(STATION), test.getBlockState(STATION));
        var stored = test.<KitchenBlockEntity>getBlockEntity(STATION);
        display.loadWithComponents(stored.getUpdateTag(level.registryAccess()), level.registryAccess());
        test.assertTrue(display.input().ingredientCount() == 1 && display.displayItems().getFirst().is(KitchenContent.RICE_ITEM.get()),
            "The client should receive the inserted ingredient");
        use(test, STATION, player, ItemStack.EMPTY);
        test.assertFalse(test.getBlockState(STATION).getValue(KitchenBlock.COOKING), "Unlit campfire started cooking");
        test.setBlock(stove, campfire);
        use(test, STATION, player, ItemStack.EMPTY);
        test.assertTrue(test.getBlockState(STATION).getValue(KitchenBlock.COOKING), "Valid inputs did not start cooking");
        use(test, wok, player, new ItemStack(Items.EGG));
        use(test, wok, player, new ItemStack(Items.CARROT));
        use(test, wok, player, KitchenContent.RICE_ITEM.toStack());
        use(test, wok, player, ItemStack.EMPTY);
        test.assertTrue(test.getBlockState(wok).getValue(KitchenBlock.COOKING), "Wok did not match the recipe in reverse order");
        test.runAfterDelay(20, () -> {
            test.setBlock(stove, campfire.setValue(BlockStateProperties.LIT, false));
            var data = test.<KitchenBlockEntity>getBlockEntity(STATION);
            var saved = data.saveWithoutMetadata(level.registryAccess());
            data.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(data.cooking() && data.input().ingredientCount() == 1 && data.serving().test(new ItemStack(Items.BOWL)),
                "Cooking progress and serving vessel should survive saving");
        });
        test.runAfterDelay(45, () -> {
            test.assertTrue(test.<KitchenBlockEntity>getBlockEntity(STATION).cooking(), "Cooking should remain pending after heat is lost");
            test.setBlock(stove, campfire);
        });
        test.runAfterDelay(120, () -> {
            test.assertTrue(test.<KitchenBlockEntity>getBlockEntity(STATION).ready(), "Steamer did not resume after heat returned");
            use(test, STATION, player, ItemStack.EMPTY);
            var cookedRice = BuiltInRegistries.ITEM.get(KitchenContent.STEAMED_RICE_ID);
            test.assertTrue(stored.ready() && player.getInventory().countItem(cookedRice) == 0, "Rice needs its serving bowl");
            var bowls = new ItemStack(Items.BOWL, 2);
            use(test, STATION, player, bowls);
            test.assertTrue(bowls.getCount() == 1, "Serving one dish should consume one bowl");
            test.assertTrue(player.getInventory().countItem(cookedRice) == 1, "Steamed rice was not collected");
            use(test, wok, player, new ItemStack(Items.BOWL));
            test.assertTrue(player.getInventory().countItem(KitchenContent.EGG_FRIED_RICE.get()) == 1, "Stir-fry was not collected");
            test.assertFalse(test.getBlockState(wok).getValue(KitchenBlock.COOKING), "Wok did not reset its state");
            var bowl = KitchenContent.EGG_FRIED_RICE.toStack().get(DataComponents.FOOD).usingConvertsTo().orElseThrow();
            test.assertTrue(bowl.is(Items.BOWL), "Eating plated dishes must return the bowl");
            test.succeed();
        });
    }

    @GameTest(template = "clearing")
    public static void recipesMatchAndFarmerCompatibilityIsOptional(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var recipes = manager.getAllRecipesFor(KitchenContent.COOKING.get());
        boolean kaleidoscope = ModList.get().isLoaded("kaleidoscope_cookery");
        test.assertTrue(recipes.size() == (kaleidoscope ? 6 : 7), "Each standalone dish should have its active recipe");
        boolean farmersDelight = ModList.get().isLoaded("farmersdelight");
        var plainRiceRecipe = SakuraBloom.id(farmersDelight ? "cooking/cooked_rice" : "cooking/steamed_rice");
        var cookedRiceId = farmersDelight ? KitchenContent.FARMERS_DELIGHT_COOKED_RICE_ID : KitchenContent.STEAMED_RICE_ID;
        test.assertTrue(BuiltInRegistries.ITEM.keySet().contains(KitchenContent.STEAMED_RICE_ID) != farmersDelight,
            "There should be exactly one registered cooked rice item for this setup");
        var oldRiceData = new CompoundTag();
        oldRiceData.putString("id", KitchenContent.STEAMED_RICE_ID.toString());
        oldRiceData.putInt("count", 3);
        var oldRice = ItemStack.parseOptional(level.registryAccess(), oldRiceData);
        test.assertTrue(oldRice.is(BuiltInRegistries.ITEM.get(cookedRiceId)) && oldRice.getCount() == 3,
            "A legacy rice stack should keep its count and resolve to the active item");

        for (var holder : recipes) {
            var recipe = holder.value();
            var stacks = recipe.getIngredients().stream().map(ingredient -> ingredient.getItems()[0]).toList();
            test.assertTrue(recipe.matches(CraftingInput.of(stacks.size(), 1, stacks), level), "Invalid ingredients for " + holder.id());
            if (holder.id().equals(plainRiceRecipe)) {
                test.assertTrue(recipe.getResultItem(level.registryAccess()).is(BuiltInRegistries.ITEM.get(cookedRiceId)),
                    "Steaming should give the active cooked rice item");
                test.assertTrue(recipe.serving().test(new ItemStack(Items.BOWL)), "Rice should be served in a bowl");
                continue;
            }
            String name = holder.id().getPath().substring("cooking/".length());
            var compat = manager.byKey(SakuraBloom.id("compat/farmersdelight/cooking/" + name));
            test.assertTrue(compat.isPresent() == farmersDelight, "Cooking pot recipe did not respect dependency presence");
            compat.ifPresent(pot -> test.assertTrue(ItemStack.isSameItem(
                pot.value().getResultItem(level.registryAccess()), recipe.getResultItem(level.registryAccess())),
                "Cooking pot recipe produced the wrong dish"));
        }
        test.assertTrue(manager.byKey(plainRiceRecipe).isPresent(), "The active steamer recipe is missing");
        test.assertTrue(manager.byKey(SakuraBloom.id("compat/farmersdelight/cooking/steamed_rice")).isEmpty(),
            "The pot should use Farmer's Delight's own rice recipe");
        if (farmersDelight) {
            var fdRice = BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:rice"));
            var riceRecipe = recipes.stream().filter(entry -> entry.id().equals(plainRiceRecipe)).findFirst().orElseThrow();
            var input = CraftingInput.of(1, 1, List.of(new ItemStack(fdRice)));
            test.assertTrue(riceRecipe.value().matches(input, level), "Steamer should accept Farmer's Delight rice");
            var fdCookedRice = manager.byKey(ResourceLocation.parse("farmersdelight:cooking/cooked_rice")).orElseThrow();
            for (ItemStack grain : List.of(KitchenContent.RICE_ITEM.toStack(), new ItemStack(fdRice))) {
                var potMatches = manager.getRecipes().stream()
                    .filter(entry -> entry.value().getType() == fdCookedRice.value().getType())
                    .filter(entry -> entry.value().getIngredients().size() == 1 && entry.value().getIngredients().getFirst().test(grain))
                    .toList();
                test.assertTrue(potMatches.equals(List.of(fdCookedRice)), "One pot recipe should match either kind of rice");
            }
            test.assertTrue(manager.byKey(SakuraBloom.id("compat/farmersdelight/cutting/cabbage")).isPresent(),
                "Cabbage cutting board recipe is missing");
            var fdStove = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("farmersdelight:stove"));
            test.setBlock(STATION.below(), fdStove.defaultBlockState().setValue(BlockStateProperties.LIT, true));
            test.assertTrue(KitchenBlock.hasHeat(level, test.absolutePos(STATION)), "Cookware did not recognize the lit stove");
            test.setBlock(STATION.below(), fdStove.defaultBlockState().setValue(BlockStateProperties.LIT, false));
            test.assertFalse(KitchenBlock.hasHeat(level, test.absolutePos(STATION)), "Cookware recognized an extinguished stove as hot");
        }
        test.succeed();
    }

    @GameTest(template = "clearing", timeoutTicks = 260)
    public static void externalRecipesUseTheirIngredientsAndServingVessels(GameTestHelper test) {
        var level = test.getLevel();
        var wok = KitchenRecipe.Station.WOK;
        var steamer = KitchenRecipe.Station.STEAMER;
        if (ModList.get().isLoaded("farmersdelight")) {
            var stew = matchRecipe(test, wok, "farmersdelight:cooking/beef_stew", "farmersdelight:beef_stew",
                new ItemStack(Items.BEEF), new ItemStack(Items.CARROT), new ItemStack(Items.POTATO));
            test.assertTrue(stew.serving().test(new ItemStack(Items.BOWL)), "Farmer's Delight stew should keep its serving bowl");
            matchRecipe(test, wok, "minecraft:cooked_beef_from_campfire_cooking", "minecraft:cooked_beef", new ItemStack(Items.BEEF));
        }
        if (!ModList.get().isLoaded("kaleidoscope_cookery")) {
            test.succeed();
            return;
        }
        var tomatoInput = CraftingInput.of(2, 1, List.of(KitchenContent.TOMATO_ITEM.toStack(), new ItemStack(Items.EGG)));
        var tomatoDish = KitchenRecipes.match(level, wok, tomatoInput).orElseThrow();
        var kaleidoscopeTomatoEgg = BuiltInRegistries.ITEM.get(KitchenContent.KALEIDOSCOPE_TOMATO_EGG_ID);
        test.assertTrue(tomatoDish.meal().is(kaleidoscopeTomatoEgg), "The shared tomato and egg recipe should be the Kaleidoscope one");
        var oldTomatoEgg = new CompoundTag();
        oldTomatoEgg.putString("id", KitchenContent.TOMATO_EGG_ID.toString());
        oldTomatoEgg.putInt("count", 2);
        var migrated = ItemStack.parseOptional(level.registryAccess(), oldTomatoEgg);
        test.assertTrue(migrated.is(kaleidoscopeTomatoEgg) && migrated.getCount() == 2, "Legacy tomato and eggs should keep their servings");
        matchRecipe(test, steamer, "kaleidoscope_cookery:steamer/mantou", "kaleidoscope_cookery:mantou",
            item("kaleidoscope_cookery:raw_dough"));
        var pie = matchRecipe(test, wok, "kaleidoscope_cookery:flex_pot/fondant_pie", "kaleidoscope_cookery:fondant_pie",
            new ItemStack(Items.SUGAR), new ItemStack(Items.PUMPKIN_PIE));
        test.assertTrue(pie.serving().test(new ItemStack(Items.BOWL)), "Kaleidoscope pie should keep its serving bowl");

        test.setBlock(STATION.below(), Blocks.MAGMA_BLOCK);
        test.setBlock(STATION, KitchenContent.IRON_WOK.get());
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var meatballs = item("kaleidoscope_cookery:raw_meatball");
        meatballs.setCount(9);
        for (int slot = 0; slot < 9; slot++) {
            use(test, STATION, player, meatballs);
        }
        var cookware = test.<KitchenBlockEntity>getBlockEntity(STATION);
        test.assertTrue(meatballs.isEmpty() && cookware.input().ingredientCount() == 9, "Wok should accept all nine ingredients");
        use(test, STATION, player, ItemStack.EMPTY);
        test.assertTrue(cookware.cooking(), "The registered nine-ingredient recipe should start through the wok");
        test.runAfterDelay(215, () -> {
            use(test, STATION, player, ItemStack.EMPTY);
            test.assertTrue(player.getInventory().countItem(item("kaleidoscope_cookery:cooked_meatball").getItem()) == 9,
                "Kaleidoscope wok should return the real nine-item result");
            test.succeed();
        });
    }

    @GameTest(template = "clearing", timeoutTicks = 25)
    public static void preparedMealsPersistUntilServed(GameTestHelper test) {
        var level = test.getLevel();
        var absolute = test.absolutePos(STATION);
        test.setBlock(STATION.below(), Blocks.MAGMA_BLOCK);
        test.setBlock(STATION, KitchenContent.BAMBOO_STEAMER.get());
        var cookware = test.<KitchenBlockEntity>getBlockEntity(STATION);
        cookware.start(new KitchenRecipes.Batch(KitchenContent.CHAWANMUSHI.toStack(2), Ingredient.of(Items.BOWL), 1));
        test.runAfterDelay(3, () -> {
            var player = test.makeMockPlayer(GameType.SURVIVAL);
            use(test, STATION, player, new ItemStack(Items.BOWL));
            test.assertTrue(cookware.ready() && cookware.displayItems().getFirst().getCount() == 1,
                "One bowl should take just one serving from a batch");
            var drops = Block.getDrops(test.getBlockState(STATION), level, absolute, cookware);
            var portable = drops.stream().filter(stack -> stack.is(KitchenContent.BAMBOO_STEAMER_ITEM.get())).findFirst().orElseThrow();
            var payload = portable.get(DataComponents.BLOCK_ENTITY_DATA);
            test.assertTrue(payload != null, "A packed steamer should carry its unserved meal");
            var unpacked = new KitchenBlockEntity(absolute, test.getBlockState(STATION));
            unpacked.loadWithComponents(payload.copyTag(), level.registryAccess());
            test.assertTrue(unpacked.ready() && unpacked.serving().test(new ItemStack(Items.BOWL))
                && unpacked.displayItems().getFirst().is(KitchenContent.CHAWANMUSHI.get()),
                "The packed meal should still use a bowl when placed again");
            test.succeed();
        });
    }

    @GameTest(template = "clearing", timeoutTicks = 180)
    public static void kaleidoscopeSteamerCooksBloomDishes(GameTestHelper test) {
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            Kaleidoscope.steam(test);
        } else {
            test.assertTrue(test.getLevel().getRecipeManager().byKey(
                SakuraBloom.id(KaleidoscopeServing.RECIPE_PATH + "steamed_rice")).isEmpty(),
                "The external recipes should load only with Kaleidoscope Cookery");
            test.succeed();
        }
    }

    private static final class Kaleidoscope {
        static void steam(GameTestHelper test) {
            var level = test.getLevel();
            var manager = level.getRecipeManager();
            var rice = KitchenContent.RICE_ITEM.toStack();
            var onigiri = craft(test, "compat/kaleidoscope/preparation/salmon_onigiri", rice.copy(),
                new ItemStack(Items.DRIED_KELP), new ItemStack(Items.COOKED_SALMON));
            var eggMix = craft(test, "compat/kaleidoscope/preparation/chawanmushi",
                new ItemStack(Items.EGG), new ItemStack(Items.BROWN_MUSHROOM));
            var roll = craft(test, "compat/kaleidoscope/preparation/steamed_pork_roll",
                KitchenContent.CABBAGE_ITEM.toStack(), new ItemStack(Items.BEEF), rice.copy());
            var ingredients = List.of(rice, onigiri, eggMix, roll);
            var dishIds = List.of("steamed_rice", "salmon_onigiri", "chawanmushi", "steamed_pork_roll");
            var cooked = List.of(KitchenContent.UNPLATED_STEAMED_RICE.get(), KitchenContent.SALMON_ONIGIRI.get(),
                KitchenContent.UNPLATED_CHAWANMUSHI.get(), KitchenContent.STEAMED_PORK_ROLL.get());
            var nativeRecipes = manager.getAllRecipesFor(ModRecipes.STEAMER_RECIPE);
            var testState = ModBlocks.STEAMER.get().defaultBlockState()
                .setValue(SteamerBlock.HALF, true).setValue(SteamerBlock.HAS_BASE, true).setValue(SteamerBlock.HAS_LID, false);
            test.setBlock(STATION.below(), Blocks.CAMPFIRE);
            test.setBlock(STATION, testState);
            var steamer = test.<SteamerBlockEntity>getBlockEntity(STATION);
            var player = test.makeMockPlayer(GameType.SURVIVAL);
            for (int slot = 0; slot < ingredients.size(); slot++) {
                var dish = dishIds.get(slot);
                var ingredient = ingredients.get(slot);
                var matches = nativeRecipes.stream()
                    .filter(holder -> holder.value().matches(new SingleRecipeInput(ingredient), level)).toList();
                test.assertTrue(matches.size() == 1 && matches.getFirst().id().equals(
                    SakuraBloom.id(KaleidoscopeServing.RECIPE_PATH + dish)), "Expected one native recipe for " + dish);
                use(test, STATION, player, ingredient);
                test.assertTrue(ingredient.isEmpty(), "Kaleidoscope's steamer should accept " + dish);
            }
            boolean farmersDelight = ModList.get().isLoaded("farmersdelight");
            if (farmersDelight) {
                var fdRiceInput = new SingleRecipeInput(item("farmersdelight:rice"));
                test.assertTrue(nativeRecipes.stream().filter(holder -> holder.value().matches(fdRiceInput, level)).toList()
                    .equals(List.of(manager.byKey(SakuraBloom.id(KaleidoscopeServing.RECIPE_PATH + "steamed_rice")).orElseThrow())),
                    "Farmer's Delight rice should have the same native steamer recipe");
            }
            test.assertFalse(KitchenRecipes.entries(level, KitchenRecipe.Station.STEAMER)
                .anyMatch(recipe -> recipe.id().getPath().startsWith(KaleidoscopeServing.RECIPE_PATH)),
                "Bloom's steamer should use its original recipes for these meals");
            var bowls = new ItemStack(Items.BOWL, 3);
            test.assertTrue(useBowl(test, player, bowls) && bowls.getCount() == 3,
                "Using a bowl before cooking finishes should leave the steamer contents and bowls intact");
            test.setBlock(STATION, testState.setValue(SteamerBlock.HAS_LID, true));

            test.runAtTickTime(150, () -> {
                for (int slot = 0; slot < cooked.size(); slot++) {
                    test.assertTrue(steamer.getItems().get(slot).is(cooked.get(slot)), "Steamer output for " + dishIds.get(slot));
                }
                var riceId = farmersDelight ? KitchenContent.FARMERS_DELIGHT_COOKED_RICE_ID : KitchenContent.STEAMED_RICE_ID;
                var cookedRice = BuiltInRegistries.ITEM.get(riceId);
                test.assertTrue(craft(test, "serving/steamed_rice", KitchenContent.UNPLATED_STEAMED_RICE.toStack(),
                    new ItemStack(Items.BOWL)).is(cookedRice), "Rice plated on a crafting grid should use the active rice item");
                test.assertTrue(craft(test, "serving/chawanmushi", KitchenContent.UNPLATED_CHAWANMUSHI.toStack(),
                    new ItemStack(Items.BOWL)).is(KitchenContent.CHAWANMUSHI.get()), "Chawanmushi should be plateable after collection");
                test.assertTrue(useBowl(test, player, bowls) && useBowl(test, player, bowls) && bowls.getCount() == 1,
                    "Serving two dishes should consume two bowls");
                test.assertTrue(player.getInventory().countItem(cookedRice) == 1
                    && player.getInventory().countItem(KitchenContent.CHAWANMUSHI.get()) == 1,
                    "Bowl interactions should give one serving of rice and one of chawanmushi");
                test.assertFalse(useBowl(test, player, bowls), "There should be no unserved bowl dish left");
                use(test, STATION, player, ItemStack.EMPTY);
                test.assertTrue(player.getInventory().countItem(KitchenContent.SALMON_ONIGIRI.get()) == 1
                    && player.getInventory().countItem(KitchenContent.STEAMED_PORK_ROLL.get()) == 1,
                    "The other two dishes should remain available through the native steamer interaction");
                test.assertTrue(steamer.getItems().stream().allMatch(ItemStack::isEmpty), "The steamer should be empty after collecting");
                test.succeed();
            });
        }

        private static ItemStack craft(GameTestHelper test, String path, ItemStack... stacks) {
            var level = test.getLevel();
            var input = CraftingInput.of(stacks.length, 1, List.of(stacks));
            var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            test.assertTrue(match.isPresent() && match.get().id().equals(SakuraBloom.id(path)), "Crafting recipe for " + path);
            return match.orElseThrow().value().assemble(input, level.registryAccess());
        }

        private static boolean useBowl(GameTestHelper test, Player player, ItemStack bowls) {
            player.setItemInHand(InteractionHand.MAIN_HAND, bowls);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit(test.absolutePos(STATION)));
            return NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(context, UseItemOnBlockEvent.UsePhase.BLOCK)).isCanceled();
        }
    }

    private static KitchenRecipes.Entry matchRecipe(GameTestHelper test, KitchenRecipe.Station station, String recipe,
                                                   String result, ItemStack... ingredients) {
        var level = test.getLevel();
        var entry = KitchenRecipes.entries(level, station)
            .filter(candidate -> candidate.id().equals(ResourceLocation.parse(recipe))).findFirst().orElseThrow();
        var output = entry.cook().apply(CraftingInput.of(ingredients.length, 1, List.of(ingredients)));
        test.assertTrue(output.is(item(result).getItem()), "Wrong adapted result for " + recipe);
        return entry;
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void use(GameTestHelper test, BlockPos pos, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var state = test.getBlockState(pos);
        var hit = hit(test.absolutePos(pos));
        var result = state.useItemOn(stack, test.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(test.getLevel(), player, hit);
        }
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
