package dev.sakurabloom.adventure.realm;

import com.mojang.serialization.Codec;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

// Each 32-block quarter is shared by four chunks. Each decoration pass writes its own
// quadrant only. Seed, rotation and biome are sampled at the same point by all four.
final class QuarterFeature extends Feature<NoneFeatureConfiguration> {
    static final int FLOOR_Y = 52;

    QuarterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        int x = Math.floorDiv(chunk.x, 2) * 32;
        int z = Math.floorDiv(chunk.z, 2) * 32;
        var source = level.getLevel().getChunkSource();
        var biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x + 16),
            QuartPos.fromBlock(FLOOR_Y), QuartPos.fromBlock(z + 16), source.randomState().sampler());
        String theme = switch (biome.unwrapKey().orElseThrow().location().getPath()) {
            case "lantern_gallery" -> "lantern";
            case "moonlit_harbor" -> "moonlit";
            case "vermilion_bridges" -> "vermilion";
            case "ashen_lofts" -> "ashen";
            default -> "";
        };
        if (theme.isEmpty()) return false;

        RandomSource random = RandomSource.create(level.getSeed() ^ Mth.getSeed(x, FLOOR_Y, z));
        Rotation rotation = Rotation.getRandom(random);
        var id = SakuraBloom.id("interstice/" + theme + "_quarter_" + random.nextInt(2));
        var template = level.getLevel().getStructureManager().getOrCreate(id);
        var origin = template.getZeroPositionWithTransform(new BlockPos(x, FLOOR_Y, z), Mirror.NONE, rotation);
        var bounds = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        var settings = new StructurePlaceSettings().setRotation(rotation).setBoundingBox(bounds)
            .setIgnoreEntities(true).setKnownShape(true);
        return template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS);
    }
}
