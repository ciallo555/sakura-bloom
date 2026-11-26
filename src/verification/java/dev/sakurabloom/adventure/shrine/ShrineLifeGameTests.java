package dev.sakurabloom.adventure.shrine;

import com.mojang.authlib.GameProfile;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public class ShrineLifeGameTests {
    @GameTest(template="clearing", skyAccess=true)
    public static void shrineWardsExtendBeyondTheCourtyard(GameTestHelper test) {
        var level = test.getLevel();
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var registries = level.registryAccess();
        var shrine = registries.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .get(SakuraBloom.id("cherry_shrine"));
        var origin = new net.minecraft.world.level.ChunkPos(test.absolutePos(new BlockPos(3,2,3)));
        var start = shrine.generate(registries,generator,generator.getBiomeSource(),source.randomState(),
            level.getStructureManager(),level.getSeed(),origin,0,level,biome -> true);
        test.assertTrue(start.isValid(),"Expected a valid shrine start");
        var box = start.getBoundingBox();
        var boundary = new BlockPos(box.maxX(), box.minY(), box.getCenter().getZ());
        var startChunk = level.getChunk(origin.x,origin.z);
        var edgeChunk = level.getChunkAt(boundary);
        var previousStarts = new java.util.HashMap<>(startChunk.getAllStarts());
        var oldRefs = new java.util.HashMap<>(edgeChunk.getAllReferences());
        try {
            startChunk.setStartForStructure(shrine,start);
            var references = new java.util.HashMap<>(oldRefs);
            var set = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(edgeChunk.getReferencesForStructure(shrine));
            set.add(origin.toLong());
            references.put(shrine,set);
            edgeChunk.setAllReferences(references);
            test.assertTrue(Yokai.nearShrine(level,boundary.east(64)),"Protection reaches 64 blocks past the shrine wall");
            test.assertFalse(Yokai.nearShrine(level,boundary.east(65)),"Protection ends beyond the last warded block");
        } finally {
            edgeChunk.setAllReferences(oldRefs);
            startChunk.setAllStarts(previousStarts);
        }
        test.succeed();
    }

    @GameTest(template="clearing")
    public static void offeringNeedsOneScripAndASecondInteraction(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var box = ShrineAdventure.BOX.get();
        var local = new BlockPos(2,2,2);
        test.setBlock(local,box);
        var pos = test.absolutePos(local);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var tokens = new ItemStack(ShrineAdventure.TALISMAN.get(),2);
        var state = box.defaultBlockState();
        box.useWithoutItem(state,test.getLevel(),pos,player,hit);
        test.assertTrue(player.getInventory().isEmpty(),"An unpaid prayer must not yield a gift");
        box.useItemOn(tokens,state,test.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
        box.useItemOn(tokens,state,test.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
        test.assertTrue(tokens.getCount()==1 && player.getInventory().isEmpty(),"Only one outstanding payment per pilgrim");
        box.useWithoutItem(state,test.getLevel(),pos,player,hit);
        int count = player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
        test.assertTrue(count>0,"Paid prayer must draw a registered gift");
        box.useWithoutItem(state,test.getLevel(),pos,player,hit);
        test.assertTrue(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum()==count,"Prayer cannot be redeemed twice");
        test.succeed();
    }

    @GameTest(template="clearing")
    public static void commissionCycleRewardsTheFan(GameTestHelper test) {
        var player = FakePlayerFactory.get(test.getLevel(),new GameProfile(UUID.randomUUID(),"Pilgrim"));
        var maiden = ShrineAdventure.MAIDEN.get().create(test.getLevel());
        var fan = BuiltInRegistries.ITEM.get(SakuraBloom.id("kamikakushi"));
        var pouch = BuiltInRegistries.ITEM.get(SakuraBloom.id("knotwork_pouch"));
        test.assertTrue(fan!=Items.AIR && pouch!=Items.AIR,"Commission rewards must be registered");
        test.assertTrue(maiden.getOffers().stream().anyMatch(o->o.getResult().is(pouch)),"Maiden sells the starting pouch");
        test.assertTrue(maiden.getOffers().stream().anyMatch(o->o.getResult().is(ShrineAdventure.TALISMAN.get())),"Maiden buys common offerings");
        for (int i=0;i<4;i++) {
            Commissions.talk(player);
            Commissions.talk(player);
            Commissions.talk(player);
            test.assertTrue(Commissions.journal(player).getInt("Completed")==i,"Incomplete commission should stay active");
            switch(i) {
                case 0 -> player.getInventory().add(new ItemStack(Items.PAPER,12));
                case 1 -> {
                    for (int kill=0;kill<3;kill++) {
                        var beast=test.spawn(ShrineAdventure.FOX.get(),3,2,3);
                        beast.hurt(player.damageSources().playerAttack(player),1000);
                    }
                }
                case 2 -> player.getInventory().add(new ItemStack(Items.INK_SAC,4));
                case 3 -> player.getInventory().add(new ItemStack(Items.AMETHYST_SHARD,3));
            }
            Commissions.talk(player);
            test.assertTrue(Commissions.journal(player).getInt("Completed")==i+1,"Expected completed commission "+i);
        }
        test.assertTrue(player.getInventory().countItem(fan)==1,"One fan per four completed commissions");
        test.assertTrue(player.getInventory().countItem(ShrineAdventure.TALISMAN.get())==16,"Each commission pays four scrip");
        test.succeed();
    }
}
