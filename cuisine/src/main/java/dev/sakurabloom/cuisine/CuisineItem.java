package dev.sakurabloom.cuisine;

import dev.sakurabloom.HintedBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.extensions.IItemExtension;

/** Attribute the pre-split registry IDs to the add-on in item browsers. */
interface CuisineItem extends IItemExtension {
    @Override
    default String getCreatorModId(ItemStack stack) {
        return SakuraCuisine.ID;
    }

    class Ingredient extends Item implements CuisineItem {
        Ingredient(Properties properties) {
            super(properties);
        }
    }

    final class Seed extends ItemNameBlockItem implements CuisineItem {
        Seed(Block crop) {
            super(crop, new Properties());
        }
    }

    final class Utensil extends HintedBlockItem implements CuisineItem {
        Utensil(Block block) {
            super(block, new Properties());
        }
    }
}
