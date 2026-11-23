package dev.sakurabloom.cuisine;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = SakuraCuisine.ID, value = Dist.CLIENT)
public final class CuisineClient {
    private CuisineClient() {}

    @SubscribeEvent
    public static void registerBlockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(KitchenContent.COOKWARE.get(), KitchenRenderer::new);
    }

    @SubscribeEvent
    public static void registerCookwareModels(ModelEvent.RegisterAdditional event) {
        event.register(KitchenRenderer.CAMPFIRE_STAND);
    }
}
