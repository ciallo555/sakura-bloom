package dev.sakurabloom.adventure.pouch;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PouchMenu extends AbstractContainerMenu {
    static final int INVENTORY_Y = 122;
    private final Inventory inventory;
    private final int bagSlot;
    private final ItemStack openedBag;

    public PouchMenu(int id, Inventory inventory, int bagSlot) {
        super(PouchContent.MENU.get(), id);
        this.inventory = inventory;
        this.bagSlot = bagSlot;
        this.openedBag = bag();
        for (int row = 0; row < 3; row++)
            for (int x = 0; x < 9; x++)
                inventorySlot(9 + row * 9 + x, 8 + x * 18, INVENTORY_Y + row * 18);
        for (int x = 0; x < 9; x++) inventorySlot(x, 8 + x * 18, INVENTORY_Y + 58);
        // Include the offhand in vanilla container synchronization even though it isn't an inventory cell.
        if (bagSlot == Inventory.SLOT_OFFHAND) inventorySlot(bagSlot, -2000, -2000);
    }

    ItemStack bag() { return inventory.getItem(bagSlot); }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player && bag().getItem() instanceof PouchItem
            && (player.level().isClientSide || bag() == openedBag);
    }

    private void inventorySlot(int index, int x, int y) {
        if (index != bagSlot) {
            addSlot(new Slot(inventory, index, x, y));
            return;
        }
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return getContainerSlot() != Inventory.SLOT_OFFHAND; }
        });
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || type == ClickType.SWAP && button == bagSlot) return;
        if (slot >= 0 && slot < slots.size() && getSlot(slot).getContainerSlot() == bagSlot) return;
        super.clicked(slot, button, type, player);
    }

    private void save(PouchLayout changed) {
        if (changed.pieces().isEmpty()) bag().remove(PouchContent.LAYOUT.get());
        else bag().set(PouchContent.LAYOUT.get(), changed);
        inventory.setChanged();
    }

    static int button(int x, int y, int turn) { return (turn * 4 + y) * 5 + x; }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button >= 80) return false;
        var pouch = (PouchItem)bag().getItem();
        int x = button % 5, y = button / 5 % 4;
        if (x >= pouch.columns || y >= pouch.rows) return false;
        var layout = PouchItem.layout(bag());
        if (getCarried().isEmpty()) {
            var existing = layout.at(x, y);
            if (existing == null) return false;
            save(layout.remove(existing));
            setCarried(existing.stack().copy());
        } else {
            var piece = new PouchLayout.Piece(getCarried().copyWithCount(1), x, y, button / 20);
            if (!layout.fits(piece, pouch)) return false;
            save(layout.add(piece));
            getCarried().shrink(1);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(slotIndex);
        var stack = slot.getItem();
        if (!slot.mayPickup(player) || CharmShape.of(stack) == null) return ItemStack.EMPTY;
        var pouch = (PouchItem)bag().getItem();
        var layout = PouchItem.layout(bag());
        for (int turn = 0; turn < 4; turn++) {
            for (int y = 0; y < pouch.rows; y++) {
                for (int x = 0; x < pouch.columns; x++) {
                    var placed = new PouchLayout.Piece(stack.copyWithCount(1), x, y, turn);
                    if (layout.fits(placed, pouch)) {
                        save(layout.add(placed));
                        slot.remove(1);
                        slot.setChanged();
                        return placed.stack().copy();
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
