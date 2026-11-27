package dev.sakurabloom;

import dev.sakurabloom.adventure.pouch.PouchItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Set;

final class CuriosTravelCharms {
    private CuriosTravelCharms() {}

    static void collect(Player player, Set<Item> equipped) {
        var inventory = CuriosApi.getCuriosInventory(player);
        if (inventory.isEmpty()) return;
        var matches = inventory.get().findCurios(stack ->
            stack.getItem() instanceof TravelCharmItem || stack.getItem() instanceof PouchItem);
        boolean allowPouch = true;
        for (var match : matches) {
            var stack = match.stack();
            if (stack.getItem() instanceof TravelCharmItem) equipped.add(stack.getItem());
            else if (allowPouch) {
                PouchItem.collect(stack, equipped);
                allowPouch = false;
            }
        }
    }
}
