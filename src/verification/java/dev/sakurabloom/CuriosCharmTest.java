package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;

final class CuriosCharmTest {
    static void check(GameTestHelper test) {
        var wearer = test.makeMockPlayer(GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(wearer).orElseThrow();
        var expected = Map.of("belt", BloomContent.WIND_BELL, "charm", BloomContent.KNOT_CHARM,
            "necklace", BloomContent.FISH_PENDANT, "bracelet", BloomContent.INK_BRACELET);
        for (var entry : expected.entrySet()) {
            var equipped = entry.getValue();
            var slots = inventory.getStacksHandler(entry.getKey()).orElseThrow().getStacks();
            for (var candidate : BloomContent.TRAVEL_CHARMS) {
                test.assertTrue(slots.isItemValid(0, candidate.toStack()) == (candidate == equipped),
                    "Incorrect Curios slot for " + candidate.getId() + " in " + entry.getKey());
            }
            test.assertTrue(slots.insertItem(0, equipped.toStack(), false).isEmpty(), "Could not equip " + equipped.getId());
        }
        TravelCharmItem.refreshBonuses(wearer);
        double speed = wearer.getAttributeValue(Attributes.MOVEMENT_SPEED);
        wearer.causeFallDamage(100, 1, wearer.damageSources().fall());
        test.assertTrue(wearer.getMaxHealth() == 24 && wearer.getHealth() == 20 && wearer.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 1
            && !wearer.hasEffect(MobEffects.WATER_BREATHING), "Different slot bonuses must coexist");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(wearer, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        test.assertTrue(wearer.getEffect(MobEffects.REGENERATION).getDuration() == 100, "No meal bonus in the charm slot");
        var pool = new BlockPos(4,2,4);
        test.setBlock(pool, Blocks.WATER);
        wearer.setPos(Vec3.atBottomCenterOf(test.absolutePos(pool)));
        wearer.baseTick();
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.hasEffect(MobEffects.WATER_BREATHING) && Math.abs(wearer.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)-1.30)<1e-6,
            "Wading with the necklace did not activate its bonuses");
        wearer.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Offhand and belt stacked twice");
        var belt = inventory.getStacksHandler("belt").orElseThrow().getStacks();
        belt.extractItem(0, 1, false);
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Removing the belt disabled the offhand bell");
        wearer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed, "Last bell removal left its bonus");
    }
}
