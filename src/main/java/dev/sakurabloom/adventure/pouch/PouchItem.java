package dev.sakurabloom.adventure.pouch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public final class PouchItem extends Item {
    final int columns, rows;

    PouchItem(int columns, int rows) {
        super(new Properties().stacksTo(1).rarity(columns == 4 ? Rarity.UNCOMMON : Rarity.RARE));
        this.columns = columns;
        this.rows = rows;
    }

    static PouchLayout layout(ItemStack bag) {
        return bag.getOrDefault(PouchContent.LAYOUT.get(), PouchLayout.EMPTY);
    }

    public static void collect(ItemStack bag, Set<Item> equipped) {
        if (bag.getItem() instanceof PouchItem)
            for (var piece : layout(bag).pieces()) equipped.add(piece.stack().getItem());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            int index = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            server.openMenu(new SimpleMenuProvider((id, inv, p) -> new PouchMenu(id, inv, index), stack.getHoverName()),
                buf -> buf.writeVarInt(index));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("pouch.sakura_bloom.capacity", columns, rows, layout(stack).pieces().size()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("pouch.sakura_bloom.wear").withStyle(ChatFormatting.GRAY));
    }
}
