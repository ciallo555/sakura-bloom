package dev.sakurabloom;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class CourtyardContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SakuraBloom.ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SakuraBloom.ID);
    static final DeferredItem<Item> BAMBOO_SPLINTS = ITEMS.registerSimpleItem("bamboo_splints");
    static final DeferredBlock<FurnishingBlock> TATAMI = BLOCKS.register("bound_tatami", () ->
        new FurnishingBlock(FurnishingBlock.Style.MAT, Properties.ofFullCopy(Blocks.MOSS_CARPET)
            .mapColor(MapColor.SAND).sound(SoundType.CROP).ignitedByLava()));
    static final List<DeferredBlock<FurnishingBlock>> SHOJI = registerWoodScreens("_shoji", 0.8F);
    static final List<DeferredBlock<FurnishingBlock>> WOOD_LATTICES = registerWoodScreens("_lattice", 1.3F);
    static final Map<DyeColor, DeferredBlock<FurnishingBlock>> NOREN = registerNoren();
    static final DeferredBlock<FurnishingBlock> STONE_LANTERN = BLOCKS.register("yukimi_lantern", () ->
        new FurnishingBlock(FurnishingBlock.Style.STONE_LANTERN, Properties.ofFullCopy(Blocks.ANDESITE)
            .noOcclusion().lightLevel(state -> 13)));
    static final List<DeferredBlock<FurnishingBlock>> LATTICES = List.of(
        screen("begonia_lattice", () -> Blocks.CHERRY_PLANKS, 1.3F),
        screen("key_fret_lattice", () -> Blocks.DARK_OAK_PLANKS, 1.3F),
        screen("ice_crackle_lattice", () -> Blocks.BIRCH_PLANKS, 1.3F),
        screen("diamond_lattice", () -> Blocks.BAMBOO_PLANKS, 1.3F),
        screen("brocade_lattice", () -> Blocks.MANGROVE_PLANKS, 1.3F));
    static final DeferredBlock<PalaceLanternBlock> PALACE_LANTERN = BLOCKS.register("silk_palace_lantern", () ->
        new PalaceLanternBlock(Properties.ofFullCopy(Blocks.LANTERN).mapColor(MapColor.COLOR_RED)
            .sound(SoundType.BAMBOO_WOOD).strength(0.8F).lightLevel(state -> 14)));
    static final List<DeferredBlock<FurnishingBlock>> VASES = List.of(
        vase("blue_blossom_vase", FurnishingBlock.Style.VASE, MapColor.QUARTZ),
        vase("celadon_bamboo_vase", FurnishingBlock.Style.SLENDER_VASE, MapColor.COLOR_LIGHT_GREEN),
        vase("rouge_plum_vase", FurnishingBlock.Style.SHOULDERED_VASE, MapColor.TERRACOTTA_PINK),
        vase("ink_magnolia_vase", FurnishingBlock.Style.TRUMPET_VASE, MapColor.QUARTZ),
        vase("amber_persimmon_vase", FurnishingBlock.Style.ROUND_VASE, MapColor.TERRACOTTA_ORANGE));
    record Roof(DeferredBlock<FurnishingBlock> tiles, DeferredBlock<StairBlock> stairs, DeferredBlock<RoofSlabBlock> slab) {}
    static final Map<DyeColor, Roof> ROOFS = registerRoofs();

    static final List<DeferredBlock<? extends Block>> DECORATIONS;

    static {
        List<DeferredBlock<? extends Block>> blocks = new ArrayList<>();
        blocks.add(TATAMI);
        blocks.addAll(SHOJI);
        blocks.addAll(NOREN.values());
        blocks.add(STONE_LANTERN);
        blocks.addAll(LATTICES);
        blocks.addAll(WOOD_LATTICES);
        blocks.add(PALACE_LANTERN);
        blocks.addAll(VASES);
        ROOFS.values().forEach(roof -> blocks.addAll(List.of(roof.tiles(), roof.stairs(), roof.slab())));
        DECORATIONS = List.copyOf(blocks);
        DECORATIONS.forEach(ITEMS::registerSimpleBlockItem);
    }

    private static DeferredBlock<FurnishingBlock> screen(String name, Supplier<Block> wood, float hardness) {
        return BLOCKS.register(name, () -> new FurnishingBlock(FurnishingBlock.Style.SCREEN,
            Properties.ofFullCopy(wood.get()).strength(hardness).noOcclusion()));
    }

    private static List<DeferredBlock<FurnishingBlock>> registerWoodScreens(String suffix, float hardness) {
        List<DeferredBlock<FurnishingBlock>> screens = new ArrayList<>();
        for (Block wood : List.of(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS,
                Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS,
                Blocks.BAMBOO_PLANKS, Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS)) {
            String name = BuiltInRegistries.BLOCK.getKey(wood).getPath().replace("_planks", suffix);
            screens.add(screen(name, () -> wood, hardness));
        }
        for (var wood : BloomContent.WOODS) {
            var planks = wood.planks();
            screens.add(screen(planks.getId().getPath().replace("_planks", suffix), planks, hardness));
        }
        return List.copyOf(screens);
    }

    private static Map<DyeColor, DeferredBlock<FurnishingBlock>> registerNoren() {
        var curtains = new EnumMap<DyeColor, DeferredBlock<FurnishingBlock>>(DyeColor.class);
        for (DyeColor dye : DyeColor.values()) {
            String name = dye == DyeColor.BLUE ? "indigo_noren" : dye.getName() + "_noren";
            curtains.put(dye, BLOCKS.register(name, () -> new FurnishingBlock(FurnishingBlock.Style.CURTAIN,
                Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(dye.getMapColor()).noOcclusion().noCollission())));
        }
        return Collections.unmodifiableMap(curtains);
    }

    private static Map<DyeColor, Roof> registerRoofs() {
        var roofs = new EnumMap<DyeColor, Roof>(DyeColor.class);
        for (var dye : DyeColor.values()) {
            String prefix = dye.getName() + "_roof_tile";
            var color = dye == DyeColor.GRAY ? MapColor.DEEPSLATE : dye.getMapColor();
            var tiles = BLOCKS.register(prefix + "s", () -> new FurnishingBlock(FurnishingBlock.Style.TILES,
                Properties.ofFullCopy(Blocks.BRICKS).mapColor(color)));
            var stairs = BLOCKS.register(prefix + "_stairs", () ->
                new StairBlock(tiles.get().defaultBlockState(), Properties.ofFullCopy(tiles.get())));
            var slab = BLOCKS.register(prefix + "_slab", () -> new RoofSlabBlock(Properties.ofFullCopy(tiles.get())));
            roofs.put(dye, new Roof(tiles, stairs, slab));
        }
        return Collections.unmodifiableMap(roofs);
    }

    private static DeferredBlock<FurnishingBlock> vase(String name, FurnishingBlock.Style form, MapColor color) {
        return BLOCKS.register(name, () -> new FurnishingBlock(form, Properties.of().mapColor(color)
            .sound(SoundType.DECORATED_POT).strength(0.6F).noOcclusion()));
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    static void addCreativeItems(CreativeModeTab.Output output) {
        output.accept(BAMBOO_SPLINTS.get());
        DECORATIONS.forEach(block -> output.accept(block.get()));
    }

    static void setupFire(FireBlock fire) {
        for (var screen : SHOJI) {
            if (screen.get().defaultBlockState().ignitedByLava()) fire.setFlammable(screen.get(), 5, 20);
        }
        LATTICES.forEach(frame -> fire.setFlammable(frame.get(), 5, 20));
        for (var frame : WOOD_LATTICES) {
            if (frame.get().defaultBlockState().ignitedByLava()) fire.setFlammable(frame.get(), 5, 20);
        }
        fire.setFlammable(TATAMI.get(), 30, 60);
        NOREN.values().forEach(curtain -> fire.setFlammable(curtain.get(), 30, 60));
    }

    private CourtyardContent() {}
}
