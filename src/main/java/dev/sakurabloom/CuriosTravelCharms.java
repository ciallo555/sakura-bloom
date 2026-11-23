package dev.sakurabloom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

final class CuriosTravelCharms {
    private CuriosTravelCharms() {}

    static boolean isEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.isEquipped(item)).orElse(false);
    }
}
