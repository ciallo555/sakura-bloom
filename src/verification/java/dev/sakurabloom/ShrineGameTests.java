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
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.EnumSet;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class ShrineGameTests {
    private record Theme(String wood, String blockNamespace, String biome, String roof,
                         String stoneStairs, String pathStairs, String paving,
                         BlockPos landmark, String landmarkBlock, BlockPos roofTile, BlockPos doorway,
                         List<BlockPos> path) {}

    static BlockPos p(int x, int y, int z) { return new BlockPos(x, y, z); }

    @GameTest(template = "clearing")
    public static void gardensNeedDryGroundAndAvoidPortalAprons(GameTestHelper test) {
        var level = test.getLevel();
        var access = level.registryAccess();
        var grove = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.CHERRY_GROVE);
        var shrine = access.registryOrThrow(Registries.STRUCTURE).get(SakuraBloom.id("cherry_shrine"));
        var random = level.getChunkSource().randomState();
        for (boolean flooded : List.of(false, true)) {
            var layers = new FlatLevelGeneratorSettings(Optional.empty(), grove, List.of());
            layers.getLayersInfo().add(new FlatLayerInfo(80, Blocks.STONE));
            if (flooded) layers.getLayersInfo().add(new FlatLayerInfo(1, Blocks.WATER));
            layers.updateLayers();
            var generator = new FlatLevelSource(layers);
            var start = shrine.generate(access, generator, generator.getBiomeSource(), random, level.getStructureManager(),
                159033431L, new ChunkPos(0, 0), 0, level, shrine.biomes()::contains);
            test.assertTrue(start.isValid() == !flooded, "A dry courtyard must fit, one block of surface water must disqualify it");
            if (!flooded) {
                var piece = (PoolElementStructurePiece)start.getPieces().getFirst();
                test.assertTrue(piece.getPosition().getY()+2 == level.getMinBuildHeight()+79,
                    "Courtyard paving must be flush with the natural grass block");
            }
        }
        // This seed put the red shrine's actual staircase inside a ruined portal's lava in the first field trial.
        var sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        var state = level.getChunkSource().getGenerator().createState(sets.asLookup(), random, 159033431L);
        for (String name : List.of("shrines", "woodland_landmarks"))
            test.assertFalse(sets.get(SakuraBloom.id(name)).placement().applyInteractionsWithOtherStructures(state, -423, 542),
                name + " allowed the portal apron to cut into the courtyard");
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void rotatedGardensMeetTheApproachOnSlopingTerrain(GameTestHelper test) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        var grove = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.CHERRY_GROVE);
        var structure = registries.registryOrThrow(Registries.STRUCTURE).get(SakuraBloom.id("cherry_shrine"));
        var random = level.getChunkSource().randomState();
        var layers = new FlatLevelGeneratorSettings(Optional.empty(), grove, List.of());
        layers.getLayersInfo().add(new FlatLayerInfo(80, Blocks.STONE));
        layers.updateLayers();
        var hillside = new FlatLevelSource(layers) {
            @Override
            public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor world, RandomState state) {
                return super.getBaseHeight(x, z, type, world, state) + Math.floorDiv(x, 20);
            }
        };
        var seen = EnumSet.noneOf(Rotation.class);
        boolean centerDiffers = false;
        for (int i = 0; i < 64 && (seen.size() < 4 || !centerDiffers); i++) {
            var start = structure.generate(registries, hillside, hillside.getBiomeSource(), random, level.getStructureManager(),
                89021L + i, new ChunkPos(i % 3, i % 5), 0, level, structure.biomes()::contains);
            test.assertTrue(start.isValid(), "A gentle hillside should admit a shrine");
            var piece = (PoolElementStructurePiece)start.getPieces().getFirst();
            seen.add(piece.getRotation());
            int[] road = new int[3];
            for (int j = 0; j < road.length; j++) {
                var pos = p(12 + j * 3, 0, -1).rotate(piece.getRotation()).offset(piece.getPosition());
                road[j] = hillside.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, random)-1;
            }
            java.util.Arrays.sort(road);
            test.assertTrue(road[1] == piece.getPosition().getY() + 2, "Gate faces a ledge after rotation " + piece.getRotation());
            var center = piece.getBoundingBox().getCenter();
            centerDiffers |= road[1]+1 != hillside.getBaseHeight(center.getX(), center.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, random);
        }
        test.assertTrue(seen.size() == 4 && centerDiffers, "Expected four rotations and a distinct uphill center");
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void shrineThemesLoadAndPlace(GameTestHelper test) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        var chunkSource = level.getChunkSource();
        var generator = chunkSource.getGenerator();
        var manager = level.getStructureManager();
        BlockPos origin = new BlockPos(1, 1, 0);
        BlockPos absolute = test.absolutePos(origin);

        var key = ResourceKey.create(Registries.STRUCTURE_SET, SakuraBloom.id("shrines"));
        var set = registries.registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(key).value();
        var themes = List.of(
            new Theme("cherry", "minecraft", "minecraft:cherry_grove", "pink",
                "stone_brick_stairs", "stone_stairs", "smooth_stone",
                p(5,4,12), "sakura_bloom:flower_mortar", p(9,11,24), p(15,7,21),
                List.of(p(15,2,4), p(15,2,16), p(15,3,17), p(15,3,24), p(9,3,24), p(9,3,25))),
            new Theme("shidare", SakuraBloom.ID, "sakura_bloom:shidare_meadow", "light_blue",
                "polished_diorite_stairs", "diorite_stairs", "calcite",
                p(5,2,12), "minecraft:water", p(15,9,19), p(15,7,22),
                List.of(p(15,2,4), p(15,2,6), p(15,3,7), p(15,3,25), p(19,3,25), p(19,3,26))),
            new Theme("yae", SakuraBloom.ID, "sakura_bloom:yae_grove", "red",
                "polished_andesite_stairs", "brick_stairs", "bricks",
                p(6,6,9), "sakura_bloom:silk_palace_lantern", p(16,16,24), p(16,8,21),
                List.of(p(16,2,4), p(16,2,15), p(16,4,17), p(16,4,22), p(23,4,22),
                    p(23,4,24), p(17,10,24), p(16,10,24), p(11,10,24), p(11,10,25))),
            new Theme("yamazakura", SakuraBloom.ID, "sakura_bloom:yamazakura_woodland", "green",
                "tuff_brick_stairs", "mossy_stone_brick_stairs", "mossy_stone_bricks",
                p(11,8,15), "minecraft:tuff_brick_stairs", p(11,15,23), p(11,12,21),
                List.of(p(15,2,4), p(15,2,8), p(15,6,12), p(15,6,15), p(13,6,15),
                    p(10,9,15), p(8,9,15), p(8,9,19), p(11,9,19), p(11,9,23), p(8,9,23), p(8,9,24)))
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
            if (theme.wood.equals("cherry")) for (int x : new int[]{12,18})
                assertBlock(test, origin.offset(x,3,17), "minecraft:chiseled_stone_bricks");
            for (int x = 10; x <= 20; x++) {
                String stairs = x >= 13 && x <= 17 ? theme.pathStairs : theme.stoneStairs;
                assertEntranceStep(test, origin.offset(x, 1, 0), stairs);
                assertEntranceStep(test, origin.offset(x, 2, 1), stairs);
            }
            assertBlock(test, origin.offset(15, 2, 2), "minecraft:" + theme.paving);
            assertBlock(test, origin.offset(11, 4, 4), theme.blockNamespace + ":" + theme.wood + "_log");
            verifyWalk(test, absolute, theme.path);
            assertBlock(test, origin.offset(theme.roofTile), "sakura_bloom:" + theme.roof + "_roof_tile_slab");
            assertBlock(test, origin.offset(theme.landmark), theme.landmarkBlock);
            verifyWoodwork(test, absolute, template, new StructurePlaceSettings());
            assertBlock(test, origin.offset(15, 8, 4), theme.blockNamespace + ":stripped_" + theme.wood + "_log");
            if (theme.wood.equals("shidare")) {
                for (int x = 14; x <= 16; x++)
                    verifyWalk(test, absolute, List.of(p(x,2,6), p(x,3,7), p(x,3,21)));
                verifyWalk(test, absolute, List.of(p(15,3,21), p(24,3,21), p(24,3,12)));
                verifyPond(test, absolute);
                for (int x : new int[]{22,27}) for (int z : new int[]{11,15})
                    for (int y = 1; y <= 7; y++) {
                        var foot = absolute.offset(x,y,z);
                        test.assertTrue(level.getBlockState(foot).isCollisionShapeFullBlock(level,foot), "Broken waterside pavilion post");
                    }
            }
            if (theme.wood.equals("yae")) {
                for (int x : new int[]{10,13}) {
                    test.assertBlockPresent(Blocks.NOTE_BLOCK, origin.offset(x,12,21));
                    test.assertBlockPresent(Blocks.AIR, origin.offset(x,13,21));
                }
                test.assertBlockPresent(chest, origin.offset(11,11,26));
                for (int x = 9; x <= 23; x++) for (int z = 21; z <= 26; z++) {
                    var trim = level.getBlockState(absolute.offset(x,11,z));
                    test.assertFalse(BuiltInRegistries.BLOCK.getKey(trim.getBlock()).getPath().endsWith("roof_tile_slab"),
                        "Roof skirt sticking through the music room floor");
                }
                for (int x = 8; x <= 24; x++) for (int z = 20; z <= 27; z++) {
                    if (x != 8 && x != 24 && z != 20 && z != 27) continue;
                    for (int y = 14; y < 21; y++) {
                        var pos = absolute.offset(x,y,z);
                        var state = level.getBlockState(pos);
                        test.assertTrue(state.isFaceSturdy(level,pos,Direction.DOWN), "Open seam above lantern gallery at " + pos);
                        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("roof_tile_slab")) break;
                        test.assertTrue(state.isCollisionShapeFullBlock(level,pos), "Broken wall plate at " + pos);
                    }
                }
            }
            verifyTurns(test, absolute, template, theme.doorway);
        }
        test.succeed();
    }

    private static void verifyPond(GameTestHelper test, BlockPos origin) {
        var level = test.getLevel();
        int shallow = 0, deep = 0, clayPatches = 0, gravelPatches = 0;
        for (int x = 2; x <= 27; x++) for (int z = 6; z <= 23; z++) {
            var water = origin.offset(x,2,z);
            if (!level.getBlockState(water).is(Blocks.WATER)) continue;
            var bed = water.below();
            if (level.getBlockState(bed).is(Blocks.WATER)) { deep++; bed = bed.below(); }
            else shallow++;
            var sediment = level.getBlockState(bed).getBlock();
            if (level.getBlockState(bed.east()).is(sediment) && level.getBlockState(bed.south()).is(sediment)
                    && level.getBlockState(bed.south().east()).is(sediment)) {
                if (sediment == Blocks.CLAY) clayPatches++;
                if (sediment == Blocks.GRAVEL) gravelPatches++;
            }
        }
        test.assertTrue(shallow > 25 && deep > 25 && clayPatches > 8 && gravelPatches > 8,
            "Pond needs shallow margins and contiguous sediment beds");
    }

    static void verifyTurns(GameTestHelper test, BlockPos origin, StructureTemplate template, BlockPos doorway) {
        var level = test.getLevel();
        for (var rotation : List.of(Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90)) {
            var turn = new StructurePlaceSettings().setRotation(rotation).setRotationPivot(p(15,0,15)).setKnownShape(true);
            template.placeInWorld(level, origin, origin, turn, level.random, 18);
            verifyWoodwork(test, origin, template, turn);
            if (doorway != null) {
                var pos = origin.offset(StructureTemplate.calculateRelativePosition(turn, doorway));
                var curtain = level.getBlockState(pos);
                test.assertTrue(curtain.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                    == rotation.rotate(Direction.NORTH) && curtain.canSurvive(level,pos), "Door curtain detached after " + rotation);
            }
        }
    }

    static void verifyWoodwork(GameTestHelper test, BlockPos origin, StructureTemplate template, StructurePlaceSettings settings) {
        var level = test.getLevel();
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof SignBlock)
                test.assertTrue(template.filterBlocks(origin, settings, block).isEmpty(), "Unexpected sign in natural structure");
            if (!(block instanceof FenceBlock)) continue;
            for (var fence : template.filterBlocks(origin, settings, block)) {
                var pos = fence.pos();
                var state = level.getBlockState(pos);
                test.assertTrue(state.is(block) && state == Block.updateFromNeighbourShapes(state, level, pos),
                    "Disconnected railing at " + pos.subtract(origin) + " " + state);
            }
        }
    }

    /** Sweep a player's standing volume along each leg, including every tread and turn. */
    static void verifyWalk(GameTestHelper test, BlockPos origin, List<BlockPos> waypoints) {
        var level = test.getLevel();
        var step = waypoints.getFirst();
        for (var end : waypoints) {
            while (!step.equals(end)) {
                step = step.offset(Integer.signum(end.getX()-step.getX()), Integer.signum(end.getY()-step.getY()),
                    Integer.signum(end.getZ()-step.getZ()));
                var pos = origin.offset(step);
                var floor = level.getBlockState(pos);
                test.assertTrue(!floor.getCollisionShape(level, pos).isEmpty(), "Missing floor on approach at " + step);
                // Allow the surface of a tatami mat on top of the deck.
                var standing = new AABB(.2, 1.065, .2, .8, 2.865, .8).move(pos);
                test.assertTrue(level.noCollision(standing), "Obstruction on walking route at " + step);
            }
        }
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
