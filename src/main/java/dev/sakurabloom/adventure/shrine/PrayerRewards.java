package dev.sakurabloom.adventure.shrine;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/** A JEI preview of the server's actual one-roll, weighted offering table. */
public record PrayerRewards(List<Prize> prizes) implements CustomPacketPayload {
    public record Prize(Item item, int count, int weight) {
        static final StreamCodec<RegistryFriendlyByteBuf, Prize> CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), Prize::item,
            ByteBufCodecs.VAR_INT, Prize::count, ByteBufCodecs.VAR_INT, Prize::weight, Prize::new);

        public ItemStack stack() { return new ItemStack(item, count); }
    }

    public static final Type<PrayerRewards> TYPE = new Type<>(SakuraBloom.id("prayer_rewards"));
    private static final StreamCodec<RegistryFriendlyByteBuf, PrayerRewards> CODEC =
        Prize.CODEC.apply(ByteBufCodecs.list()).map(PrayerRewards::new, PrayerRewards::prizes);

    @Override public Type<PrayerRewards> type() { return TYPE; }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> PrayerPreviewClient.receive(payload));
    }

    static void sync(OnDatapackSyncEvent event) {
        var catalog = from(event.getPlayerList().getServer());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player,catalog));
    }

    static PrayerRewards from(MinecraftServer server) {
        LootTable table = server.reloadableRegistries().getLootTable(OfferingBox.REWARD);
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = LootTable.DIRECT_CODEC.encodeStart(ops, table).result();
        return new PrayerRewards(json.map(j -> parse(j.getAsJsonObject())).orElse(List.of()));
    }

    private static boolean nonEmpty(JsonObject object, String array) {
        return object.has(array) && !object.getAsJsonArray(array).isEmpty();
    }

    private static int constant(JsonObject object, String field, int fallback) {
        if (!object.has(field)) return fallback;
        var value = object.get(field);
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : -1;
    }

    // Dynamic loot conditions depend on the player and world. Hide the numeric preview
    // for such overrides instead of advertising stale default odds.
    private static List<Prize> parse(JsonObject table) {
        if (!table.has("pools") || table.getAsJsonArray("pools").size()!=1 || nonEmpty(table,"functions")) return List.of();
        var pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
        if (constant(pool,"rolls",0)!=1 || constant(pool,"bonus_rolls",0)!=0
            || nonEmpty(pool,"conditions") || nonEmpty(pool,"functions")) return List.of();
        var result = new ArrayList<Prize>();
        for (var element : pool.getAsJsonArray("entries")) {
            var entry = element.getAsJsonObject();
            if (!entry.get("type").getAsString().equals("minecraft:item") || nonEmpty(entry,"conditions")
                || constant(entry,"quality",0)!=0) return List.of();
            int weight = constant(entry,"weight",1);
            int count = 1;
            if (nonEmpty(entry,"functions")) {
                var functions = entry.getAsJsonArray("functions");
                if (functions.size()!=1) return List.of();
                var function = functions.get(0).getAsJsonObject();
                if (!function.get("function").getAsString().equals("minecraft:set_count") || nonEmpty(function,"conditions")
                    || function.has("add") && function.get("add").getAsBoolean()) return List.of();
                count = constant(function,"count",-1);
            }
            if (count < 1 || weight < 1) return List.of();
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.get("name").getAsString()));
            result.add(new Prize(item,count,weight));
        }
        return List.copyOf(result);
    }
}
