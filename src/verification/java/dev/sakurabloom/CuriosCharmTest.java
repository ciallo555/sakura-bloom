package dev.sakurabloom;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import top.theillusivec4.curios.api.CuriosApi;

final class CuriosCharmTest {
    static void check(GameTestHelper test) {
        var wearer = test.makeMockPlayer(GameType.SURVIVAL);
        var inventory = CuriosApi.getCuriosInventory(wearer).orElseThrow();
        for (var equipped : BloomContent.TRAVEL_CHARMS) {
            var slots = inventory.getStacksHandler(equipped.get().slot).orElseThrow().getStacks();
            for (var candidate : BloomContent.TRAVEL_CHARMS) {
                test.assertTrue(slots.isItemValid(0, candidate.toStack()) == (candidate == equipped),
                    "Incorrect Curios slot for " + candidate.getId() + " in " + equipped.get().slot);
            }
            test.assertTrue(slots.insertItem(0, equipped.toStack(), false).isEmpty(), "Could not equip " + equipped.getId());
        }
        TravelCharmItem.refreshBonuses(wearer);
        double speed = wearer.getAttributeValue(Attributes.MOVEMENT_SPEED);
        test.assertTrue(wearer.getMaxHealth() == 24 && wearer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) == 9
            && wearer.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) == 1.25, "Different slot bonuses must coexist");
        wearer.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        TravelCharmItem.refreshBonuses(wearer);
        var belt = inventory.getStacksHandler("belt").orElseThrow().getStacks();
        belt.extractItem(0, 1, false);
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed
            && wearer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) == 9, "Duplicate slots must count as one equipped bell");
        wearer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(wearer);
        test.assertTrue(wearer.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)==3, "Last bell removal left its bonus");
    }
}
