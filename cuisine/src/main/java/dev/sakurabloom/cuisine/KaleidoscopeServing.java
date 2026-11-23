package dev.sakurabloom.cuisine;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

final class KaleidoscopeServing {
    static final String RECIPE_PATH = "compat/kaleidoscope/steaming/";
    private static final TagKey<Item> RICE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("c", "crops/rice"));

    private KaleidoscopeServing() {}

    static void serve(UseItemOnBlockEvent event) {
        var player = event.getPlayer();
        var bowl = event.getItemStack();
        var level = event.getLevel();
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK || player == null || !bowl.is(Items.BOWL)
            || !(level.getBlockEntity(event.getPos()) instanceof SteamerBlockEntity steamer)) {
            return;
        }
        var above = event.getPos().above();
        if (level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN)) {
            return;
        }

        var contents = steamer.getItems();
        int capacity = steamer.getBlockState().getValue(SteamerBlock.HALF) ? 4 : contents.size();
        boolean pending = false;
        for (int slot = 0; slot < capacity; slot++) {
            var food = contents.get(slot);
            var meal = serving(food);
            if (!meal.isEmpty() && steamer.getCookingTime()[slot] == -1) {
                if (!level.isClientSide) {
                    bowl.consume(1, player);
                    food.shrink(1);
                    if (food.isEmpty()) {
                        contents.set(slot, ItemStack.EMPTY);
                        steamer.getCookingTime()[slot] = 0;
                        steamer.getCookingProgress()[slot] = 0;
                    }
                    steamer.refresh();
                    player.getInventory().placeItemBackInInventory(meal);
                }
                event.cancelWithResult(ItemInteractionResult.sidedSuccess(level.isClientSide));
                return;
            }
            pending |= food.is(RICE) || food.is(KitchenContent.CHAWANMUSHI_MIX.get());
        }
        if (pending) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.sakura_bloom.cooking.bowl"), true);
            }
            event.cancelWithResult(ItemInteractionResult.sidedSuccess(level.isClientSide));
        }
    }

    private static ItemStack serving(ItemStack food) {
        if (food.is(KitchenContent.UNPLATED_STEAMED_RICE.get())) {
            return new ItemStack(BuiltInRegistries.ITEM.get(KitchenContent.STEAMED_RICE_ID));
        }
        if (food.is(KitchenContent.UNPLATED_CHAWANMUSHI.get())) {
            return KitchenContent.CHAWANMUSHI.toStack();
        }
        return ItemStack.EMPTY;
    }
}
