package dev.sakurabloom;

import dev.sakurabloom.adventure.pouch.PouchContent;
import dev.sakurabloom.adventure.pouch.PouchMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class PouchGameTests {
    @GameTest(template = "clearing")
    public static void puzzlePlacementPackingAndWithdrawal(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var pouch = PouchContent.KNOTWORK.toStack();
        player.setItemInHand(InteractionHand.OFF_HAND, pouch);
        var menu = new PouchMenu(1, player.getInventory(), Inventory.SLOT_OFFHAND);
        var bell = BloomContent.WIND_BELL.toStack();
        bell.set(DataComponents.CUSTOM_NAME, Component.literal("鐜夊搷"));
        menu.setCarried(bell.copy());
        test.assertFalse(menu.clickMenuButton(player, 3+20), "Horizontal bell crosses right border");
        test.assertTrue(menu.clickMenuButton(player, 1+20), "Three-cell bell fits after rotation");
        test.assertTrue(menu.getCarried().isEmpty(), "Packing kept a cursor copy");
        menu.setCarried(BloomContent.KNOT_CHARM.toStack());
        test.assertFalse(menu.clickMenuButton(player, 0), "Intersecting pieces accepted");
        test.assertTrue(menu.clickMenuButton(player, 5), "Knot fits on lower rows");
        menu.setCarried(BloomContent.WIND_BELL.toStack());
        test.assertFalse(menu.clickMenuButton(player, 0), "Duplicate bell accepted");
        menu.setCarried(PouchContent.CLOUD.toStack());
        test.assertFalse(menu.clickMenuButton(player, 2), "Nested pouch accepted");
        menu.setCarried(ItemStack.EMPTY);
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getMaxHealth() == 20 && player.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.11,
            "An offhand pouch activated its contents");

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            CuriosCharmTest.equip(player,"belt",pouch.copy());
            TravelCharmItem.refreshBonuses(player);
            test.assertTrue(player.getMaxHealth()==24 && player.getAttributeValue(Attributes.MOVEMENT_SPEED)>.11, "Worn belt pouch did not activate both charms");
            CuriosCharmTest.equip(player,"belt",ItemStack.EMPTY);
            TravelCharmItem.refreshBonuses(player);
        }
        var restored = ItemStack.parseOptional(test.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)pouch.save(test.getLevel().registryAccess()));
        player.setItemInHand(InteractionHand.OFF_HAND, restored);
        test.assertFalse(menu.clickMenuButton(player, 3), "Stale menu accessed replacement bag");
        var reopened = new PouchMenu(2, player.getInventory(), Inventory.SLOT_OFFHAND);
        test.assertTrue(reopened.clickMenuButton(player, 3), "Could not pick up by non-anchor cell");
        test.assertTrue(ItemStack.matches(bell, reopened.getCarried()), "Charm stack did not survive packing and serialization");
        TravelCharmItem.refreshBonuses(player);
        test.assertTrue(player.getMaxHealth() == 20 && player.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.11, "Removed charm still active");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void largePouchSupportsExtraRowAndColumn(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, PouchContent.CLOUD.toStack());
        var menu = new PouchMenu(1, player.getInventory(), player.getInventory().selected);
        menu.setCarried(BloomContent.WIND_BELL.toStack());
        test.assertTrue(menu.clickMenuButton(player, 9), "Large pouch must allow placing down its fifth column");
        test.assertTrue(menu.clickMenuButton(player, 19), "Bottom-right cell should hold the bell");
        player.setItemInHand(InteractionHand.MAIN_HAND, PouchContent.KNOTWORK.toStack());
        var small = new PouchMenu(2, player.getInventory(), player.getInventory().selected);
        small.setCarried(BloomContent.WIND_BELL.toStack());
        test.assertFalse(small.clickMenuButton(player, 9), "Small pouch accepted a fifth column");
        test.assertFalse(small.clickMenuButton(player, 19), "Small pouch accepted a fourth row");
        test.assertFalse(small.clickMenuButton(player, -1), "Malformed button accepted");
        test.succeed();
    }
}
