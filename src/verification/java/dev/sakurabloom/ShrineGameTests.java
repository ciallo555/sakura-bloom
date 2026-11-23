package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class ShrineGameTests {
    private record Theme(String wood, String blockNamespace, String biome, String roof,
                         String stoneStairs, String pathStairs, String paving, String plaque,
                         BlockPos landmark, String landmarkBlock, int floor, int hallZ, int eaveZ, int eaveY) {}

    @GameTest(template = "clearing", skyAccess = true)
    public static void shrineThemesLoadAndPlace(GameTestHelper test) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        var chunkSource = level.getChunkSource();
        var generator = chunkSource.getGenerator();
        var manager = level.getStructureManager();
        BlockPos origin = new BlockPos(3, 2, 3);
        BlockPos absolute = test.absolutePos(origin);

        var key = ResourceKey.create(Registries.STRUCTURE_SET, SakuraBloom.id("shrines"));
        var set = registries.registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(key).value();
        var themes = List.of(
            new Theme("cherry", "minecraft", "minecraft:cherry_grove", "pink",
                "stone_brick_stairs", "stone_stairs", "smooth_stone", "花染之庭",
                new BlockPos(6,4,11), "sakura_bloom:flower_mortar", 3,16,14,8),
            new Theme("shidare", SakuraBloom.ID, "sakura_bloom:shidare_meadow", "light_blue",
                "polished_diorite_stairs", "diorite_stairs", "calcite", "垂樱水榭",
                new BlockPos(7,2,11), "minecraft:water", 3,15,13,8),
            new Theme("yae", SakuraBloom.ID, "sakura_bloom:yae_grove", "red",
                "polished_andesite_stairs", "brick_stairs", "bricks", "八重灯殿",
                new BlockPos(5,6,10), "sakura_bloom:silk_palace_lantern", 3,14,12,8),
            new Theme("yamazakura", SakuraBloom.ID, "sakura_bloom:yamazakura_woodland", "green",
                "tuff_brick_stairs", "mossy_stone_brick_stairs", "mossy_stone_bricks", "山樱遥拜",
                new BlockPos(11,6,14), "minecraft:tuff_brick_stairs", 6,17,15,11)
        );
        test.assertTrue(set.structures().size() == themes.size(), "The shared placement set did not include all four themes");

        for (Theme theme : themes) {
            var shrineKey = ResourceKey.create(Registries.STRUCTURE, SakuraBloom.id(theme.wood + "_shrine"));
            var holder = structures.getHolderOrThrow(shrineKey);
            var structure = holder.value();
            var biomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(theme.biome));
            var biome = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(biomeKey);
            test.assertTrue(structure.biomes().size() == 1 && structure.biomes().contains(biome),
                "Wrong generation biome for " + theme.wood);
            test.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().equals(holder)),
                "Missing variant in the common placement set");

            // The flat test world can validate assembly of the piece without sampling the overworld biome source.
            var start = structure.generate(registries, generator, generator.getBiomeSource(), chunkSource.randomState(),
                manager, level.getSeed(), new ChunkPos(absolute), 0, level, ignored -> true);
            test.assertTrue(start.isValid() && start.getPieces().size() == 1, "Jigsaw did not assemble " + theme.wood);

            var template = manager.get(SakuraBloom.id("shrine/" + theme.wood)).orElseThrow();
            boolean placed = template.placeInWorld(level, absolute, absolute, new StructurePlaceSettings().setKnownShape(true),
                level.random, 18);
            test.assertTrue(placed, "Unable to place template for " + theme.wood);
            var chest = theme.wood.equals("cherry") ? Blocks.CHEST : BuiltInRegistries.BLOCK.get(SakuraBloom.id(theme.wood + "_chest"));
            TravelGameTests.checkChest(test, template, absolute, chest, "shrine_offerings");
            for (int x = 6; x <= 16; x++) {
                String stairs = x >= 10 && x <= 12 ? theme.pathStairs : theme.stoneStairs;
                assertEntranceStep(test, origin.offset(x, 1, 0), stairs);
                assertEntranceStep(test, origin.offset(x, 2, 1), stairs);
            }
            assertBlock(test, origin.offset(11, 2, 2), "minecraft:" + theme.paving);
            assertBlock(test, origin.offset(8, 4, 3), theme.blockNamespace + ":" + theme.wood + "_log");
            assertBlock(test, origin.offset(11, theme.floor, theme.hallZ), theme.blockNamespace + ":" + theme.wood + "_planks");
            assertBlock(test, origin.offset(11, theme.floor+1, theme.hallZ+1), "sakura_bloom:bound_tatami");
            test.assertBlockPresent(Blocks.AIR, origin.offset(11, theme.floor+1, theme.hallZ));
            test.assertBlockPresent(Blocks.AIR, origin.offset(11, theme.floor+2, theme.hallZ));
            int roofX = theme.wood.equals("shidare") || theme.wood.equals("yamazakura") ? 5 : 11;
            assertBlock(test, origin.offset(roofX, theme.eaveY, theme.eaveZ), "sakura_bloom:" + theme.roof + "_roof_tile_stairs");
            assertBlock(test, origin.offset(theme.landmark), theme.landmarkBlock);
            assertBlock(test, origin.offset(15, 3, 5), "minecraft:water_cauldron");
            assertBlock(test, origin.offset(11, 8, 3), theme.blockNamespace + ":" + theme.wood + "_hanging_sign");
            SignBlockEntity plaque = test.getBlockEntity(origin.offset(11, 8, 3));
            test.assertTrue(plaque.getFrontText().getMessage(1, false).getString().equals(theme.plaque),
                "Wrong shrine name on the torii for " + theme.wood);
            // Jigsaw may turn the template. Check the supported hanging decor after a quarter-turn.
            var turn = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90)
                .setRotationPivot(new BlockPos(11,0,11)).setKnownShape(true);
            template.placeInWorld(level, absolute, absolute, turn, level.random, 18);
            var lintel = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                .calculateRelativePosition(turn, new BlockPos(11, theme.floor+3, theme.hallZ));
            var curtain = test.getBlockState(origin.offset(lintel));
            test.assertTrue(curtain.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                && curtain.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.EAST
                && curtain.canSurvive(level, absolute.offset(lintel)), "Door curtain did not rotate with the hall");

        }
        test.succeed();
    }

    private static void assertEntranceStep(GameTestHelper test, BlockPos pos, String stairs) {
        assertBlock(test, pos, "minecraft:" + stairs);
        var state = test.getBlockState(pos);
        test.assertTrue(state.getValue(StairBlock.FACING) == Direction.SOUTH && state.getValue(StairBlock.HALF) == Half.BOTTOM,
            "The stairs should rise toward the torii");
        test.assertBlockPresent(Blocks.AIR, pos.above());
        test.assertBlockPresent(Blocks.AIR, pos.above(2));
    }

    private static void assertBlock(GameTestHelper test, BlockPos pos, String id) {
        var expected = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        test.assertBlockPresent(expected, pos);
    }
}
