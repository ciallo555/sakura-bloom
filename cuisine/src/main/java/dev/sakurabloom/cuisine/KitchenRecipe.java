package dev.sakurabloom.cuisine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

public final class KitchenRecipe extends ShapelessRecipe {
    static final int MAX_INGREDIENTS = 9;
    private final Station station;
    private final int cookingTime;
    private final Ingredient serving;

    KitchenRecipe(Station station, List<Ingredient> ingredients, ItemStack result, int cookingTime, Ingredient serving) {
        super("", CraftingBookCategory.MISC, result, NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new)));
        this.station = station;
        this.cookingTime = cookingTime;
        this.serving = serving;
    }

    public Station station() {
        return station;
    }

    public int cookingTime() {
        return cookingTime;
    }

    public Ingredient serving() {
        return serving;
    }

    @Override
    public RecipeType<?> getType() {
        return KitchenContent.COOKING.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return KitchenContent.COOKING_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public enum Station implements StringRepresentable {
        STEAMER("bamboo_steamer"), WOK("iron_wok");

        static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);
        private final String name;

        Station(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    static final class Serializer implements RecipeSerializer<KitchenRecipe> {
        private static final MapCodec<KitchenRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Station.CODEC.fieldOf("station").forGetter(KitchenRecipe::station),
            Ingredient.CODEC_NONEMPTY.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(KitchenRecipe::getIngredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null)),
            Codec.intRange(1, 12000).fieldOf("cooking_time").forGetter(KitchenRecipe::cookingTime),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("serving", Ingredient.EMPTY).forGetter(KitchenRecipe::serving)
        ).apply(instance, KitchenRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INGREDIENTS =
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS));
        private static final StreamCodec<RegistryFriendlyByteBuf, KitchenRecipe> STREAM = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeEnum(recipe.station());
                INGREDIENTS.encode(buffer, recipe.getIngredients());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
                buffer.writeVarInt(recipe.cookingTime());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.serving());
            },
            buffer -> new KitchenRecipe(buffer.readEnum(Station.class), INGREDIENTS.decode(buffer),
                ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
        );

        @Override
        public MapCodec<KitchenRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KitchenRecipe> streamCodec() {
            return STREAM;
        }
    }
}
