package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class TravelGameTests {
    @GameTest(template = "clearing", skyAccess = true)
    public static void landmarkChestsGenerateAndOpen(GameTestHelper test) {
        var level = test.getLevel();
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var registries = level.registryAccess();
        var structs = registries.registryOrThrow(Registries.STRUCTURE);
        var set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(SakuraBloom.id("woodland_landmarks"));
        var origin = test.absolutePos(new BlockPos(3, 2, 3));
        record Site(String name, BloomContent.CherryWood wood) {}
        for (var site : List.of(new Site("flower_dyehouse", BloomContent.YAE), new Site("rain_pavilion", BloomContent.SHIDARE),
            new Site("hillside_joinery", BloomContent.YAMAZAKURA))) {
            var holder = structs.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, SakuraBloom.id(site.name)));
            test.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().equals(holder)), "Missing placement for " + site.name);
            var start = holder.value().generate(registries, generator, generator.getBiomeSource(), source.randomState(),
                level.getStructureManager(), level.getSeed(), new ChunkPos(origin), 0, level, biome -> true);
            test.assertTrue(start.isValid() && start.getPieces().size() == 1, "No start piece for " + site.name);
            var template = level.getStructureManager().get(SakuraBloom.id("landmark/" + site.name)).orElseThrow();
            template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setKnownShape(true), level.random, 18);
            checkChest(test, template, origin, site.wood.chest().get(), site.name);
        }
        test.succeed();
    }

    static void checkChest(GameTestHelper test, StructureTemplate template, BlockPos origin, Block block, String table) {
        var positions = template.filterBlocks(origin, new StructurePlaceSettings(), block);
        test.assertTrue(positions.size() == 1, "Expected one reward chest in " + table);
        var pos = positions.getFirst().pos();
        var level = test.getLevel();
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        var key = ResourceKey.create(Registries.LOOT_TABLE, SakuraBloom.id("chests/" + table));
        test.assertTrue(chest != null && key.equals(chest.getLootTable()), "Missing deferred loot for " + table);
        test.assertFalse(ChestBlock.isChestBlockedAt(level, pos), "Blocked chest lid in " + table);
        chest.unpackLootTable(null);
        test.assertTrue(chest.getLootTable() == null && !chest.isEmpty(), "Chest failed to roll its supplies: " + table);
    }

    @GameTest(template = "clearing")
    public static void rewardPoolsKeepSuppliesAndSometimesGiveOneCharm(GameTestHelper test) {
        var level = test.getLevel();
        var context = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        for (String name : List.of("flower_dyehouse", "rain_pavilion", "hillside_joinery", "shrine_offerings")) {
            var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, SakuraBloom.id("chests/" + name)));
            int wins = 0;
            for (int seed = 1; seed <= 160; seed++) {
                var loot = table.getRandomItems(context, seed);
                var charms = loot.stream().filter(s -> s.getItem() instanceof TravelCharmItem).mapToInt(ItemStack::getCount).sum();
                test.assertTrue(loot.size() >= 3 && charms <= 1, "Missing supplies or duplicate charm: " + name);
                wins += charms;
            }
            test.assertTrue(wins >= 15 && wins <= 90, "Expected both chest outcomes for " + name + ", got " + wins + "/160");
        }
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void travelCharmsWorkWhenEquipped(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        player.causeFallDamage(8, 1, player.damageSources().fall());
        test.assertTrue(player.getHealth() == 18, "Wind bell should take three blocks off a fall");
        player.setHealth(12);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.KNOT_CHARM.toStack());
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.POTION), 0, new ItemStack(Items.GLASS_BOTTLE)));
        test.assertTrue(player.getHealth() == 12, "A potion triggered the omamori");
        for (int i = 0; i < 2; i++)
            NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        test.assertTrue(player.getHealth() == 14 && player.getCooldowns().isOnCooldown(BloomContent.KNOT_CHARM.get()),
            "The omamori should heal once for consecutive meals");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var cherry = Blocks.CHERRY_LOG.defaultBlockState();
        var oak = Blocks.OAK_PLANKS.defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        var pos = test.absolutePos(new BlockPos(3, 1, 3));
        float cherryBase = player.getDigSpeed(cherry, pos), oakBase = player.getDigSpeed(oak, pos), stoneBase = player.getDigSpeed(stone, pos);
        player.getInventory().add(BloomContent.INK_BRACELET.toStack());
        test.assertTrue(player.getDigSpeed(cherry, pos) == cherryBase, "A bracelet in the backpack should be inactive");
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.INK_BRACELET.toStack());
        test.assertTrue(player.getDigSpeed(cherry, pos) == cherryBase * 1.35F && player.getDigSpeed(oak, pos) == oakBase * 1.2F
            && player.getDigSpeed(stone, pos) == stoneBase, "Inkcord applied the wrong harvesting bonus");
        if (ModList.get().isLoaded("curios")) CuriosCharmTest.check(test);
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void carpPendantOnlySlowsUnderwaterAirLoss(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, Blocks.WATER);
        test.setBlock(pos.above(), Blocks.WATER);
        player.setPos(Vec3.atBottomCenterOf(test.absolutePos(pos)));
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.FISH_PENDANT.toStack());
        player.setAirSupply(100);
        for (int tick = 1; tick <= 10; tick++) {
            player.tickCount = tick;
            player.baseTick();
        }
        test.assertTrue(player.getAirSupply() == 95, "Ten submerged ticks should use five air");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.baseTick();
        test.assertTrue(player.getAirSupply() == 94, "Removing the pendant did not restore normal breathing");
        test.succeed();
    }
}
