package dev.sakurabloom.cuisine;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

class KitchenCropBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seed;

    KitchenCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = seed;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

}
