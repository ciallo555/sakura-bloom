package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.Level;
import java.util.EnumSet;

public final class ShrineMaiden extends AbstractVillager {
    private BlockPos sanctuary;
    private float prayerYaw;
    private long restocked = -1;
    public ShrineMaiden(EntityType<? extends ShrineMaiden> type, Level level) { super(type,level); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.2));
        goalSelector.addGoal(2,new TradeWithPlayerGoal(this));
        goalSelector.addGoal(2,new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(3,new NightPrayer());
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,.6));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.5));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,7));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override protected void customServerAiStep() {
        if (sanctuary == null) {
            sanctuary = blockPosition().immutable();
            prayerYaw = getYRot();
            restrictTo(sanctuary, 12);
        }
        long day = Math.floorDiv(level().getDayTime(),24000L);
        if (day != restocked && !isTrading()) {
            if (offers != null) offers.forEach(MerchantOffer::resetUses);
            restocked = day;
        }
        super.customServerAiStep();
    }
    private final class NightPrayer extends Goal {
        private int ticks;
        NightPrayer() { setFlags(EnumSet.of(Flag.LOOK,Flag.MOVE)); }
        @Override public boolean canUse() { return sanctuary != null && ShrineAdventure.isNight(level()) && !isTrading(); }
        @Override public void start() { ticks = 0; }
        @Override public void tick() {
            if (distanceToSqr(sanctuary.getX()+.5,sanctuary.getY(),sanctuary.getZ()+.5) > 1.5) {
                if (ticks % 15 == 0) getNavigation().moveTo(sanctuary.getX()+.5,sanctuary.getY(),sanctuary.getZ()+.5,.75);
                setShiftKeyDown(false);
            } else {
                getNavigation().stop();
                setYRot(prayerYaw);
                setYBodyRot(prayerYaw);
                setYHeadRot(prayerYaw);
                setShiftKeyDown(true);
                if (ticks % 45 == 0 && level() instanceof ServerLevel server)
                    server.sendParticles(ParticleTypes.CHERRY_LEAVES,getX(),getY()+1.25,getZ(),1,.15,.1,.15,.01);
            }
            ticks++;
        }
        @Override public void stop() { setShiftKeyDown(false); getNavigation().stop(); }
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer visitor && !isTrading()) {
            if (visitor.isShiftKeyDown() || visitor.getItemInHand(hand).is(ShrineAdventure.TALISMAN.get())) {
                setTradingPlayer(visitor);
                openTradingScreen(visitor,getDisplayName(),1);
            } else Commissions.talk(visitor);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override protected void updateTrades() {
        Item token = ShrineAdventure.TALISMAN.get();
        trade(Items.PINK_PETALS,12,token,1,8);
        trade(Items.AMETHYST_SHARD,4,token,2,8);
        trade(token,2,Items.HONEY_BOTTLE,1,8);
        trade(token,4,Items.EXPERIENCE_BOTTLE,2,6);
        trade(token,12,BuiltInRegistries.ITEM.get(SakuraBloom.id("knotwork_pouch")),1,3);
        trade(token,20,Items.GOLDEN_APPLE,1,2);
    }
    private void trade(Item cost, int price, Item result, int count, int stock) {
        offers.add(new MerchantOffer(new ItemCost(cost,price),new ItemStack(result,count),stock,2,0));
    }
    @Override protected void rewardTradeXp(MerchantOffer offer) {
        if (level() instanceof ServerLevel level) ExperienceOrb.award(level,position(),2);
    }
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (sanctuary != null) tag.putLong("Sanctuary",sanctuary.asLong());
        tag.putFloat("PrayerYaw",prayerYaw);
        tag.putLong("Restocked",restocked);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Sanctuary")) {
            sanctuary = BlockPos.of(tag.getLong("Sanctuary"));
            restrictTo(sanctuary,12);
        }
        prayerYaw = tag.getFloat("PrayerYaw");
        restocked = tag.getLong("Restocked");
    }
}
