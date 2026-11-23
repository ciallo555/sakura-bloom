package dev.sakurabloom;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

public final class BloomBoatTypes {
    public static final EnumProxy<Boat.Type> SHIDARE = variant("shidare", () -> BloomContent.SHIDARE);
    public static final EnumProxy<Boat.Type> YAE = variant("yae", () -> BloomContent.YAE);
    public static final EnumProxy<Boat.Type> YAMAZAKURA = variant("yamazakura", () -> BloomContent.YAMAZAKURA);

    private BloomBoatTypes() {}

    private static EnumProxy<Boat.Type> variant(String name, Supplier<BloomContent.CherryWood> wood) {
        // Boat.Type initializes during vanilla bootstrap. Resolve our registered blocks and items only when used.
        Supplier<Block> planks = () -> wood.get().planks().get();
        Supplier<Item> boat = () -> wood.get().boats().boat().get();
        Supplier<Item> chestBoat = () -> wood.get().boats().chestBoat().get();
        Supplier<Item> sticks = () -> Items.STICK;
        return new EnumProxy<>(Boat.Type.class, planks, SakuraBloom.ID + ":" + name, boat, chestBoat, sticks, false);
    }
}
