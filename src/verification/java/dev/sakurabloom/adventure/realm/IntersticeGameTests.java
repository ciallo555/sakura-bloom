package dev.sakurabloom.adventure.realm;

import com.mojang.authlib.GameProfile;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class IntersticeGameTests {
    @GameTest(template = "clearing", timeoutTicks = 400)
    public static void midnightDistrictsHaveBrokenCourtsAndDistinctResidents(GameTestHelper test) {
        var realm = test.getLevel().getServer().getLevel(Interstice.LEVEL);
        test.assertTrue(realm != null && realm.dimensionType().fixedTime().orElse(-1) == 18000, "Interstice is fixed at midnight");
        String[] districts = {"lantern_gallery", "moonlit_harbor", "vermilion_bridges", "ashen_lofts"};
        // The last sample is across the negative-coordinate origin.
        int[] centers = {47, 175, 303, -81};
        for (int i = 0; i < centers.length; i++) {
            int x = centers[i];
            var pos = new BlockPos(x, 57, 47);
            var biome = realm.getBiome(pos);
            test.assertTrue(biome.unwrapKey().orElseThrow().location().equals(SakuraBloom.id(districts[i])), "Wrong district at " + pos);
            var mobs = biome.value().getMobSettings();
            test.assertTrue(mobs.getMobs(MobCategory.MONSTER).isEmpty() == (i == 0), "Unexpected hostile spawn list in "+districts[i]);
            test.assertTrue(mobs.getMobs(MobCategory.CREATURE).isEmpty() == (i == 3), "Unexpected friendly spawn list in "+districts[i]);
        }
        var heights = new java.util.HashSet<Integer>();
        int intact = 0, broken = 0;
        for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) {
            var ward = FloatingWard.at(realm.getSeed(),cx,cz);
            heights.add(ward.center().getY());
            for (var dir : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH}) {
                var path = ward.spanTo(ward.neighbor(realm.getSeed(),dir),dir);
                if (ward.road(realm.getSeed(),dir)) {
                    intact++;
                    for (int step=1; step<path.size()-1; step++) {
                        var tile=path.get(step);
                        test.assertTrue(!realm.getBlockState(tile).isAir(), "Missing bridge plank at "+tile);
                        test.assertTrue(realm.getBlockState(tile.above(2)).isAir(), "Insufficient bridge headroom at "+tile);
                        test.assertTrue(Math.abs(tile.getY()-path.get(step-1).getY())<=1, "Bridge step too steep");
                    }
                } else {
                    broken++;
                    test.assertTrue(realm.getBlockState(path.get(path.size()/2)).isAir(), "Broken span filled in");
                }
            }
        }
        test.assertTrue(heights.size()>=4 && intact>0 && broken>0, "Missing elevation or broken passages");
        test.succeed();
    }

    @GameTest(template = "clearing", timeoutTicks = 400)
    public static void fanReturnsToDepartureAndVoidReturnsToSpawn(GameTestHelper test) {
        var home = test.getLevel();
        var player = new FakePlayer(home, new GameProfile(UUID.randomUUID(), "veil_traveller")) {
            @Override
            public Entity changeDimension(DimensionTransition transition) {
                // The game-test server has no client for the dimension-change packet.
                // Apply the destination selected by the actual fan and return logic.
                setServerLevel(transition.newLevel());
                setPos(transition.pos());
                return this;
            }
        };
        var start = test.absolutePos(new BlockPos(1, 2, 1));
        var respawn = test.absolutePos(new BlockPos(3, 2, 3));
        for (var pos : new BlockPos[]{start,respawn}) {
            home.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            home.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            home.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        }
        player.setRespawnPosition(home.dimension(), respawn, 0, true, false);
        player.setPos(start.getX()+.5, start.getY(), start.getZ()+.5);
        ItemStack fan = new ItemStack(Interstice.FAN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, fan);
        fan.use(home, player, InteractionHand.MAIN_HAND);
        test.assertTrue(player.level().dimension().equals(Interstice.LEVEL), "Fan did not enter realm");
        test.assertTrue(!player.level().getBlockState(player.blockPosition().below()).isAir(), "Landing over void");
        fan.use(player.level(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(player.level()==home && player.blockPosition().equals(start), "Return ticket lost");
        fan.use(home, player, InteractionHand.MAIN_HAND);
        player.setPos(player.getX(), 1, player.getZ());
        player.setDeltaMovement(0,-2,0);
        player.fallDistance=42;
        MapleFan.returnFromVoid(new PlayerTickEvent.Pre(player));
        test.assertTrue(player.level()==home && player.blockPosition().equals(respawn), "Void did not return to personal spawn");
        test.assertTrue(player.fallDistance==0 && player.getDeltaMovement().equals(Vec3.ZERO), "Void preserved fall damage");
        test.succeed();
    }
}
