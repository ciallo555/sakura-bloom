package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class FarmingGameTests {
    private static final BlockPos BED = new BlockPos(4, 3, 4);

    @GameTest(template = "clearing", skyAccess = true)
    public static void farmersPickUpPlantAndHarvestNewVegetables(GameTestHelper test) {
        var level = test.getLevel();
        int index = 0;
        for (var crop : List.of(KitchenContent.CABBAGE, KitchenContent.TOMATO)) {
            BlockPos plant = BED.east(index++ * 4);
            BlockPos absolute = test.absolutePos(plant);
            test.setBlock(plant.below(), Blocks.FARMLAND);
            var villager = EntityType.VILLAGER.create(level);
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
            villager.setPos(Vec3.atBottomCenterOf(absolute));
            villager.getBrain().setMemory(MemoryModuleType.SECONDARY_JOB_SITE, List.of(GlobalPos.of(level.dimension(), absolute.below())));
            var seeds = crop.seeds().toStack();
            test.assertTrue(villager.wantsToPickUp(seeds), "Farmer did not recognize the seed drop");
            villager.getInventory().setItem(0, seeds);
            var farmWork = new HarvestFarmland();
            long start = level.getGameTime();
            test.assertTrue(farmWork.tryStart(level, villager, start), "Farmer did not choose the farm bed");
            farmWork.tickOrStop(level, villager, start + 1);
            var block = crop.block().get();
            test.assertBlockPresent(block, plant);
            test.assertTrue(villager.getInventory().getItem(0).isEmpty(), "Planting did not spend the seed");
            test.setBlock(plant, block.getStateForAge(block.getMaxAge()));
            farmWork.tickOrStop(level, villager, start + 2);
            test.assertBlockPresent(Blocks.AIR, plant);
        }
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void paddyRespectsSpaceAndHarvestsEitherHalfOnce(GameTestHelper test) {
        var level = test.getLevel();
        var rice = KitchenContent.RICE.block().get();
        for (boolean harvestTop : List.of(false, true)) {
            BlockPos root = harvestTop ? BED.east(6) : BED;
            BlockPos absolute = test.absolutePos(root);
            test.setBlock(root.below(), Blocks.FARMLAND);
            test.setBlock(root, rice.getStateForAge(6));
            test.setBlock(root.above(), Blocks.GLASS);
            test.assertFalse(rice.isValidBonemealTarget(level, absolute, test.getBlockState(root)),
                "Mature rice should require room for the panicle");
            test.setBlock(root.above(), Blocks.AIR);
            rice.performBonemeal(level, level.random, absolute, test.getBlockState(root));
            test.assertTrue(rice.isMaxAge(test.getBlockState(root)), "Rice did not mature");
            test.assertBlockPresent(KitchenContent.RICE_PANICLE.get(), root.above());
            test.assertTrue(level.getFluidState(absolute).isSourceOfType(Fluids.WATER), "Growth lost the water source");
            BlockPos cut = harvestTop ? root.above() : root;
            level.destroyBlock(test.absolutePos(cut), true);
            test.assertBlockPresent(Blocks.WATER, root);
            test.assertBlockPresent(Blocks.AIR, root.above());
            test.assertItemEntityCountIs(KitchenContent.RICE_ITEM.get(), cut, 1.5, 1);
        }
        BlockPos legacy = BED.south(6);
        test.setBlock(legacy.below(), Blocks.FARMLAND);
        var dry = rice.defaultBlockState().setValue(PaddyRiceBlock.AGE, rice.getMaxAge());
        test.setBlock(legacy, dry);
        BlockPos legacyPos = test.absolutePos(legacy);
        test.assertFalse(rice.isValidBonemealTarget(level, legacyPos, dry), "A legacy dry crop should wait for flooding");
        test.assertTrue(rice.placeLiquid(level, legacyPos, dry, Fluids.WATER.getSource(false)), "Legacy rice could not be flooded");
        test.assertBlockPresent(KitchenContent.RICE_PANICLE.get(), legacy.above());
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void wildCropsHaveTheRightHabitatAndDrops(GameTestHelper test) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        var features = registries.registryOrThrow(Registries.CONFIGURED_FEATURE);
        var placements = registries.registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = registries.registryOrThrow(Registries.BIOME);
        var cabbage = features.get(SakuraBloom.id("wild_cabbage"));
        var rice = features.get(SakuraBloom.id("wild_rice"));
        var generator = level.getChunkSource().getGenerator();
        test.assertTrue(biomes.getHolderOrThrow(Biomes.BEACH).value().getGenerationSettings()
            .hasFeature(placements.get(SakuraBloom.id("wild_cabbage"))), "Wild cabbage is missing from beaches");
        test.assertTrue(biomes.getHolderOrThrow(Biomes.RIVER).value().getGenerationSettings()
            .hasFeature(placements.get(SakuraBloom.id("wild_rice"))), "Wild rice is missing from rivers");

        BlockPos dry = BED;
        BlockPos wet = BED.east(5);
        test.setBlock(dry.below(), Blocks.SAND);
        test.assertTrue(cabbage.place(level, generator, level.random, test.absolutePos(dry)), "Wild cabbage did not place on sand");
        test.assertBlockPresent(KitchenContent.WILD_CABBAGE.get(), dry);
        level.destroyBlock(test.absolutePos(dry), true);
        test.assertItemEntityPresent(KitchenContent.CABBAGE.seeds().get(), dry, 1.25);

        test.setBlock(wet.below(), Blocks.DIRT);
        var wetPos = test.absolutePos(wet);
        test.assertFalse(rice.place(level, generator, level.random, wetPos), "Wild rice placed without a source");
        test.setBlock(wet, Blocks.WATER);
        test.setBlock(wet.above(), Blocks.WATER);
        test.assertFalse(rice.place(level, generator, level.random, wetPos), "Wild rice placed under two blocks of water");
        test.setBlock(wet.above(), Blocks.AIR);
        test.assertTrue(rice.place(level, generator, level.random, wetPos), "Wild rice did not place on shallow submerged dirt");
        test.assertTrue(test.getBlockState(wet).getValue(BlockStateProperties.WATERLOGGED), "Wild rice lost its source");
        test.assertTrue(test.getBlockState(wet.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
            "Wild rice did not place its top");
        level.destroyBlock(wetPos.above(), true);
        test.assertBlockPresent(Blocks.WATER, wet);
        test.assertItemEntityCountIs(KitchenContent.RICE.seeds().get(), wet.above(), 1.25, 1);
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void villageFarmsSampleVegetablesAndVanillaCrops(GameTestHelper test) {
        var level = test.getLevel();
        var processors = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        var settings = new StructurePlaceSettings();
        var wheat = Blocks.WHEAT.defaultBlockState();
        var templateCrop = new StructureBlockInfo(BlockPos.ZERO, wheat, null);
        for (String list : List.of("farm_plains", "farm_savanna", "farm_taiga", "farm_desert", "farm_snowy", "mossify_10_percent")) {
            var rules = processors.get(ResourceLocation.withDefaultNamespace(list));
            var seen = new HashSet<Block>();
            for (int i = 0; i < 512; i++) {
                var pos = test.absolutePos(BED).offset(i % 32, 0, i / 32);
                var placedCrop = new StructureBlockInfo(pos, wheat, null);
                for (var processor : rules.list()) {
                    placedCrop = processor.process(level, BlockPos.ZERO, BlockPos.ZERO, templateCrop, placedCrop, settings, null);
                }
                seen.add(placedCrop.state().getBlock());
            }
            test.assertTrue(seen.containsAll(List.of(Blocks.WHEAT, KitchenContent.CABBAGE.block().get(), KitchenContent.TOMATO.block().get())),
                "Village processor did not mix its crops: " + list);
        }
        test.succeed();
    }
}
