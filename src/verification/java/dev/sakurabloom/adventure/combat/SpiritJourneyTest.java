package dev.sakurabloom.adventure.combat;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.Arrays;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class SpiritJourneyTest {
    @GameTest(template = "clearing")
    public static void sanctumRewardsAndRecipes(GameTestHelper test) {
        var level = test.getLevel();
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
        var loot = level.getServer().reloadableRegistries();
        var largeBag = BuiltInRegistries.ITEM.get(SakuraBloom.id("cloud_brocade_pouch"));
        for (var theme : Sanctum.values()) {
            var chest = loot.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, SakuraBloom.id("chests/"+theme.id+"_sanctum")));
            var bossLoot = loot.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, SakuraBloom.id("entities/"+theme.id+"_guardian")));
            var guaranteed = bossLoot.getRandomItems(params, 21);
            test.assertTrue(guaranteed.stream().anyMatch(s -> s.is(theme.core.get())), "Boss core missing: "+theme);
            var blade = BuiltInRegistries.ITEM.get(SakuraBloom.id(theme.id+"_blade"));
            test.assertTrue(guaranteed.stream().anyMatch(s -> s.is(blade)), "Boss weapon missing: "+theme);
            boolean bag = false, relic = false;
            for (int seed = 1; seed < 65; seed++) {
                var result = chest.getRandomItems(params, seed);
                test.assertTrue(result.stream().anyMatch(s -> s.is(theme.shard.get()) && s.getCount() >= 4), "Missing ritual material");
                bag |= result.stream().anyMatch(s -> s.is(largeBag));
                relic |= result.stream().anyMatch(s -> s.getItem().getClass().getSimpleName().equals("TravelCharmItem"));
            }
            test.assertTrue(bag && relic, "Unreachable pouch or relic reward: "+theme);
            test.assertTrue(level.getRecipeManager().byKey(theme.seal.getId()).isPresent(), "Missing seal recipe");
        }
        test.succeed();
    }

    @GameTest(template = "clearing", timeoutTicks = 300)
    public static void summonAtSanctumAltar(GameTestHelper test) {
        var server = test.getLevel().getServer();
        var level = server.getLevel(SpiritCombat.REALM);
        test.assertTrue(level != null, "No Interstice dimension");
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structures = HolderSet.direct(Arrays.stream(Sanctum.values()).map(t -> registry.getHolderOrThrow(
            ResourceKey.create(Registries.STRUCTURE,SakuraBloom.id(t.id+"_sanctum")))).toList());
        // The vanilla GameTest server disables structure starts globally. Build one through the
        // registered generator and install the same chunk references that worldgen supplies.
        var theme = Sanctum.LANTERN;
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var chunkPos = new net.minecraft.world.level.ChunkPos(72, 8);
        var structure = structures.get(0).value();
        var start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), source.randomState(),
            level.getStructureManager(), level.getSeed(), chunkPos, 0, level, b -> true);
        test.assertTrue(start.isValid(), "No sanctum start");
        level.getChunk(chunkPos.x,chunkPos.z).setStartForStructure(structure, start);
        var bounds = start.getBoundingBox();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                var chunk = level.getChunk(cx,cz);
                chunk.addReferenceForStructure(structure,chunkPos.toLong());
                start.placeInChunk(level,level.structureManager(),generator,level.random,
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(cx*16,0,cz*16,cx*16+15,127,cz*16+15), chunk.getPos());
            }
        }
        var pos = new BlockPos(chunkPos.getMinBlockX()+16,76,chunkPos.getMinBlockZ()+16);
        test.assertTrue(level.getBlockState(pos).is(theme.altar.get()), "Missing natural altar at "+pos);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(theme.seal.get(),2));
        var ctx = new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        var difficulty = level.getDifficulty();
        try {
            server.setDifficulty(Difficulty.NORMAL, true);
            for (var dir : Direction.Plane.HORIZONTAL) {
                var candle = pos.relative(dir,2);
                test.assertTrue(level.getBlockState(candle).is(Blocks.RED_CANDLE),"Natural ritual candle is missing");
                level.setBlockAndUpdate(candle,Blocks.RED_CANDLE.defaultBlockState());
            }
            test.assertFalse(theme.seal.get().useOn(ctx).consumesAction(), "Unlit altar summoned boss");
            test.assertTrue(ctx.getItemInHand().getCount()==2,"Refused ritual consumed seal");
            for (var dir : Direction.Plane.HORIZONTAL)
                level.setBlockAndUpdate(pos.relative(dir,2), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.LIT,true));
            test.assertTrue(theme.seal.get().useOn(ctx).consumesAction(),"Four lit candles failed to summon at "+pos);
            var guardians = level.getEntitiesOfClass(VeilGuardian.class, new AABB(pos).inflate(6),e->e.theme==theme);
            test.assertTrue(guardians.size()==1 && ctx.getItemInHand().getCount()==1, "Summon must cost one seal for one boss");
            test.assertFalse(theme.seal.get().useOn(ctx).consumesAction(),"Duplicate guardian");
            guardians.forEach(VeilGuardian::discard);
        } finally {
            server.setDifficulty(difficulty,true);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        test.succeed();
    }
}
