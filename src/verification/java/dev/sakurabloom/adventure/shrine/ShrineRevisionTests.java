package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import dev.sakurabloom.adventure.combat.SpiritCombat;
import dev.sakurabloom.adventure.realm.Interstice;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class ShrineRevisionTests {
    @GameTest(template="clearing")
    public static void centeredBoxesAndSpawnEggs(GameTestHelper test) {
        var level = test.getLevel();
        var centers = Map.of("cherry",new BlockPos(15,5,26),"shidare",new BlockPos(15,5,27),
            "yae",new BlockPos(16,6,27),"yamazakura",new BlockPos(11,11,25));
        for (var site:centers.entrySet()) {
            var template = level.getStructureManager().get(SakuraBloom.id("shrine/"+site.getKey())).orElseThrow();
            var boxes = template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),ShrineAdventure.BOX.get());
            test.assertTrue(boxes.size()==1 && boxes.getFirst().pos().equals(site.getValue()),"Wrong central offering box: "+site.getKey());
        }
        for (var type : List.of(ShrineAdventure.FOX.get(), ShrineAdventure.WOLF.get(), ShrineAdventure.HARE.get(),
            ShrineAdventure.MAIDEN.get(), Interstice.WISP.get(), SpiritCombat.SENTINEL.get())) {
            var egg = SpawnEggItem.byId(type);
            test.assertTrue(egg!=null && egg.getType(egg.getDefaultInstance())==type, "Mismatched spawn egg: "+type);
        }
        var prizes = PrayerRewards.from(level.getServer()).prizes();
        test.assertTrue(prizes.size()==10 && prizes.stream().mapToInt(PrayerRewards.Prize::weight).sum()==66,"Wrong synced prize odds: "+prizes);
        test.assertTrue(prizes.stream().anyMatch(p -> p.item()==Items.AMETHYST_SHARD && p.count()==3 && p.weight()==14),
            "Count or weight for amethyst changed");
        test.succeed();
    }

    @GameTest(template="clearing",skyAccess=true)
    public static void nightPatrolsSpawnForAllSpecies(GameTestHelper test) {
        var level = test.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var species = List.of(ShrineAdventure.FOX.get(),ShrineAdventure.WOLF.get(),ShrineAdventure.HARE.get());
        for (var id : List.of("minecraft:cherry_grove","sakura_bloom:shidare_meadow","sakura_bloom:yae_grove","sakura_bloom:yamazakura_woodland")) {
            var biome = biomes.get(net.minecraft.resources.ResourceLocation.parse(id));
            var entries = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            for (var type : species) test.assertTrue(entries.stream().anyMatch(s->s.type==type && s.getWeight().asInt()==55),
                "Missing equal regional spawn: "+id+" "+type);
        }
        // Build a small surface patch in a separate chunk so /fillbiome doesn't affect other tests.
        var chunk = level.getChunkAt(test.absolutePos(new BlockPos(0,0,0)).offset(288,0,288));
        var grove = biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE);
        var pos = chunk.getPos().getMiddleBlockPosition(101);
        var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        var sampler = level.getChunkSource().randomState().sampler();
        var previousBiomes = section.getBiomes();
        var originalGround = level.getBlockState(pos.below());
        var originalBranch = level.getBlockState(pos.above(4));
        var spawned = new java.util.ArrayList<Entity>();
        section.fillBiomesFromNoise((x,y,z,noise)->grove,sampler,0,0,0);
        level.setBlockAndUpdate(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(pos.above(4),Blocks.CHERRY_LOG.defaultBlockState());
        long previous = level.getDayTime();
        try {
            level.setDayTime(18000);
            for (var type : species) {
                test.assertTrue(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,RandomSource.create(7)),
                    "Rejected nighttime cherry ground under a branch: "+type);
                var patrol = type.spawn(level,pos,MobSpawnType.NATURAL);
                test.assertTrue(patrol!=null,"Could not spawn "+type);
                spawned.add(patrol);
            }
            level.setDayTime(6000);
            for (var type:species) test.assertFalse(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,RandomSource.create(7)),
                "Daytime patrol: "+type);
        } finally {
            spawned.forEach(Entity::discard);
            level.setBlockAndUpdate(pos.below(),originalGround);
            level.setBlockAndUpdate(pos.above(4),originalBranch);
            section.fillBiomesFromNoise((x,y,z,noise)->previousBiomes.get(x,y,z),sampler,0,0,0);
            level.setDayTime(previous);
        }
        test.succeed();
    }
}
