package dev.sakurabloom.adventure.realm;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class Interstice {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, SakuraBloom.id("interstice"));
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SakuraBloom.ID);
    public static final Supplier<Item> FAN = ITEMS.register("kamikakushi",
        () -> new MapleFan(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, SakuraBloom.ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SakuraBloom.ID);
    static final Supplier<StructureType<SanctumStructure>> SANCTUM = STRUCTURES.register("interstice_sanctum", () -> () -> SanctumStructure.CODEC);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SakuraBloom.ID);
    public static final Supplier<EntityType<Allay>> WISP = ENTITIES.register("lantern_wisp",
        () -> EntityType.Builder.of(Allay::new, MobCategory.CREATURE).sized(.35F, .6F)
            .clientTrackingRange(8).updateInterval(2).build("sakura_bloom:lantern_wisp"));

    static {
        FEATURES.register("interstice_quarter", () -> new QuarterFeature(NoneFeatureConfiguration.CODEC));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(WISP.get(), Allay.createAttributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(WISP.get(), SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        ITEMS.register(bus);
        FEATURES.register(bus);
        STRUCTURES.register(bus);
        NeoForge.EVENT_BUS.addListener(MapleFan::returnFromVoid);
    }
}
