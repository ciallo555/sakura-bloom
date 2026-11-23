package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static dev.sakurabloom.ShrineGameTests.p;

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
        record Site(String name, BloomContent.CherryWood wood, BlockPos furnishing, Block block, List<BlockPos> walk) {}
        for (var site : List.of(
            new Site("flower_dyehouse", BloomContent.YAE, p(6,4,16), Blocks.LOOM,
                List.of(p(10,2,3), p(10,2,5), p(10,3,6), p(10,3,14), p(8,3,14), p(8,3,15))),
            new Site("rain_pavilion", BloomContent.SHIDARE, p(21,2,10), Blocks.WATER,
                List.of(p(11,2,2), p(11,2,4), p(16,2,4), p(16,2,9), p(19,2,9), p(19,2,12),
                    p(19,3,13), p(19,3,18), p(16,3,18), p(16,3,19))),
            new Site("hillside_joinery", BloomContent.YAMAZAKURA, p(7,5,13), Blocks.STONECUTTER,
                List.of(p(12,2,3), p(12,2,8), p(12,3,9), p(12,3,16), p(9,3,16), p(9,3,18))))) {
            var holder = structs.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, SakuraBloom.id(site.name)));
            test.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().equals(holder)), "Missing placement for " + site.name);
            var start = holder.value().generate(registries, generator, generator.getBiomeSource(), source.randomState(),
                level.getStructureManager(), level.getSeed(), new ChunkPos(origin), 0, level, biome -> true);
            test.assertTrue(start.isValid() && start.getPieces().size() == 1, "No start piece for " + site.name);
            var template = level.getStructureManager().get(SakuraBloom.id("landmark/" + site.name)).orElseThrow();
            template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setKnownShape(true), level.random, 18);
            checkChest(test, template, origin, site.wood.chest().get(), site.name);
            test.assertTrue(level.getBlockState(origin.offset(site.furnishing)).is(site.block), "Missing furnishing in " + site.name);
            ShrineGameTests.verifyWalk(test, origin, site.walk);
            if (site.name.equals("flower_dyehouse")) {
                ShrineGameTests.verifyWalk(test, origin, List.of(p(16,2,4), p(16,2,22)));
                for (int x : new int[]{14,18}) for (int z : new int[]{8,17})
                    test.assertTrue(level.getBlockState(origin.offset(x,3,z)).is(Blocks.STONE_BRICKS), "Missing aisle post footing");
            }
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
        var facing = level.getBlockState(pos).getValue(ChestBlock.FACING);
        var wall = pos.relative(facing.getOpposite());
        test.assertTrue(level.getBlockState(wall).isFaceSturdy(level, wall, facing), "Chest has no wall behind it in " + table);
        var front = pos.relative(facing);
        var shape = level.getBlockState(front).getCollisionShape(level, front);
        test.assertTrue(shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.125, "Chest aisle is obstructed in " + table);
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
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        TravelCharmItem.refreshBonuses(player);
        player.causeFallDamage(8, 1, player.damageSources().fall());
        test.assertTrue(player.getHealth() == 20 && Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-1.15*speed)<1e-6,
            "The bell should absorb an eight-block fall and improve movement");
        player.setHealth(12);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.KNOT_CHARM.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getMaxHealth() == 24 && player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Swapping charms left stale bonuses");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.POTION), 0, new ItemStack(Items.GLASS_BOTTLE)));
        test.assertTrue(player.getHealth() == 12, "A potion triggered the omamori");
        for (int i = 0; i < 2; i++)
            NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        test.assertTrue(player.getHealth() == 16 && player.getCooldowns().isOnCooldown(BloomContent.KNOT_CHARM.get()),
            "The omamori should heal once for consecutive meals");

        player.setHealth(24);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var mining = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        var other = new AttributeModifier(SakuraBloom.id("test_other_tool"), 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mining.addTransientModifier(other);
        player.getInventory().add(BloomContent.INK_BRACELET.toStack());
        TravelCharmItem.refreshBonuses(player);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        test.assertTrue(player.getHealth() == 20 && Math.abs(mining.getValue()-1.4)<1e-6, "Backpack or expired health bonus leaked");
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.INK_BRACELET.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(Math.abs(mining.getValue()-1.65)<1e-6 && player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)==reach+1,
            "Bracelet must combine with other mods' modifiers");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(mining.hasModifier(other.id()) && Math.abs(mining.getValue()-1.4)<1e-6, "Unequipping disturbed another modifier");
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
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(Math.abs(player.getAttributeValue(NeoForgeMod.SWIM_SPEED)-1.35)<1e-6
            && Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED)-0.6)<1e-6, "Missing swimming or underwater mining bonus");
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
