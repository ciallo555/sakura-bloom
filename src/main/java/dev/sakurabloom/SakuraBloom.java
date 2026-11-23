package dev.sakurabloom;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;

import java.util.function.Consumer;

@Mod(SakuraBloom.ID)
public final class SakuraBloom {
    public static final String ID = "sakura_bloom";

    public SakuraBloom(IEventBus bus) {
        BloomContent.register(bus);
        GardenStructure.register(bus);
        GardenSpread.register(bus);
        bus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BloomContent.setupWood();
            Regions.register(new Region(id("sakura_country"), RegionType.OVERWORLD, 5) {
                @Override
                public void addBiomes(Registry<Biome> registry,
                                      Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
                    addModifiedVanillaOverworldBiomes(mapper, builder -> {
                        builder.replaceBiome(Biomes.CHERRY_GROVE, biome("yae_grove"));
                        builder.replaceBiome(Biomes.MEADOW, biome("shidare_meadow"));
                        builder.replaceBiome(Biomes.FLOWER_FOREST, biome("yamazakura_woodland"));
                    });
                }
            });
        });
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, id(name));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
