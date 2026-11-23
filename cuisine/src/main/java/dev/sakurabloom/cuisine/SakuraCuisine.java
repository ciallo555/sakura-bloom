package dev.sakurabloom.cuisine;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SakuraCuisine.ID)
public final class SakuraCuisine {
    public static final String ID = "sakura_bloom_cuisine";

    public SakuraCuisine(IEventBus bus) {
        KitchenContent.register(bus);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            NeoForge.EVENT_BUS.addListener(KaleidoscopeServing::serve);
        }
    }
}
