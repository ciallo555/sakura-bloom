package dev.sakurabloom.cuisine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

final class KitchenBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> ingredients = NonNullList.withSize(KitchenRecipe.MAX_INGREDIENTS, ItemStack.EMPTY);
    private ItemStack meal = ItemStack.EMPTY;
    private Ingredient serving = Ingredient.EMPTY;
    private int remaining;

    KitchenBlockEntity(BlockPos pos, BlockState state) {
        super(KitchenContent.COOKWARE.get(), pos, state);
    }

    CraftingInput input() {
        return CraftingInput.of(ingredients.size(), 1, ingredients);
    }

    List<ItemStack> displayItems() {
        return ready() ? List.of(meal) : ingredients;
    }

    boolean addIngredient(ItemStack item) {
        for (int slot = 0; slot < ingredients.size(); slot++) {
            if (ingredients.get(slot).isEmpty()) {
                ingredients.set(slot, item.copyWithCount(1));
                contentsChanged();
                return true;
            }
        }
        return false;
    }

    void returnIngredients(Player player) {
        for (int slot = 0; slot < ingredients.size(); slot++) {
            ItemStack item = ingredients.set(slot, ItemStack.EMPTY);
            if (!item.isEmpty()) {
                player.getInventory().placeItemBackInInventory(item);
            }
        }
        contentsChanged();
    }

    void returnLegacyBowls(Player player) {
        boolean changed = false;
        for (int slot = 0; slot < ingredients.size(); slot++) {
            if (ingredients.get(slot).is(Items.BOWL)) {
                player.getInventory().placeItemBackInInventory(ingredients.set(slot, ItemStack.EMPTY));
                changed = true;
            }
        }
        if (changed) {
            contentsChanged();
        }
    }

    boolean cooking() {
        return remaining > 0;
    }

    boolean ready() {
        return !cooking() && !meal.isEmpty();
    }

    Ingredient serving() {
        return serving;
    }

    void start(KitchenRecipes.Batch batch) {
        meal = batch.meal();
        serving = batch.serving();
        remaining = batch.ticks();
        contentsChanged();
    }

    ItemStack collect() {
        ItemStack result = meal.split(serving.isEmpty() ? meal.getCount() : 1);
        if (meal.isEmpty()) {
            serving = Ingredient.EMPTY;
        }
        contentsChanged();
        return result;
    }

    static void tick(Level level, BlockPos pos, BlockState state, KitchenBlockEntity cookware) {
        if (!cookware.cooking() || !KitchenBlock.hasHeat(level, pos)) {
            return;
        }
        cookware.remaining--;
        if (!cookware.cooking()) {
            for (ItemStack ingredient : cookware.ingredients) {
                if (ingredient.hasCraftingRemainingItem()) {
                    Block.popResource(level, pos, ingredient.getCraftingRemainingItem());
                }
            }
            cookware.ingredients.clear();
            level.setBlockAndUpdate(pos, state.setValue(KitchenBlock.COOKING, false));
            level.playSound(null, pos, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 0.7F, 1.35F);
            cookware.contentsChanged();
        } else {
            cookware.setChanged();
        }
    }

    void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, ingredients);
        if (ready() && serving.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, meal);
        }
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            var state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, ingredients, registries);
        tag.put("Meal", meal.saveOptional(registries));
        tag.putInt("CookTime", remaining);
        if (!serving.isEmpty()) {
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            Ingredient.CODEC_NONEMPTY.encodeStart(ops, serving).result().ifPresent(encoded -> tag.put("Serving", encoded));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingredients.clear();
        ContainerHelper.loadAllItems(tag, ingredients, registries);
        meal = ItemStack.parseOptional(registries, tag.getCompound("Meal"));
        remaining = meal.isEmpty() ? 0 : Math.max(0, tag.getInt("CookTime"));
        // Meals saved before serving containers were separate already include their bowl.
        serving = tag.contains("Serving")
            ? Ingredient.CODEC_NONEMPTY.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Serving"))
                .result().orElse(Ingredient.EMPTY)
            : Ingredient.EMPTY;
    }
}
