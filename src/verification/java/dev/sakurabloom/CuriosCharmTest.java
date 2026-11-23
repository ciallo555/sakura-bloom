package dev.sakurabloom;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import top.theillusivec4.curios.api.CuriosApi;

final class CuriosCharmTest {
    static void check(GameTestHelper test) {
        var wearer = test.makeMockPlayer(GameType.SURVIVAL);
        var charms = CuriosApi.getCuriosInventory(wearer).orElseThrow().getStacksHandler("charm").orElseThrow().getStacks();
        for (var charm : BloomContent.TRAVEL_CHARMS) {
            test.assertTrue(charms.isItemValid(0, charm.toStack()), "Curios rejected " + charm.getId());
        }
        test.assertTrue(charms.insertItem(0, BloomContent.WIND_BELL.toStack(), false).isEmpty(), "Charm slot refused the bell");
        wearer.causeFallDamage(8, 1, wearer.damageSources().fall());
        test.assertTrue(wearer.getHealth() == 18, "Empty off hand and equipped Curios bell should cushion the fall");
        wearer.setItemInHand(InteractionHand.OFF_HAND, BloomContent.WIND_BELL.toStack());
        wearer.invulnerableTime = 0;
        wearer.causeFallDamage(8, 1, wearer.damageSources().fall());
        test.assertTrue(wearer.getHealth() == 16, "Duplicate bells must not stack");
    }
}
