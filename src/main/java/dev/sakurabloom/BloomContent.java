package dev.sakurabloom;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static dev.sakurabloom.SakuraBloom.ID;
import static dev.sakurabloom.SakuraBloom.id;

public final class BloomContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MortarRecipe>> GRINDING =
        RECIPE_TYPES.register("grinding", key -> RecipeType.simple(key));
    public static final DeferredHolder<RecipeSerializer<?>, MortarRecipe.Serializer> GRINDING_SERIALIZER =
        SERIALIZERS.register("grinding", MortarRecipe.Serializer::new);

    public static final CherryWood SHIDARE = cherryWood("shidare", MapColor.SAND, MapColor.COLOR_BROWN, BloomBoatTypes.SHIDARE);
    public static final CherryWood YAE = cherryWood("yae", MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_RED, BloomBoatTypes.YAE);
    public static final CherryWood YAMAZAKURA = cherryWood("yamazakura", MapColor.TERRACOTTA_PURPLE, MapColor.TERRACOTTA_GRAY, BloomBoatTypes.YAMAZAKURA);
    public static final List<CherryWood> WOODS = List.of(SHIDARE, YAE, YAMAZAKURA);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CherryChestBlock.Entity>> CHERRY_CHEST =
        BLOCK_ENTITIES.register("cherry_chest", () -> BlockEntityType.Builder.of(CherryChestBlock.Entity::new, chestBlocks()).build(null));

    public static final DeferredBlock<MortarBlock> MORTAR = BLOCKS.register("flower_mortar",
        () -> new MortarBlock(Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
            .strength(1.25F, 4.0F).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final DeferredBlock<HanamiLanternBlock> LANTERN = BLOCKS.register("hanami_lantern",
        () -> new HanamiLanternBlock(Properties.of().mapColor(MapColor.SAND).sound(SoundType.CHERRY_WOOD)
            .strength(0.8F).noOcclusion().ignitedByLava()
            .lightLevel(state -> HanamiLanternBlock.glows(state) ? 14 : 0)));

    public static final DeferredItem<BlockItem> MORTAR_ITEM = workstation(MORTAR);
    public static final DeferredItem<BlockItem> LANTERN_ITEM = workstation(LANTERN);

    static {
        TABS.register("sakura_bloom", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sakura_bloom"))
            .icon(() -> LANTERN_ITEM.toStack())
            .displayItems((parameters, output) -> {
                WOODS.forEach(wood -> wood.items().forEach(item -> output.accept(item.get())));
                output.accept(MORTAR_ITEM.get());
                output.accept(LANTERN_ITEM.get());
                CourtyardContent.addCreativeItems(output);
            })
            .build());
    }

    private BloomContent() {}

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        PARTICLES.register(bus);
        RECIPE_TYPES.register(bus);
        SERIALIZERS.register(bus);
        TABS.register(bus);
        CourtyardContent.register(bus);
        bus.addListener(BloomContent::registerSignBlocks);
        bus.addListener(BloomContent::registerChestInventory);
    }

    private static Block[] chestBlocks() {
        return WOODS.stream().map(wood -> wood.chest().get()).toArray(Block[]::new);
    }

    private static void registerChestInventory(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, entity, side) -> {
            var inventory = ChestBlock.getContainer((ChestBlock) state.getBlock(), state, level, pos, true);
            return inventory == null ? null : new InvWrapper(inventory);
        }, chestBlocks());
    }

    private static void registerSignBlocks(BlockEntityTypeAddBlocksEvent event) {
        for (CherryWood wood : WOODS) {
            WoodSigns signs = wood.signs();
            event.modify(BlockEntityType.SIGN, signs.standing().get(), signs.wall().get());
            event.modify(BlockEntityType.HANGING_SIGN, signs.hanging().get(), signs.wallHanging().get());
        }
    }

    static void setupWood() {
        FlowerPotBlock emptyPot = (FlowerPotBlock) Blocks.FLOWER_POT;
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (CherryWood wood : WOODS) {
            emptyPot.addPlant(wood.sapling().getId(), wood.potted());
            for (var holder : wood.woodShapes()) {
                Block block = holder.get();
                fire.setFlammable(block, 5, block instanceof RotatedPillarBlock ? 5 : 20);
            }
            fire.setFlammable(wood.leaves().get(), 30, 60);

            WoodBoats boats = wood.boats();
            DispenserBlock.registerBehavior(boats.boat().get(), new BoatDispenseItemBehavior(boats.type().getValue()));
            DispenserBlock.registerBehavior(boats.chestBoat().get(), new BoatDispenseItemBehavior(boats.type().getValue(), true));
        }
        fire.setFlammable(LANTERN.get(), 5, 20);
        CourtyardContent.setupFire(fire);
    }

    private static CherryWood cherryWood(String name, MapColor timber, MapColor bark, EnumProxy<Boat.Type> boatType) {
        var pollen = PARTICLES.register(name + "_falling_petals", () -> new SimpleParticleType(false));
        TreeGrower grower = new TreeGrower(ID + ":" + name, Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, id(name + "_tree"))),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, id(name + "_tree_bees"))));

        var log = BLOCKS.register(name + "_log", () -> log(Blocks.CHERRY_LOG, timber, bark));
        var wood = BLOCKS.register(name + "_wood", () -> log(Blocks.CHERRY_WOOD, bark, bark));
        var strippedLog = BLOCKS.register("stripped_" + name + "_log",
            () -> log(Blocks.STRIPPED_CHERRY_LOG, timber, timber));
        var strippedWood = BLOCKS.register("stripped_" + name + "_wood",
            () -> log(Blocks.STRIPPED_CHERRY_WOOD, timber, timber));
        var planks = BLOCKS.register(name + "_planks", () -> new Block(copy(Blocks.CHERRY_PLANKS, timber)));
        var stairs = BLOCKS.register(name + "_stairs",
            () -> new StairBlock(planks.get().defaultBlockState(), copy(Blocks.CHERRY_STAIRS, timber)));
        var slab = BLOCKS.register(name + "_slab", () -> new SlabBlock(copy(Blocks.CHERRY_SLAB, timber)));
        var fence = BLOCKS.register(name + "_fence", () -> new FenceBlock(copy(Blocks.CHERRY_FENCE, timber)));
        var gate = BLOCKS.register(name + "_fence_gate",
            () -> new FenceGateBlock(WoodType.CHERRY, copy(Blocks.CHERRY_FENCE_GATE, timber)));
        var door = BLOCKS.register(name + "_door",
            () -> new DoorBlock(BlockSetType.CHERRY, copy(Blocks.CHERRY_DOOR, timber)));
        var trapdoor = BLOCKS.register(name + "_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.CHERRY, copy(Blocks.CHERRY_TRAPDOOR, timber)));
        var plate = BLOCKS.register(name + "_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.CHERRY, copy(Blocks.CHERRY_PRESSURE_PLATE, timber)));
        var button = BLOCKS.register(name + "_button",
            () -> new ButtonBlock(BlockSetType.CHERRY, 30, copy(Blocks.CHERRY_BUTTON, timber)));
        List<DeferredBlock<? extends Block>> woodShapes =
            List.of(log, wood, strippedLog, strippedWood, planks, stairs, slab, fence, gate, door, trapdoor, plate, button);

        var leaves = BLOCKS.register(name + "_leaves",
            () -> new BlossomLeavesBlock(copy(Blocks.CHERRY_LEAVES, timber), pollen));
        var sapling = BLOCKS.register(name + "_sapling",
            () -> new SaplingBlock(grower, copy(Blocks.CHERRY_SAPLING, timber)));
        var potted = BLOCKS.register("potted_" + name + "_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, sapling,
                Properties.ofFullCopy(Blocks.POTTED_CHERRY_SAPLING)));
        var petals = BLOCKS.register(name + "_petals",
            () -> new PinkPetalsBlock(copy(Blocks.PINK_PETALS, timber)));

        WoodSigns signs = registerSigns(name, timber);
        var chest = BLOCKS.register(name + "_chest", () -> new CherryChestBlock(copy(Blocks.CHEST, timber)));
        var boat = ITEMS.register(name + "_boat", () -> new BoatItem(false, boatType.getValue(), new Item.Properties().stacksTo(1)));
        var chestBoat = ITEMS.register(name + "_chest_boat", () -> new BoatItem(true, boatType.getValue(), new Item.Properties().stacksTo(1)));
        var boats = new WoodBoats(boatType, boat, chestBoat);

        var items = new ArrayList<DeferredItem<? extends Item>>();
        woodShapes.forEach(block -> items.add(ITEMS.registerSimpleBlockItem(block)));
        items.add(ITEMS.registerSimpleBlockItem(leaves));
        items.add(ITEMS.registerSimpleBlockItem(sapling));
        items.add(ITEMS.registerSimpleBlockItem(petals));
        items.add(ITEMS.registerSimpleBlockItem(chest));
        items.addAll(List.of(signs.signItem(), signs.hangingItem(), boat, chestBoat));
        return new CherryWood(woodShapes, planks, leaves, sapling, potted, petals, pollen, signs, boats, chest, List.copyOf(items));
    }

    private static WoodSigns registerSigns(String name, MapColor timber) {
        var standing = BLOCKS.register(name + "_sign", () -> {
            WoodType cherry = WoodType.CHERRY;
            WoodType type = WoodType.register(new WoodType(id(name).toString(), cherry.setType(), cherry.soundType(),
                cherry.hangingSignSoundType(), cherry.fenceGateClose(), cherry.fenceGateOpen()));
            return new StandingSignBlock(type, copy(Blocks.CHERRY_SIGN, timber));
        });
        var wall = BLOCKS.register(name + "_wall_sign",
            () -> new WallSignBlock(standing.get().type(), copy(Blocks.CHERRY_WALL_SIGN, timber).lootFrom(standing)));
        var hanging = BLOCKS.register(name + "_hanging_sign",
            () -> new CeilingHangingSignBlock(standing.get().type(), copy(Blocks.CHERRY_HANGING_SIGN, timber)));
        var wallHanging = BLOCKS.register(name + "_wall_hanging_sign",
            () -> new WallHangingSignBlock(standing.get().type(), copy(Blocks.CHERRY_WALL_HANGING_SIGN, timber).lootFrom(hanging)));
        var signItem = ITEMS.register(name + "_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), standing.get(), wall.get()));
        var hangingItem = ITEMS.register(name + "_hanging_sign",
            () -> new HangingSignItem(hanging.get(), wallHanging.get(), new Item.Properties().stacksTo(16)));
        return new WoodSigns(standing, wall, hanging, wallHanging, signItem, hangingItem);
    }

    private static RotatedPillarBlock log(Block source, MapColor end, MapColor side) {
        return new RotatedPillarBlock(Properties.ofFullCopy(source)
            .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? end : side));
    }

    private static Properties copy(Block source, MapColor color) {
        return Properties.ofFullCopy(source).mapColor(color);
    }

    private static DeferredItem<BlockItem> workstation(DeferredBlock<?> block) {
        return ITEMS.register(block.getId().getPath(),
            () -> new HintedBlockItem(block.get(), new Item.Properties()));
    }

    public record CherryWood(List<DeferredBlock<? extends Block>> woodShapes,
                             DeferredBlock<Block> planks,
                             DeferredBlock<BlossomLeavesBlock> leaves,
                             DeferredBlock<SaplingBlock> sapling,
                             DeferredBlock<FlowerPotBlock> potted,
                             DeferredBlock<PinkPetalsBlock> petals,
                             DeferredHolder<ParticleType<?>, SimpleParticleType> fallingPetals,
                             WoodSigns signs,
                             WoodBoats boats,
                             DeferredBlock<CherryChestBlock> chest,
                             List<DeferredItem<? extends Item>> items) {}

    public record WoodSigns(DeferredBlock<StandingSignBlock> standing, DeferredBlock<WallSignBlock> wall,
                            DeferredBlock<CeilingHangingSignBlock> hanging, DeferredBlock<WallHangingSignBlock> wallHanging,
                            DeferredItem<SignItem> signItem, DeferredItem<HangingSignItem> hangingItem) {}

    public record WoodBoats(EnumProxy<Boat.Type> type, DeferredItem<BoatItem> boat, DeferredItem<BoatItem> chestBoat) {}
}
