package dev.sakurabloom.adventure.combat;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.entity.EntityType;

public enum Sanctum {
    LANTERN("lantern", "lantern_gallery", BossEvent.BossBarColor.YELLOW),
    MOONLIT("moonlit", "moonlit_harbor", BossEvent.BossBarColor.BLUE),
    VERMILION("vermilion", "vermilion_bridges", BossEvent.BossBarColor.RED),
    ASHEN("ashen", "ashen_lofts", BossEvent.BossBarColor.PURPLE);

    final String id;
    final ResourceKey<Biome> biome;
    final BossEvent.BossBarColor color;
    final DeferredItem<Item> shard, core, seal;
    final DeferredHolder<Block, Block> altar;
    final DeferredHolder<EntityType<?>, EntityType<VeilGuardian>> guardian;

    Sanctum(String id, String biome, BossEvent.BossBarColor color) {
        this.id = id;
        this.biome = ResourceKey.create(Registries.BIOME, SakuraBloom.id(biome));
        this.color = color;
        shard = SpiritCombat.ITEMS.registerSimpleItem(id + "_shard");
        core = SpiritCombat.ITEMS.registerSimpleItem(id + "_core", new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
        seal = SpiritCombat.ITEMS.register(id + "_seal", () -> new RitualSeal(this));
        altar = SpiritCombat.makeAltar(id);
        guardian = SpiritCombat.makeGuardian(id + "_guardian", this, true);
        SpiritCombat.ITEMS.register(id + "_blade", () -> new RelicBlade(this));
    }

    static Sanctum at(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (var sanctum : values()) if (biome.is(sanctum.biome)) return sanctum;
        return null;
    }
}
