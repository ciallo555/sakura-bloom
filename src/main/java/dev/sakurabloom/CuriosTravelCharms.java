package dev.sakurabloom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;

final class CuriosTravelCharms {
    private CuriosTravelCharms() {}

    static boolean isEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.isEquipped(item)).orElse(false);
    }

    static Set<Item> equipped(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player);
        if (inventory.isEmpty()) return Set.of();
        var matches = inventory.get().findCurios(stack -> stack.getItem() instanceof TravelCharmItem);
        if (matches.isEmpty()) return Set.of();
        var result = new HashSet<Item>();
        for (var match : matches) result.add(match.stack().getItem());
        return result;
    }
}
