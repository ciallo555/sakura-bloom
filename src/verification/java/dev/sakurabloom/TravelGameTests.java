package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
            ShrineGameTests.verifyWoodwork(test, origin, template, new StructurePlaceSettings());
            if (site.name.equals("flower_dyehouse")) {
                ShrineGameTests.verifyWalk(test, origin, List.of(p(16,2,4), p(16,2,22)));
                for (int x : new int[]{14,18}) for (int z : new int[]{8,17})
                    test.assertTrue(level.getBlockState(origin.offset(x,3,z)).is(Blocks.STONE_BRICKS), "Missing aisle post footing");
                ShrineGameTests.verifyWalk(test, origin, List.of(p(8,3,14), p(6,3,14)));
                ShrineGameTests.verifyLadder(test, origin, p(5,4,14), 8, Direction.EAST);
                ShrineGameTests.verifyWalk(test, origin, List.of(p(5,7,14), p(14,7,14)));
                test.assertTrue(level.getBlockState(origin.offset(13,9,16)).is(Blocks.BLUE_WOOL), "No cloth on the loft rack");
            } else if (site.name.equals("hillside_joinery")) {
                ShrineGameTests.verifyWalk(test, origin, List.of(p(9,3,18), p(10,3,18)));
                ShrineGameTests.verifyLadder(test, origin, p(10,4,19), 7, Direction.NORTH);
                ShrineGameTests.verifyWalk(test, origin, List.of(p(10,7,19), p(10,7,18), p(13,7,18)));
                test.assertTrue(level.getBlockState(origin.offset(13,8,19)).is(site.wood.chest().get()), "Missing loft chest");
            }
            ShrineGameTests.verifyTurns(test, origin, template, null);
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
    public static void bellPreventsFallDamageOnlyWhenEquipped(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        TravelCharmItem.refreshBonuses(player);
        player.causeFallDamage(200, 1, player.damageSources().fall());
        player.hurt(player.damageSources().fall(), 80);
        test.assertTrue(player.getHealth() == 20 && Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-1.15*speed)<1e-6,
            "Even a lethal fall should be harmless while wearing the bell");
        test.assertTrue(player.hurt(player.damageSources().generic(), 2) && player.getHealth() < 20, "The bell blocked ordinary damage");
        player.invulnerableTime = 0;
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().add(BloomContent.WIND_BELL.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed && player.hurt(player.damageSources().fall(), 5),
            "A bell in the backpack should have no effect");
        if (ModList.get().isLoaded("curios")) CuriosCharmTest.check(test);
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void omamoriGrantsFiveSecondsAfterEveryMeal(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.KNOT_CHARM.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getMaxHealth() == 24, "Expected four extra health points");
        player.setHealth(24);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.POTION), 0, new ItemStack(Items.GLASS_BOTTLE)));
        test.assertFalse(player.hasEffect(MobEffects.REGENERATION), "Drinking a potion counted as eating food");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        var regen = player.getEffect(MobEffects.REGENERATION);
        test.assertTrue(regen != null && regen.getAmplifier() == 0 && regen.getDuration() == 100, "No five-second Regeneration I at full health");
        player.setHealth(12);
        for (int tick = 0; tick < 60; tick++) player.baseTick();
        float healed = player.getHealth();
        test.assertTrue(healed > 12 && healed < 16, "Regeneration should heal gradually");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(Items.CARROT), 0, ItemStack.EMPTY));
        test.assertTrue(player.getHealth() == healed && player.getEffect(MobEffects.REGENERATION).getDuration() == 100,
            "A second meal should refresh Regeneration with no cooldown or instant heal");
        player.setHealth(24);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getHealth() == 20 && player.getMaxHealth() == 20, "Extra health survived unequipping the omamori");
        for (int tick = 0; tick < 100; tick++) player.baseTick();
        test.assertFalse(player.hasEffect(MobEffects.REGENERATION), "Regeneration outlasted five seconds");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void carpPendantWorksOnlyInWater(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var wet = p(3,2,3);
        test.setBlock(wet, Blocks.WATER);
        test.setBlock(wet.above(), Blocks.WATER);
        player.setPos(Vec3.atBottomCenterOf(test.absolutePos(p(7,2,3))));
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.FISH_PENDANT.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getAttributeValue(NeoForgeMod.SWIM_SPEED) == 1 && player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) == 1
            && !player.hasEffect(MobEffects.WATER_BREATHING), "The dry pendant gave underwater bonuses");
        player.setPos(Vec3.atBottomCenterOf(test.absolutePos(wet)));
        player.baseTick();
        player.baseTick();
        var stone = Blocks.STONE.defaultBlockState();
        float baseMining = player.getDestroySpeed(stone);
        TravelCharmItem.refreshBonuses(player);
        var breath = player.getEffect(MobEffects.WATER_BREATHING);
        test.assertTrue(breath != null && breath.getAmplifier() == 0 && Math.abs(player.getAttributeValue(NeoForgeMod.SWIM_SPEED)-1.3)<1e-6
            && Math.abs(player.getDestroySpeed(stone)-1.3*baseMining)<1e-6, "The pendant must give Water Breathing I and 30% aquatic movement and mining");
        player.setAirSupply(100);
        for (int tick = 0; tick < 10; tick++) player.baseTick();
        test.assertTrue(player.getAirSupply() >= 100, "A breathing wearer lost air underwater");
        player.setPos(Vec3.atBottomCenterOf(test.absolutePos(p(7,2,3))));
        player.baseTick();
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING) && player.getAttributeValue(NeoForgeMod.SWIM_SPEED)==1
            && player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)==1, "Pendant bonuses followed the wearer onto dry land");

        player.setPos(Vec3.atBottomCenterOf(test.absolutePos(wet)));
        player.baseTick();
        TravelCharmItem.refreshBonuses(player);
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getEffect(MobEffects.WATER_BREATHING).getDuration() == 600, "Unequipping consumed the breathing potion");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void inkcordGrantsHasteAndPreservesOtherSources(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var stone = Blocks.STONE.defaultBlockState();
        var mining = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        var other = new AttributeModifier(SakuraBloom.id("test_other_tool"), 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mining.addTransientModifier(other);
        float baseline = player.getDestroySpeed(stone);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.INK_BRACELET.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 1 && Math.abs(player.getDestroySpeed(stone)-baseline*1.4)<1e-5
            && player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)==reach, "Bracelet should give vanilla Haste II only");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(!player.hasEffect(MobEffects.DIG_SPEED) && mining.hasModifier(other.id()), "Unequipping the bracelet disturbed another modifier");
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 0));
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.INK_BRACELET.toStack());
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getEffect(MobEffects.DIG_SPEED).getAmplifier()==1, "A weaker potion hid Haste II");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        for (int tick = 0; tick < 2; tick++) player.baseTick();
        var original = player.getEffect(MobEffects.DIG_SPEED);
        test.assertTrue(original != null && original.getAmplifier()==0 && original.getDuration()==298, "Lost the underlying Haste I potion");
        player.removeEffect(MobEffects.DIG_SPEED);
        player.setItemInHand(InteractionHand.OFF_HAND, BloomContent.INK_BRACELET.toStack());
        TravelCharmItem.refreshBonuses(player);
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 2));
        TravelCharmItem.refreshBonuses(player);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getEffect(MobEffects.DIG_SPEED).getAmplifier()==2 && player.getEffect(MobEffects.DIG_SPEED).getDuration()==240,
            "The bracelet replaced another source of Haste III");
        test.succeed();
    }
}
