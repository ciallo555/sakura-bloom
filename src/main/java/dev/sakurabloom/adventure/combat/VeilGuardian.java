package dev.sakurabloom.adventure.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VeilGuardian extends Monster {
    final Sanctum theme;
    private final ServerBossEvent bar;
    private BlockPos altar;
    private int recovery = 65, charge;
    // An attack is locked to these coordinates throughout its warning.
    private Vec3 origin = Vec3.ZERO, forward = Vec3.ZERO;

    VeilGuardian(EntityType<? extends Monster> type, Level level, Sanctum theme) {
        super(type, level);
        this.theme = theme;
        bar = theme == null ? null : new ServerBossEvent(getDisplayName(), theme.color, BossEvent.BossBarOverlay.PROGRESS);
        xpReward = theme == null ? 16 : 75;
    }

    static AttributeSupplier.Builder attributes(boolean boss) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, boss ? 180 : 44)
            .add(Attributes.MOVEMENT_SPEED, boss ? .27 : .30).add(Attributes.ATTACK_DAMAGE, boss ? 8 : 5)
            .add(Attributes.ARMOR, boss ? 8 : 4).add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.KNOCKBACK_RESISTANCE, boss ? .85 : .4);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override public boolean canUse() { return charge == 0 && super.canUse(); }
            @Override public boolean canContinueToUse() { return charge == 0 && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .75));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    void bindAltar(BlockPos pos) {
        altar = pos.immutable();
        setPersistenceRequired();
        restrictTo(pos, 12);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (altar != null) tag.putLong("RitualAltar", altar.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RitualAltar")) bindAltar(BlockPos.of(tag.getLong("RitualAltar")));
        if (bar != null) bar.setName(getDisplayName());
    }
    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bar != null) bar.addPlayer(player);
    }
    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bar != null) bar.removePlayer(player);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (bar == null || !(level() instanceof ServerLevel server)) return;
        bar.setProgress(getHealth() / getMaxHealth());
        if (altar != null && (position().distanceToSqr(Vec3.atCenterOf(altar)) > 27*27 || getY() < altar.getY()-5)) {
            teleportTo(altar.getX()+.5, altar.getY()+1, altar.getZ()+.5);
            charge = 0;
            recovery = 40;
        }
        if (charge > 0) {
            getNavigation().stop();
            if (--charge % 5 == 0) drawWarning(server);
            if (charge == 0) strike(server);
            return;
        }
        var target = getTarget();
        if (target == null || !hasLineOfSight(target) || distanceToSqr(target) > 15*15 || --recovery > 0) return;
        origin = (theme == Sanctum.ASHEN || theme == Sanctum.MOONLIT) ? target.position() : position();
        forward = target.position().subtract(position()).multiply(1,0,1).normalize();
        if (forward.lengthSqr() < .01) forward = new Vec3(0,0,1);
        charge = 30;
        recovery = getHealth() < getMaxHealth()*.5 ? 50 : 85;
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, .65F);
        drawWarning(server);
    }

    boolean marked(double x, double z) {
        double radius = x*x + z*z;
        return switch (theme) {
            case LANTERN -> radius >= 3*3 && radius <= 7*7;
            case MOONLIT -> radius <= 3*3;
            case VERMILION -> {
                double along = x*forward.x + z*forward.z;
                yield along >= 0 && along <= 14 && Math.abs(x*forward.z - z*forward.x) < 1.7;
            }
            case ASHEN -> radius <= 6*6 && (Math.abs(x) <= 1 || Math.abs(z) <= 1);
        };
    }

    private void drawWarning(ServerLevel server) {
        ParticleOptions dust = switch (theme) {
            case LANTERN -> ParticleTypes.FLAME;
            case MOONLIT -> ParticleTypes.END_ROD;
            case VERMILION -> ParticleTypes.CHERRY_LEAVES;
            case ASHEN -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        int reach = theme == Sanctum.VERMILION ? 15 : 8;
        for (int x = -reach; x <= reach; x++)
            for (int z = -reach; z <= reach; z++) {
                if (marked(x,z) && (!marked(x+1,z) || !marked(x-1,z) || !marked(x,z+1) || !marked(x,z-1)))
                    server.sendParticles(dust, origin.x+x, origin.y+.18, origin.z+z, 1, .04,.04,.04, .01);
            }
    }

    private void strike(ServerLevel server) {
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2F, .7F);
        var bounds = new net.minecraft.world.phys.AABB(origin, origin).inflate(16,3,16);
        for (var player : server.getEntitiesOfClass(Player.class, bounds, p -> !p.isSpectator() && !p.isCreative())) {
            Vec3 offset = player.position().subtract(origin);
            if (!marked(offset.x,offset.z) || !hasLineOfSight(player) || !player.hurt(damageSources().mobAttack(this), 10)) continue;
            switch (theme) {
                case LANTERN -> player.igniteForSeconds(3);
                case MOONLIT -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                case VERMILION -> player.knockback(.65, -forward.x, -forward.z);
                case ASHEN -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 45));
            }
        }
    }
}
