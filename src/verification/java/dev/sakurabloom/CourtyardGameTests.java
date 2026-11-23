package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class CourtyardGameTests {
    @GameTest(template = "clearing")
    public static void courtyardCraftingAndSalvage(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var bamboo = Items.BAMBOO.getDefaultInstance();
        var splints = CourtyardContent.BAMBOO_SPLINTS.toStack();
        var split = manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(bamboo, bamboo)), level);
        var bundle = split.orElseThrow().value().getResultItem(level.registryAccess());
        test.assertTrue(bundle.is(splints.getItem()) && bundle.getCount() == 4, "Two bamboo must yield four splints");
        var paper = Items.PAPER.getDefaultInstance();
        test.assertTrue(CourtyardContent.SHOJI.size() == 14, "All 1.21.1 woods and three cherries must have shoji");
        test.assertTrue(CourtyardContent.WOOD_LATTICES.size() == 14, "Each wood needs a carved lattice window");
        for (var screen : CourtyardContent.SHOJI) {
            String wood = screen.getId().getPath().replace("_shoji", "");
            boolean local = List.of("shidare", "yae", "yamazakura").contains(wood);
            var plank = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(local ? SakuraBloom.ID : "minecraft",
                wood + "_planks")).getDefaultInstance();
            var grid = CraftingInput.of(3, 3, List.of(splints, paper, splints, splints, paper, splints, plank, plank, plank));
            var result = manager.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid, level.registryAccess());
            test.assertTrue(result.is(screen.asItem()) && result.getCount() == 4, "Shoji rejected " + wood);
            var windowGrid = CraftingInput.of(3, 3, List.of(plank, splints, plank, splints, ItemStack.EMPTY, splints,
                plank, splints, plank));
            var windows = manager.getRecipeFor(RecipeType.CRAFTING, windowGrid, level).orElseThrow().value()
                .assemble(windowGrid, level.registryAccess());
            var window = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(SakuraBloom.ID, wood + "_lattice"));
            test.assertTrue(window != Blocks.AIR && windows.is(window.asItem()) && windows.getCount() == 4,
                "Window frame did not match its planks: " + wood);
            test.assertTrue(window.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE),
                "Missing axe tag: " + wood);
        }
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        for (var block : CourtyardContent.DECORATIONS) {
            var recipe = manager.byKey(block.getId()).orElseThrow();
            test.assertTrue(recipe.value().getResultItem(level.registryAccess()).is(block.asItem()), "Missing craft for " + block.getId());
            var drops = Block.getDrops(block.get().defaultBlockState(), level, pos, null);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()), "Missing drop for " + block.getId());
        }
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void allGlazedRoofSets(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var brick = Items.BRICK.getDefaultInstance();
        var empty = ItemStack.EMPTY;
        for (var color : DyeColor.values()) {
            var roof = CourtyardContent.ROOFS.get(color);
            var dye = net.minecraft.world.item.DyeItem.byColor(color).getDefaultInstance();
            var grid = CraftingInput.of(3, 3, List.of(empty, brick, empty, brick, dye, brick, empty, brick, empty));
            var made = manager.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid, level.registryAccess());
            test.assertTrue(made.is(roof.tiles().asItem()) && made.getCount() == 2, "Wrong firing for " + color);
            for (var shape : List.of(roof.stairs(), roof.slab())) {
                var result = manager.byKey(SakuraBloom.id(shape.getId().getPath() + "_from_stonecutting")).orElseThrow().value();
                test.assertTrue(result.getType() == RecipeType.STONECUTTING && result.getIngredients().getFirst().test(made)
                    && result.getResultItem(level.registryAccess()).is(shape.asItem())
                    && result.getResultItem(level.registryAccess()).getCount() == (shape == roof.slab() ? 2 : 1), "Wrong cut for " + shape.getId());
            }
            var slab = roof.slab().get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE)
                .setValue(RoofSlabBlock.AXIS, Direction.Axis.X);
            var drops = Block.getDrops(slab, level, test.absolutePos(new BlockPos(2, 2, 2)), null);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(roof.slab().asItem()) && drops.getFirst().getCount() == 2,
                "Double slab did not return both halves for " + color);
            test.assertTrue(slab.rotate(Rotation.CLOCKWISE_90).getValue(RoofSlabBlock.AXIS) == Direction.Axis.Z,
                "Roof ridges did not rotate for " + color);
        }
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void courtyardFurnishingsFitTheirSupports(GameTestHelper test) {
        var pos = new BlockPos(4, 3, 4);
        var level = test.getLevel();
        var absolute = test.absolutePos(pos);
        test.setBlock(pos.above(), Blocks.CHERRY_PLANKS);
        var curtain = CourtyardContent.NOREN.get(DyeColor.BLUE).get().defaultBlockState();
        test.assertTrue(curtain.canSurvive(level, absolute), "Noren should hang from a lintel");
        test.setBlock(pos, curtain);
        test.assertTrue(curtain.getCollisionShape(level, absolute).isEmpty(), "Noren must be walk-through");
        test.setBlock(pos.above(), Blocks.AIR);
        test.assertBlockPresent(Blocks.AIR, pos);
        var palace = CourtyardContent.PALACE_LANTERN.get().defaultBlockState();
        test.setBlock(pos.above(), Blocks.CHAIN);
        test.assertTrue(palace.setValue(LanternBlock.HANGING, true).canSurvive(level, absolute), "Palace lantern needs chain support");
        test.setBlock(pos.below(), Blocks.STONE);
        test.assertTrue(palace.canSurvive(level, absolute) && palace.getLightEmission() == 14, "Standing palace lantern should light the ground");
        test.assertTrue(CourtyardContent.STONE_LANTERN.get().defaultBlockState().getLightEmission() == 13, "Stone lantern must emit light");
        var screen = CourtyardContent.LATTICES.getFirst().get().defaultBlockState();
        var rotated = screen.rotate(Rotation.CLOCKWISE_90);
        var bounds = rotated.getShape(level, absolute, CollisionContext.empty()).bounds();
        test.assertTrue(bounds.getXsize() < bounds.getZsize(), "Rotating a window should rotate its thin collision shape");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void allSixteenNorenCraftAndRedye(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var splint = CourtyardContent.BAMBOO_SPLINTS.toStack();
        for (DyeColor color : DyeColor.values()) {
            var curtain = CourtyardContent.NOREN.get(color);
            var wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool")).getDefaultInstance();
            var dye = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_dye")).getDefaultInstance();
            var grid = CraftingInput.of(3,3,List.of(splint, Items.STRING.getDefaultInstance(), splint, wool,
                ItemStack.EMPTY, wool, wool, ItemStack.EMPTY, wool));
            var made = manager.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid, level.registryAccess());
            test.assertTrue(made.is(curtain.asItem()) && made.getCount() == 2, "Wrong cloth for " + color);
            for (var original : CourtyardContent.NOREN.values()) {
                var swatch = CraftingInput.of(2,1,List.of(original.asItem().getDefaultInstance(), dye));
                var result = manager.getRecipeFor(RecipeType.CRAFTING, swatch, level);
                if (original == curtain) {
                    test.assertTrue(result.isEmpty(), "Same-color cloth would waste dye");
                } else {
                    test.assertTrue(result.orElseThrow().value().assemble(swatch, level.registryAccess()).is(curtain.asItem()),
                        "Failed to dye " + original.getId() + " " + color);
                }
            }
        }
        test.succeed();
    }
}
