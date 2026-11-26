package dev.sakurabloom.adventure.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

final class MapleFan extends Item {
    private static final String RETURN = "sakura_bloom:fan_return";
    private static final BlockPos ENTRANCE = new BlockPos(47, QuarterFeature.FLOOR_Y + 5, 47);

    MapleFan(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.sakura_bloom.kamikakushi.hint").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer traveller)) return InteractionResultHolder.sidedSuccess(stack, true);
        DimensionTransition target;
        if (level.dimension().equals(Interstice.LEVEL)) {
            target = departure(traveller);
        } else {
            var realm = traveller.server.getLevel(Interstice.LEVEL);
            var feet = realm == null ? null : safePosition(realm, ENTRANCE);
            if (feet == null) {
                player.displayClientMessage(Component.translatable("message.sakura_bloom.fan_no_landing"), true);
                return InteractionResultHolder.fail(stack);
            }
            target = transition(realm, feet, player.getYRot());
        }
        CompoundTag origin = new CompoundTag();
        origin.putString("dimension", level.dimension().location().toString());
        origin.putLong("position", player.blockPosition().asLong());
        origin.putFloat("yaw", player.getYRot());
        traveller.stopRiding();
        if (traveller.changeDimension(target) == null) return InteractionResultHolder.fail(stack);
        if (target.newLevel().dimension().equals(Interstice.LEVEL)) {
            traveller.getPersistentData().put(RETURN, origin);
        } else {
            traveller.getPersistentData().remove(RETURN);
        }
        stopMomentum(traveller);
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static DimensionTransition departure(ServerPlayer player) {
        var data = player.getPersistentData().getCompound(RETURN);
        var id = ResourceLocation.tryParse(data.getString("dimension"));
        if (id != null && data.contains("position")) {
            var level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null && !level.dimension().equals(Interstice.LEVEL)) {
                var feet = safePosition(level, BlockPos.of(data.getLong("position")));
                if (feet != null) return transition(level, feet, data.getFloat("yaw"));
            }
        }
        return respawn(player);
    }

    private static Vec3 safePosition(ServerLevel level, BlockPos target) {
        for (var candidate : BlockPos.withinManhattan(target, 5, 3, 5)) {
            if (level.isOutsideBuildHeight(candidate) || !level.getWorldBorder().isWithinBounds(candidate)) continue;
            Vec3 feet = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, candidate, true);
            if (feet != null) return feet;
        }
        return null;
    }

    private static DimensionTransition transition(ServerLevel level, Vec3 feet, float yaw) {
        return new DimensionTransition(level, feet, Vec3.ZERO, yaw, 0, DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    private static void stopMomentum(ServerPlayer player) {
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static DimensionTransition respawn(ServerPlayer player) {
        return player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.PLACE_PORTAL_TICKET);
    }

    static void returnFromVoid(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(Interstice.LEVEL)
            && player.getY() < player.level().getMinBuildHeight() + 8) {
            player.stopRiding();
            if (player.changeDimension(respawn(player)) != null) {
                stopMomentum(player);
                player.getPersistentData().remove(RETURN);
            }
        }
    }
}
