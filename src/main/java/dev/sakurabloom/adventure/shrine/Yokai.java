package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class Yokai extends Monster {
    private static final TagKey<Structure> SHRINES = TagKey.create(Registries.STRUCTURE, SakuraBloom.id("shrines"));
    private static final TagKey<Biome> GROVES = TagKey.create(Registries.BIOME, SakuraBloom.id("yokai_groves"));
    public static final int WARD_RADIUS = 64;
    public Yokai(EntityType<? extends Yokai> type, Level level) { super(type, level); xpReward = 5; }
    static AttributeSupplier.Builder attributes(double health, double speed, double attack) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ATTACK_DAMAGE, attack).add(Attributes.FOLLOW_RANGE, 24);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new LeapAtTargetGoal(this,.32F));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this, .85));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    static boolean canSpawn(EntityType<Yokai> type, ServerLevelAccessor access, MobSpawnType reason, BlockPos pos, RandomSource rng) {
        var level = access.getLevel();
        if (level.dimension() != Level.OVERWORLD)
            return Monster.checkAnyLightMonsterSpawnRules(type,access,reason,pos,rng);
        // Patrol the forest floor under the canopy as well as open meadows. A horizontal cherry branch
        // can be several blocks above valid grass, so a heightmap test alone would reject that ground.
        if (!ShrineAdventure.isNight(level) || level.getDifficulty() == Difficulty.PEACEFUL ||
            !access.getBiome(pos).is(GROVES) || access.getBrightness(LightLayer.BLOCK, pos) > 7 ||
            !Mob.checkMobSpawnRules(type,access,reason,pos,rng)) return false;
        int surface = access.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ());
        if (pos.getY() < surface - 1) {
            if (surface - pos.getY() > 16) return false;
            var cursor = pos.mutable();
            while (cursor.getY() < surface) {
                var above = access.getBlockState(cursor);
                if (!above.isAir() && !above.is(net.minecraft.tags.BlockTags.LOGS) &&
                    !above.is(net.minecraft.tags.BlockTags.LEAVES) && !above.getCollisionShape(access,cursor).isEmpty()) return false;
                cursor.move(Direction.UP);
            }
        }
        return !nearShrine(level,pos);
    }

    // A referenced chunk intersects the shrine footprint. Only consult chunks already loaded
    // for natural spawning, avoiding structure searches or generation on the server tick.
    static boolean nearShrine(ServerLevel level, BlockPos pos) {
        int x = pos.getX(), z = pos.getZ();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (int cx=(x-WARD_RADIUS)>>4; cx <= (x+WARD_RADIUS)>>4; cx++) {
            for (int cz=(z-WARD_RADIUS)>>4; cz <= (z+WARD_RADIUS)>>4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx,cz);
                if (chunk == null) continue;
                int dx = Math.max(0, Math.max(cx*16-x,x-(cx*16+15)));
                int dz = Math.max(0, Math.max(cz*16-z,z-(cz*16+15)));
                if (dx*dx+dz*dz > WARD_RADIUS*WARD_RADIUS) continue;
                for (var ref : chunk.getAllReferences().entrySet()) {
                    if (!structures.wrapAsHolder(ref.getKey()).is(SHRINES)) continue;
                    for (long key : ref.getValue()) {
                        var startChunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
                        if (startChunk == null) return true;
                        var start = startChunk.getStartForStructure(ref.getKey());
                        if (start == null || !start.isValid()) continue;
                        var box = start.getBoundingBox();
                        dx = Math.max(0,Math.max(box.minX()-x,x-box.maxX()));
                        dz = Math.max(0,Math.max(box.minZ()-z,z-box.maxZ()));
                        if (dx*dx+dz*dz <= WARD_RADIUS*WARD_RADIUS) return true;
                    }
                }
            }
        }
        return false;
    }
}
