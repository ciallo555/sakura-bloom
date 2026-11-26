package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ShrineAdventure {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SakuraBloom.ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SakuraBloom.ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SakuraBloom.ID);
    public static final Supplier<Item> TALISMAN = ITEMS.registerSimpleItem("sakura_talisman");
    public static final Supplier<OfferingBox> BOX = BLOCKS.register("offering_box",
        () -> new OfferingBox(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS).noOcclusion()));
    public static final Supplier<BlockItem> BOX_ITEM = ITEMS.register("offering_box", () -> new BlockItem(BOX.get(), new Item.Properties()));
    public static final Supplier<EntityType<Yokai>> FOX = beast("dusk_fox", .65F, .7F);
    public static final Supplier<EntityType<Yokai>> WOLF = beast("ink_wolf", .75F, .85F);
    public static final Supplier<EntityType<Yokai>> HARE = beast("gloom_hare", .50F, .68F);
    public static final Supplier<EntityType<ShrineMaiden>> MAIDEN = ENTITIES.register("shrine_maiden", () ->
        EntityType.Builder.of(ShrineMaiden::new, MobCategory.CREATURE).sized(.6F, 1.8F).clientTrackingRange(10).build("sakura_bloom:shrine_maiden"));
    private static Supplier<EntityType<Yokai>> beast(String name, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(Yokai::new, MobCategory.MONSTER)
            .sized(width,height).clientTrackingRange(8).build("sakura_bloom:"+name));
    }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ShrineAdventure::attributes);
        bus.addListener(ShrineAdventure::placements);
        bus.addListener(ShrineAdventure::creative);
        NeoForge.EVENT_BUS.addListener(Commissions::onDeath);
    }
    public static void display(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(TALISMAN.get());
        output.accept(BOX_ITEM.get());
    }
    static boolean isNight(net.minecraft.world.level.Level level) {
        long clock = Math.floorMod(level.dimensionType().fixedTime().orElse(level.getDayTime()), 24000L);
        return clock >= 13000 && clock < 23000;
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(FOX.get(), Yokai.attributes(16, .32, 4).build());
        event.put(WOLF.get(), Yokai.attributes(24, .29, 5).build());
        event.put(HARE.get(), Yokai.attributes(12, .36, 3).build());
        event.put(MAIDEN.get(), Villager.createAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED,.45).build());
    }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(MAIDEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            net.minecraft.world.entity.Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var type : java.util.List.of(FOX.get(), WOLF.get(), HARE.get()))
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Yokai::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(TALISMAN.get());
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BOX_ITEM.get());
    }
}
