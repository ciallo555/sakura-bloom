package dev.sakurabloom.adventure.shrine;

import dev.sakurabloom.SakuraBloom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = SakuraBloom.ID, value = Dist.CLIENT)
public final class PrayerPreviewClient {
    private static List<PrayerRewards.Prize> current = List.of();
    private static Consumer<List<PrayerRewards.Prize>> listener;
    private PrayerPreviewClient() {}

    public static List<PrayerRewards.Prize> current() { return current; }
    public static void setListener(Consumer<List<PrayerRewards.Prize>> onChange) { listener=onChange; }

    static void receive(PrayerRewards payload) {
        if (current.equals(payload.prizes())) return;
        current = List.copyOf(payload.prizes());
        if (listener!=null) listener.accept(current);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        receive(new PrayerRewards(List.of()));
    }
}
