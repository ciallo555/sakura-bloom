package dev.sakurabloom.adventure.combat;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import dev.sakurabloom.BloomContent;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import java.util.List;

final class RitualSeal extends Item {
    private final Sanctum theme;
    RitualSeal(Sanctum theme) {
        super(new Properties().stacksTo(16));
        this.theme = theme;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("ritual.sakura_bloom.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        var pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).is(theme.altar.get())) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        var structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SakuraBloom.id(theme.id + "_sanctum"));
        String failure = null;
        if (!level.dimension().equals(SpiritCombat.REALM) || structure == null || !server.structureManager().getStructureWithPieceAt(pos, structure).isValid())
            failure = "location";
        else if (level.getDifficulty() == Difficulty.PEACEFUL) failure = "peaceful";
        else if (!server.getEntitiesOfClass(VeilGuardian.class, new AABB(pos).inflate(32), g -> g.theme != null && g.isAlive()).isEmpty()) failure = "occupied";
        else for (Direction dir : Direction.Plane.HORIZONTAL) {
            var state = level.getBlockState(pos.relative(dir, 2));
            if (!state.is(BloomContent.LANTERN.get()) || !state.getValue(BlockStateProperties.LIT)) { failure = "lamps"; break; }
        }
        if (failure != null) {
            if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.translatable("ritual.sakura_bloom." + failure), true);
            return InteractionResult.FAIL;
        }
        var boss = theme.guardian.get().create(server);
        if (boss == null) return InteractionResult.FAIL;
        boss.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, 0, 0);
        if (!server.noCollision(boss) || !server.addFreshEntity(boss)) return InteractionResult.FAIL;
        boss.bindAltar(pos);
        boss.setTarget(ctx.getPlayer());
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            var lamp = pos.relative(dir, 2);
            server.setBlockAndUpdate(lamp, server.getBlockState(lamp).setValue(BlockStateProperties.LIT,false));
        }
        server.sendParticles(ParticleTypes.CHERRY_LEAVES, boss.getX(), boss.getY()+2, boss.getZ(), 45, 1, 1.1, 1, 0.07);
        server.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.2F, 0.75F);
        return InteractionResult.CONSUME;
    }
}
