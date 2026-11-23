package dev.sakurabloom;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/** Use the vanilla rotated jigsaw piece, but only build a courtyard on a dry, reasonably even site. */
final class GardenStructure extends Structure {
    private static final MapCodec<GardenStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        JigsawStructure.CODEC.forGetter(site -> site.layout),
        Codec.intRange(1, 32).fieldOf("max_surface_relief").forGetter(site -> site.maxRelief)
    ).apply(instance, GardenStructure::new));
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SakuraBloom.ID);
    private static final Supplier<StructureType<GardenStructure>> TYPE = TYPES.register("garden_site", () -> () -> CODEC);
    private final JigsawStructure layout;
    private final int maxRelief;

    private GardenStructure(JigsawStructure layout, int maxRelief) {
        super(layout.getModifiedStructureSettings());
        this.layout = layout;
        this.maxRelief = maxRelief;
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return layout.findValidGenerationPoint(context).flatMap(start -> {
            var pieces = start.getPiecesBuilder();
            if (pieces.isEmpty() || !suitableGround(context, pieces.getBoundingBox())) return Optional.empty();
            return Optional.of(new GenerationStub(start.position(), Either.right(pieces)));
        });
    }

    private boolean suitableGround(GenerationContext context, BoundingBox plot) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        var center = plot.getCenter();
        var biomes = new BiomeManager((x, y, z) -> context.biomeSource().getNoiseBiome(x, y, z, context.randomState().sampler()),
            BiomeManager.obfuscateSeed(context.seed()));
        int matching = 0;
        // Sample inside the rounded garden edge, in world coordinates after vanilla chose its rotation.
        for (int x : new int[]{plot.minX()+3, center.getX(), plot.maxX()-3}) {
            for (int z : new int[]{plot.minZ()+3, center.getZ(), plot.maxZ()-3}) {
                var generator = context.chunkGenerator();
                int ground = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                low = Math.min(low, ground);
                high = Math.max(high, ground);
                if (surface > ground || high-low > maxRelief) return false;
                if (context.validBiome().test(biomes.getBiome(new BlockPos(x, surface, z)))) matching++;
                else if (x == center.getX() && z == center.getZ()) return false;
            }
        }
        return matching >= 5;
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }
}
