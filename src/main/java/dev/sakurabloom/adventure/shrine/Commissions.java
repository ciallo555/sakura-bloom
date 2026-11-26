package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

final class Commissions {
    private record Task(String id, Item supply, int count) {}
    private static final Task[] TASKS = {
        new Task("paper", Items.PAPER, 12), new Task("watch", null, 3),
        new Task("ink", Items.INK_SAC, 4), new Task("crystal", Items.AMETHYST_SHARD, 3)
    };
    static CompoundTag journal(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        CompoundTag persistent = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains("SakuraShrine")) persistent.put("SakuraShrine",new CompoundTag());
        return persistent.getCompound("SakuraShrine");
    }
    static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
    private static Task current(CompoundTag tag) { return TASKS[Math.floorMod(tag.getInt("Completed"), TASKS.length)]; }
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Yokai) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var data = journal(player);
        Task task = current(data);
        if (data.getBoolean("Active") && task.supply == null && data.getInt("Hunts") < task.count) {
            int kills = data.getInt("Hunts")+1;
            data.putInt("Hunts",kills);
            player.displayClientMessage(Component.translatable("shrine.sakura_bloom.hunts", kills, task.count),true);
        }
    }
    static void talk(ServerPlayer player) {
        var data = journal(player);
        Task task = current(data);
        var description = Component.translatable("shrine.sakura_bloom.task."+task.id);
        if (!data.getBoolean("Active")) {
            if (!data.getBoolean("Offered")) {
                data.putBoolean("Offered",true);
                player.sendSystemMessage(Component.translatable("shrine.sakura_bloom.offer",description));
            } else {
                data.putBoolean("Active",true);
                data.putInt("Hunts",0);
                player.sendSystemMessage(Component.translatable("shrine.sakura_bloom.accept",description));
            }
            return;
        }
        var inventory = player.getInventory().items;
        int count = task.supply == null ? data.getInt("Hunts") : inventory.stream().filter(stack -> stack.is(task.supply)).mapToInt(ItemStack::getCount).sum();
        if (count < task.count) {
            player.sendSystemMessage(Component.translatable("shrine.sakura_bloom.progress",description,count,task.count));
            return;
        }
        if (task.supply != null) {
            int needed = task.count;
            for (ItemStack stack : inventory) {
                if (!stack.is(task.supply)) continue;
                int take = Math.min(needed,stack.getCount());
                stack.shrink(take);
                needed -= take;
                if (needed == 0) break;
            }
            player.getInventory().setChanged();
        }
        int completed = data.getInt("Completed")+1;
        data.putInt("Completed",completed);
        data.putBoolean("Active",false);
        data.putBoolean("Offered",false);
        data.putInt("Hunts",0);
        give(player,new ItemStack(ShrineAdventure.TALISMAN.get(),4));
        if (completed % TASKS.length == 0) {
            give(player,new ItemStack(BuiltInRegistries.ITEM.get(SakuraBloom.id("kamikakushi"))));
            player.sendSystemMessage(Component.translatable("shrine.sakura_bloom.fan"));
        } else player.sendSystemMessage(Component.translatable("shrine.sakura_bloom.thanks",completed % TASKS.length,TASKS.length));
    }
}
