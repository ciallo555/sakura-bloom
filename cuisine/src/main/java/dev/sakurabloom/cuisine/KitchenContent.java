package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;
import dev.sakurabloom.HintedBlockItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.sakurabloom.SakuraBloom.ID;

final class KitchenContent {
    private static final String FARMERS_DELIGHT = "farmersdelight";
    static final ResourceLocation STEAMED_RICE_ID = SakuraBloom.id("steamed_rice");
    static final ResourceLocation FARMERS_DELIGHT_COOKED_RICE_ID =
        ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT, "cooked_rice");
    static final ResourceLocation TOMATO_EGG_ID = SakuraBloom.id("tomato_egg");
    static final ResourceLocation KALEIDOSCOPE_TOMATO_EGG_ID =
        ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "scramble_egg_with_tomatoes");

    // Keep pre-split registry keys so existing inventories, crops and cookware load unchanged.
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);

    static final DeferredHolder<RecipeType<?>, RecipeType<KitchenRecipe>> COOKING =
        RECIPE_TYPES.register("cooking", key -> RecipeType.simple(key));
    static final DeferredHolder<RecipeSerializer<?>, KitchenRecipe.Serializer> COOKING_SERIALIZER =
        SERIALIZERS.register("cooking", KitchenRecipe.Serializer::new);

    static final DeferredBlock<TeaKettleBlock> KETTLE = BLOCKS.register("tea_kettle",
        () -> new TeaKettleBlock(Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(1.5F, 5.0F).sound(SoundType.COPPER).noOcclusion().pushReaction(PushReaction.BLOCK)));
    static final DeferredItem<HintedBlockItem> KETTLE_ITEM = stationItem(KETTLE);
    static final DeferredItem<TeaItem> SHIDARE_TEA = tea("shidare", () -> new MobEffectInstance(MobEffects.REGENERATION, 120));
    static final DeferredItem<TeaItem> YAE_TEA = tea("yae", () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900));
    static final DeferredItem<TeaItem> YAMAZAKURA_TEA = tea("yamazakura", () -> new MobEffectInstance(MobEffects.NIGHT_VISION, 1500));

    static final Crop<KitchenCropBlock> CABBAGE = vegetable("cabbage");
    static final Crop<KitchenCropBlock> TOMATO = vegetable("tomato");
    static final Crop<PaddyRiceBlock> RICE = crop("rice",
        seed -> new PaddyRiceBlock(Properties.ofFullCopy(Blocks.WHEAT), seed));
    static final DeferredBlock<RicePanicleBlock> RICE_PANICLE = BLOCKS.register("rice_panicle",
        () -> new RicePanicleBlock(Properties.ofFullCopy(Blocks.DANDELION).offsetType(OffsetType.NONE)));
    static final DeferredBlock<WildCabbageBlock> WILD_CABBAGE = BLOCKS.register("wild_cabbage",
        () -> new WildCabbageBlock(Properties.ofFullCopy(Blocks.DANDELION).offsetType(OffsetType.NONE)));
    static final DeferredBlock<WildRiceBlock> WILD_RICE = BLOCKS.register("wild_rice",
        () -> new WildRiceBlock(Properties.ofFullCopy(Blocks.TALL_GRASS).offsetType(OffsetType.NONE)));
    static final DeferredItem<Item> CABBAGE_ITEM = food("cabbage", 2, 0.35F, false);
    static final DeferredItem<Item> TOMATO_ITEM = food("tomato", 2, 0.3F, false);
    static final DeferredItem<Item> RICE_ITEM = ITEMS.registerItem("rice", CuisineItem.Ingredient::new);
    static final DeferredItem<Item> WHEAT_NOODLES = ITEMS.registerItem("wheat_noodles", CuisineItem.Ingredient::new);

    static final DeferredBlock<KitchenBlock> BAMBOO_STEAMER =
        station(KitchenRecipe.Station.STEAMER, MapColor.SAND, SoundType.BAMBOO_WOOD);
    static final DeferredBlock<KitchenBlock> IRON_WOK =
        station(KitchenRecipe.Station.WOK, MapColor.COLOR_GRAY, SoundType.METAL);
    static final DeferredItem<HintedBlockItem> BAMBOO_STEAMER_ITEM = stationItem(BAMBOO_STEAMER);
    static final DeferredItem<HintedBlockItem> IRON_WOK_ITEM = stationItem(IRON_WOK);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KitchenBlockEntity>> COOKWARE =
        BLOCK_ENTITIES.register("cookware",
            () -> BlockEntityType.Builder.of(KitchenBlockEntity::new, BAMBOO_STEAMER.get(), IRON_WOK.get()).build(null));

    static final DeferredItem<Item> SALMON_ONIGIRI = food("salmon_onigiri", 6, 0.65F, false);
    static final DeferredItem<Item> CHAWANMUSHI = food("chawanmushi", 5, 0.65F, true);
    static final DeferredItem<Item> STEAMED_PORK_ROLL = food("steamed_pork_roll", 7, 0.7F, false);
    static final DeferredItem<Item> YAKISOBA = food("yakisoba", 8, 0.7F, true);
    static final DeferredItem<Item> EGG_FRIED_RICE = food("egg_fried_rice", 7, 0.65F, true);
    static final DeferredItem<Item> RAW_SALMON_ONIGIRI = ITEMS.registerItem("raw_salmon_onigiri", CuisineItem.Ingredient::new);
    static final DeferredItem<Item> CHAWANMUSHI_MIX = ITEMS.registerItem("chawanmushi_mix", CuisineItem.Ingredient::new);
    static final DeferredItem<Item> RAW_PORK_ROLL = ITEMS.registerItem("raw_pork_roll", CuisineItem.Ingredient::new);
    static final DeferredItem<Item> UNPLATED_STEAMED_RICE = ITEMS.registerItem("unplated_steamed_rice", CuisineItem.Ingredient::new);
    static final DeferredItem<Item> UNPLATED_CHAWANMUSHI = ITEMS.registerItem("unplated_chawanmushi", CuisineItem.Ingredient::new);

    private static final List<DeferredItem<? extends Item>> TAB_ITEMS = List.of(
        KETTLE_ITEM, SHIDARE_TEA, YAE_TEA, YAMAZAKURA_TEA,
        CABBAGE.seeds(), TOMATO.seeds(), RICE.seeds(), CABBAGE_ITEM, TOMATO_ITEM, RICE_ITEM, WHEAT_NOODLES,
        BAMBOO_STEAMER_ITEM, IRON_WOK_ITEM, SALMON_ONIGIRI, CHAWANMUSHI, STEAMED_PORK_ROLL,
        YAKISOBA, EGG_FRIED_RICE);

    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SakuraCuisine.ID);

    static {
        TABS.register("cuisine", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sakura_bloom_cuisine"))
            .icon(() -> BAMBOO_STEAMER_ITEM.toStack())
            .displayItems((parameters, output) -> addCreativeItems(output)).build());
    }

    private KitchenContent() {}

    static void register(IEventBus bus) {
        registerSharedMeal(STEAMED_RICE_ID, FARMERS_DELIGHT_COOKED_RICE_ID, 4, 0.5F);
        registerSharedMeal(TOMATO_EGG_ID, KALEIDOSCOPE_TOMATO_EGG_ID, 6, 0.65F);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        RECIPE_TYPES.register(bus);
        SERIALIZERS.register(bus);
        TABS.register(bus);
    }

    static void addCreativeItems(CreativeModeTab.Output output) {
        TAB_ITEMS.forEach(item -> output.accept(item.get()));
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            List.of(RAW_SALMON_ONIGIRI, CHAWANMUSHI_MIX, RAW_PORK_ROLL, UNPLATED_STEAMED_RICE, UNPLATED_CHAWANMUSHI)
                .forEach(item -> output.accept(item.get()));
        }
        for (var local : List.of(STEAMED_RICE_ID, TOMATO_EGG_ID)) {
            if (BuiltInRegistries.ITEM.keySet().contains(local)) {
                output.accept(BuiltInRegistries.ITEM.get(local));
            }
        }
    }

    private static void registerSharedMeal(ResourceLocation local, ResourceLocation other, int nutrition, float saturation) {
        if (ModList.get().isLoaded(other.getNamespace())) {
            ITEMS.addAlias(local, other);
        } else {
            food(local.getPath(), nutrition, saturation, true);
        }
    }

    private static DeferredItem<TeaItem> tea(String name, Supplier<MobEffectInstance> effect) {
        return ITEMS.register(name + "_tea", () -> new TeaItem(effect));
    }

    private static Crop<KitchenCropBlock> vegetable(String name) {
        return crop(name, seed -> new KitchenCropBlock(Properties.ofFullCopy(Blocks.WHEAT), seed));
    }

    private static <T extends KitchenCropBlock> Crop<T> crop(String name, Function<Supplier<Item>, T> factory) {
        String seedName = name + "_seeds";
        var seedKey = SakuraBloom.id(seedName);
        var plant = BLOCKS.register(name + "_crop", () -> factory.apply(() -> BuiltInRegistries.ITEM.get(seedKey)));
        DeferredItem<ItemNameBlockItem> seeds = ITEMS.register(seedName, () -> new CuisineItem.Seed(plant.get()));
        return new Crop<>(plant, seeds);
    }

    private static DeferredBlock<KitchenBlock> station(KitchenRecipe.Station station, MapColor color, SoundType sound) {
        return BLOCKS.register(station.getSerializedName(), () -> new KitchenBlock(station,
            Properties.of().strength(1.25F).mapColor(color).sound(sound).noOcclusion().pushReaction(PushReaction.BLOCK)));
    }

    private static DeferredItem<HintedBlockItem> stationItem(DeferredBlock<?> block) {
        return ITEMS.register(block.getId().getPath(), () -> new CuisineItem.Utensil(block.get()));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation, boolean bowl) {
        FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
        Item.Properties properties = new Item.Properties();
        if (bowl) {
            food.usingConvertsTo(Items.BOWL);
            properties.stacksTo(16);
        }
        return ITEMS.registerItem(name, CuisineItem.Ingredient::new, properties.food(food.build()));
    }

    record Crop<T extends KitchenCropBlock>(DeferredBlock<T> block, DeferredItem<ItemNameBlockItem> seeds) {}
}
