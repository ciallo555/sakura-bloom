package dev.sakurabloom.adventure.realm;

import com.mojang.serialization.Codec;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.List;

final class QuarterFeature extends Feature<NoneFeatureConfiguration> {
    QuarterFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        var level = ctx.level();
        var chunk = new ChunkPos(ctx.origin());
        var ward = FloatingWard.containing(level.getSeed(), chunk.getMinBlockX(), chunk.getMinBlockZ());
        var bounds = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
            chunk.getMaxBlockX(), level.getMaxBuildHeight()-1, chunk.getMaxBlockZ());
        var center = ward.center();
        var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()),
            QuartPos.fromBlock(center.getY()), QuartPos.fromBlock(center.getZ()), level.getLevel().getChunkSource().randomState().sampler());
        String theme = switch (biome.unwrapKey().orElseThrow().location().getPath()) {
            case "lantern_gallery" -> "lantern";
            case "moonlit_harbor" -> "moonlit";
            case "vermilion_bridges" -> "vermilion";
            case "ashen_lofts" -> "ashen";
            default -> "";
        };
        if (theme.isEmpty()) return false;
        var random = RandomSource.create(level.getSeed() ^ Mth.getSeed(center));
        var rotation = Rotation.getRandom(random);
        var id = SakuraBloom.id("interstice/" + theme + "_quarter_" + random.nextInt(2));
        var template = level.getLevel().getStructureManager().getOrCreate(id);
        var origin = template.getZeroPositionWithTransform(ward.corner(), Mirror.NONE, rotation);
        var settings = new StructurePlaceSettings().setRotation(rotation).setBoundingBox(bounds)
            .setIgnoreEntities(true).setKnownShape(true);
        if (bounds.intersects(template.getBoundingBox(settings,origin)))
            template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS);

        // The cut ends of minor roofs occupy different levels between the inhabited courts.
        // Positions are shared by all chunks in the ward, just like the main architecture.
        var debris = RandomSource.create(level.getSeed() ^ Mth.getSeed(center) ^ 0x63A17L);
        int fragments = debris.nextInt(5);
        if (fragments != 0) {
            int quadrant = debris.nextInt(4);
            fallenEave(level,ward,bounds,theme,quadrant,RandomSource.create(debris.nextLong()), false);
            if (fragments == 4) fallenEave(level,ward,bounds,theme,quadrant ^ 3,RandomSource.create(debris.nextLong()), true);
        }

        // Each chunk clips its own strip; either end of a shared bridge computes identical geometry.
        for (var dir : Direction.Plane.HORIZONTAL) {
            var neighbor = ward.neighbor(level.getSeed(), dir);
            boolean outward = dir.getAxisDirection()==Direction.AxisDirection.POSITIVE;
            var start = outward ? ward : neighbor;
            var end = outward ? neighbor : ward;
            var positive = outward ? dir : dir.getOpposite();
            var path = start.spanTo(end, positive);
            if (start.road(level.getSeed(),positive)) {
                bridge(level, bounds, path, positive);
            } else {
                // Truncated approaches leave visible broken beams above the gulf.
                for (int i=1; i<=3; i++) {
                    int index = outward ? i : path.size()-1-i;
                    var at = path.get(index).atY(center.getY());
                    for (int w=-1; w<=1; w++)
                        if (i<3 || w==1) set(level,bounds,at.relative(positive.getClockWise(),w),Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState());
                }
            }
        }
        return true;
    }

    private static void fallenEave(WorldGenLevel level, FloatingWard ward, BoundingBox bounds, String theme,
                                   int quadrant, RandomSource random, boolean high) {
        int x = ward.cellX()*FloatingWard.SIZE + ((quadrant & 1)==0 ? 1 : 46) + random.nextInt(3);
        int z = ward.cellZ()*FloatingWard.SIZE + ((quadrant & 2)==0 ? 1 : 46) + random.nextInt(3);
        int y = ward.center().getY() + (high ? 7 + random.nextInt(9) : -23 + random.nextInt(9));
        var fragment = level.getLevel().getStructureManager().getOrCreate(SakuraBloom.id("interstice/"+theme+"_fallen_eave"));
        var spin = Rotation.getRandom(random);
        var origin = fragment.getZeroPositionWithTransform(new BlockPos(x,y,z),Mirror.NONE,spin);
        var settings = new StructurePlaceSettings().setRotation(spin).setBoundingBox(bounds).setIgnoreEntities(true).setKnownShape(true);
        if (bounds.intersects(fragment.getBoundingBox(settings,origin)))
            fragment.placeInWorld(level,origin,origin,settings,random,Block.UPDATE_CLIENTS);
    }

    private static void bridge(WorldGenLevel level, BoundingBox bounds, List<BlockPos> path, Direction along) {
        var timber = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        for (int i=1; i<path.size()-1; i++) {
            var at = path.get(i);
            var prior = path.get(i-1);
            var next = path.get(i+1);
            var forward = Direction.fromDelta(next.getX()-at.getX(),0,next.getZ()-at.getZ());
            var backward = Direction.fromDelta(prior.getX()-at.getX(),0,prior.getZ()-at.getZ());
            var uphill = next.getY()>at.getY() ? forward : prior.getY()>at.getY() ? backward : null;
            boolean corner = forward != backward.getOpposite();
            // Widen landings at a turn, but keep the centreline and its headroom untouched.
            for (int w=-1; w<=1; w++) {
                var pos = at.relative(forward.getClockWise(),w);
                set(level,bounds,pos,timber);
                if (uphill != null) set(level,bounds,pos.above(),Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,uphill));
                if (corner) set(level,bounds,at.relative(backward.getClockWise(),w),timber);
            }
            if (i%6==0 && !corner && forward==along && backward==along.getOpposite()) {
                for (int side : new int[]{-2,2}) {
                    var foot = at.relative(along.getClockWise(),side);
                    for (int y=-3; y<=0; y++) set(level,bounds,foot.above(y),Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                    if (i%12==0 && side==2) set(level,bounds,foot.above(),Blocks.LANTERN.defaultBlockState());
                    else set(level,bounds,foot.above(),Blocks.DARK_OAK_FENCE.defaultBlockState());
                }
                for (int w=-1; w<=1; w++) set(level,bounds,at.relative(along.getClockWise(),w).below(),Blocks.DARK_OAK_WOOD.defaultBlockState());
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state) {
        if (bounds.isInside(pos)) level.setBlock(pos,state,Block.UPDATE_CLIENTS);
    }
}
