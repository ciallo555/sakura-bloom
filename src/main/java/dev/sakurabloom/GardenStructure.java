package dev.sakurabloom;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/** Use the vanilla rotated jigsaw piece, but only build a courtyard on a dry, reasonably even site. */
final class GardenStructure extends Structure {
    private static final MapCodec<GardenStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        JigsawStructure.CODEC.forGetter(site -> site.layout),
        Codec.intRange(1, 32).fieldOf("max_surface_relief").forGetter(site -> site.maxRelief),
        Codec.intRange(0, 48).optionalFieldOf("entrance_center_x", 15).forGetter(site -> site.entranceCenter),
        Codec.intRange(1, 17).optionalFieldOf("entrance_width", 11).forGetter(site -> site.entranceWidth),
        BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("entrance_paving", Blocks.SMOOTH_STONE).forGetter(site -> site.paving),
        ResourceLocation.CODEC.optionalFieldOf("clearance_template").forGetter(site -> site.clearanceTemplate)
    ).apply(instance, GardenStructure::new));
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SakuraBloom.ID);
    private static final Supplier<StructureType<GardenStructure>> TYPE = TYPES.register("garden_site", () -> () -> CODEC);
    private final JigsawStructure layout;
    private final int maxRelief;
    private final int entranceCenter;
    private final int entranceWidth;
    private final Block paving;
    private final Optional<ResourceLocation> clearanceTemplate;
    private static final int COURT_Y = 2;

    private GardenStructure(JigsawStructure layout, int maxRelief, int center, int width, Block paving, Optional<ResourceLocation> clearanceTemplate) {
        super(layout.getModifiedStructureSettings());
        this.layout = layout;
        this.maxRelief = maxRelief;
        this.entranceCenter = center;
        this.entranceWidth = width;
        this.paving = paving;
        this.clearanceTemplate = clearanceTemplate;
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
        GardenClearance.register(bus);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return layout.findValidGenerationPoint(context).flatMap(start -> {
            var pieces = start.getPiecesBuilder();
            if (pieces.isEmpty() || !(pieces.build().pieces().getFirst() instanceof PoolElementStructurePiece first))
                return Optional.empty();
            // Heightmaps return the first free block. Anchor the garden surface to the
            // soil block outside the rotated gate, even when the hall is farther uphill.
            var heights = new int[3];
            int spread = Math.max(1, entranceWidth / 3);
            for (int i = 0; i < heights.length; i++) {
                var foot = local(first, entranceCenter + (i - 1) * spread, 0, -1);
                int dry = height(context, foot, Heightmap.Types.OCEAN_FLOOR_WG);
                if (dry != height(context, foot, Heightmap.Types.WORLD_SURFACE_WG)) return Optional.empty();
                heights[i] = dry - 1;
            }
            int middle = heights[1];
            java.util.Arrays.sort(heights);
            int yard = heights[1];
            if (heights[2] - heights[0] > 2 || Math.abs(middle - yard) > 1 || !suitableGround(context, pieces.getBoundingBox(), yard))
                return Optional.empty();
            int offset = yard - COURT_Y - first.getPosition().getY();
            pieces.offsetPiecesVertically(offset);
            return Optional.of(new GenerationStub(start.position().offset(0, offset, 0), Either.right(pieces)));
        });
    }

    private static BlockPos local(PoolElementStructurePiece piece, int x, int y, int z) {
        return new BlockPos(x, y, z).rotate(piece.getRotation()).offset(piece.getPosition());
    }

    private static int height(GenerationContext context, BlockPos pos, Heightmap.Types type) {
        return context.chunkGenerator().getBaseHeight(pos.getX(), pos.getZ(), type, context.heightAccessor(), context.randomState());
    }

    private boolean suitableGround(GenerationContext context, BoundingBox plot, int yard) {
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
                if (surface > ground || high-low > maxRelief || yard - (ground-1) > 2 || (ground-1) - yard > maxRelief-2)
                    return false;
                if (context.validBiome().test(biomes.getBiome(new BlockPos(x, surface, z)))) matching++;
                else if (x == center.getX() && z == center.getZ()) return false;
            }
        }
        return matching >= 5;
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunkBounds, ChunkPos chunk, PiecesContainer pieces) {
        if (pieces.pieces().isEmpty() || !(pieces.pieces().getFirst() instanceof PoolElementStructurePiece first)) return;
        clearanceTemplate.ifPresent(template -> GardenClearance.remember(level, chunk, template, first));
        int yard = first.getPosition().getY() + COURT_Y;
        var randomState = level.getLevel().getChunkSource().randomState();
        for (int i = 0; i < entranceWidth; i++) {
            int x = entranceCenter - entranceWidth / 2 + i;
            var outside = local(first, x, 0, -1);
            var toe = local(first, x, 1, 0);
            var landing = local(first, x, 2, 1);
            if (!chunkBounds.isInside(toe) && !chunkBounds.isInside(landing)) continue;
            int soil = generator.getBaseHeight(outside.getX(), outside.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1;
            if (soil < yard - 1) continue; // Both original treads are needed on this slope.
            for (var pos : new BlockPos[]{toe, landing}) {
                if (!chunkBounds.isInside(pos)) continue;
                var stair = level.getBlockState(pos);
                if (!(stair.getBlock() instanceof StairBlock)) continue;
                var surface = entranceWidth > 5 && Math.abs(x - entranceCenter) > 2
                    ? level.getBlockState(pos.below()) : paving.defaultBlockState();
                if (pos.equals(toe)) {
                    level.setBlock(pos, level.getBlockState(pos.below()), Block.UPDATE_CLIENTS);
                    level.setBlock(pos.above(), soil < yard ? stair : surface, Block.UPDATE_CLIENTS);
                } else {
                    level.setBlock(pos, surface, Block.UPDATE_CLIENTS);
                }
            }
        }
        // Close small undercuts beneath the occupied foundation only, within this chunk.
        var plot = first.getBoundingBox();
        for (int x = Math.max(plot.minX(), chunkBounds.minX()); x <= Math.min(plot.maxX(), chunkBounds.maxX()); x++) {
            for (int z = Math.max(plot.minZ(), chunkBounds.minZ()); z <= Math.min(plot.maxZ(), chunkBounds.maxZ()); z++) {
                var foot = new BlockPos(x, yard - COURT_Y, z);
                if (!level.getBlockState(foot).is(Blocks.DIRT)) continue;
                for (int depth = 1; depth <= 3; depth++) {
                    var below = foot.below(depth);
                    if (!level.getBlockState(below).canBeReplaced()) break;
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }
}
