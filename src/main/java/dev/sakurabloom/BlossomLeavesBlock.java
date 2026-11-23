package dev.sakurabloom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlossomLeavesBlock extends LeavesBlock {
    public static final MapCodec<BlossomLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        propertiesCodec(),
        BuiltInRegistries.PARTICLE_TYPE.holderByNameCodec().fieldOf("petal").forGetter(block -> block.petal)
    ).apply(instance, BlossomLeavesBlock::new));

    private final Holder<ParticleType<?>> petal;

    public BlossomLeavesBlock(Properties properties, Holder<ParticleType<?>> petal) {
        super(properties);
        this.petal = petal;
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(10) == 0 && petal.value() instanceof SimpleParticleType particle) {
            BlockPos below = pos.below();
            if (!isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) {
                ParticleUtils.spawnParticleBelow(level, pos, random, particle);
            }
        }
    }
}
