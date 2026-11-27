package dev.sakurabloom.adventure.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** One suspended group of buildings and the bridges shared with its neighbours. */
record FloatingWard(int cellX, int cellZ, BlockPos center) {
    static final int SIZE = 64;

    static FloatingWard containing(long seed, int x, int z) {
        return at(seed, Math.floorDiv(x, SIZE), Math.floorDiv(z, SIZE));
    }

    static FloatingWard at(long seed, int cx, int cz) {
        var rng = RandomSource.create(seed ^ Mth.getSeed(cx, 1847, cz));
        int x = cx * SIZE + 27 + rng.nextInt(11);
        int z = cz * SIZE + 27 + rng.nextInt(11);
        double phaseX = (seed & 1023) / 128.0;
        double phaseZ = (seed >>> 10 & 1023) / 128.0;
        int y = 55 + (int)Math.round(12 * Math.sin(.78*cx + .42*cz + phaseX)
            + 8 * Math.cos(.31*cx - .73*cz + phaseZ)) + rng.nextInt(5) - 2;
        return new FloatingWard(cx, cz, new BlockPos(x, y, z));
    }

    BlockPos corner() { return center.offset(-16, -4, -16); }

    FloatingWard neighbor(long seed, Direction dir) {
        return at(seed, cellX + dir.getStepX(), cellZ + dir.getStepZ());
    }

    // In each court of four islands, retain three edges of the square. For each pair
    // of adjacent courts one randomly chosen passage joins them. No island is stranded.
    boolean road(long seed, Direction positive) {
        int px = Math.floorMod(cellX,2), pz = Math.floorMod(cellZ,2);
        var random = RandomSource.create(seed ^ Mth.getSeed(Math.floorDiv(cellX,2), 6283, Math.floorDiv(cellZ,2)));
        int missing = random.nextInt(4), east = random.nextInt(2), south = random.nextInt(2);
        if (positive == Direction.EAST) return px == 0 ? missing != pz : pz == east;
        return pz == 0 ? missing != 2 + px : px == south;
    }

    List<BlockPos> spanTo(FloatingWard next, Direction along) {
        boolean east = along == Direction.EAST;
        var first = center.relative(along, 15);
        var last = next.center.relative(along.getOpposite(), 16);
        int run = east ? last.getX()-first.getX() : last.getZ()-first.getZ();
        int shift = east ? last.getZ()-first.getZ() : last.getX()-first.getX();
        int bend = run/2;
        int length = run + Math.abs(shift);
        var lateral = east ? Direction.SOUTH : Direction.EAST;
        if (shift < 0) lateral = lateral.getOpposite();
        var path = new ArrayList<BlockPos>(length+1);
        for (int i=0; i<=length; i++) {
            int side = Mth.clamp(i-bend, 0, Math.abs(shift));
            int rise = (int)Math.round((double)(last.getY()-first.getY()) * i/length);
            path.add(first.relative(along, i-side).relative(lateral,side).above(rise));
        }
        return path;
    }
}
