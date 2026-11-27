package dev.sakurabloom.adventure.combat;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SpiritCombat {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SakuraBloom.ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SakuraBloom.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SakuraBloom.ID);
    static final ResourceKey<Level> REALM = ResourceKey.create(Registries.DIMENSION, SakuraBloom.id("interstice"));
    public static final DeferredHolder<EntityType<?>, EntityType<VeilGuardian>> SENTINEL = makeGuardian("veil_sentinel", null, false);

    static DeferredHolder<Block, Block> makeAltar(String id) {
        var block = BLOCKS.register(id + "_altar", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE)
            .strength(-1.0F, 3600000.0F).lightLevel(state -> 7).noLootTable()));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    static DeferredHolder<EntityType<?>, EntityType<VeilGuardian>> makeGuardian(String id, Sanctum theme, boolean boss) {
        return ENTITIES.register(id, () -> EntityType.Builder.<VeilGuardian>of(
            (type, level) -> new VeilGuardian(type, level, theme), MobCategory.MONSTER)
            .sized(boss ? 1.25F : 0.85F, boss ? 2.9F : 2.1F).clientTrackingRange(10).build(id));
    }

    public static void register(IEventBus bus) {
        Sanctum.values();
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(SpiritCombat::attributes);
        NeoForge.EVENT_BUS.addListener(SpiritCombat::drops);
    }

    public static void display(CreativeModeTab.Output output) {
        ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SENTINEL.get(), VeilGuardian.attributes(false).build());
        for (var theme : Sanctum.values()) event.put(theme.guardian.get(), VeilGuardian.attributes(true).build());
    }

    private static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (!(entity instanceof Monster) || !(event.getSource().getEntity() instanceof Player)
            || !(entity.level() instanceof ServerLevel level) || !level.dimension().equals(REALM)
            || !EntityType.getKey(entity.getType()).getNamespace().equals(SakuraBloom.ID)) return;
        var theme = Sanctum.at(level, entity.blockPosition());
        if (theme != null) {
            // Foxes carry amber from the lantern market into neighbouring hunting grounds.
            // Players can gather its seal material without harming friendly inhabitants.
            if (theme != Sanctum.LANTERN && entity.getType() == dev.sakurabloom.adventure.shrine.ShrineAdventure.FOX.get())
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level,entity.getX(),entity.getY(),entity.getZ(),
                    new ItemStack(Sanctum.LANTERN.shard.get())));
            var stack = new ItemStack(theme.shard.get(), entity instanceof VeilGuardian ? 2 : 1);
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack));
        }
    }
}
