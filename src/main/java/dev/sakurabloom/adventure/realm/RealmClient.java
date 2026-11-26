package dev.sakurabloom.adventure.realm;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SakuraBloom.ID, value = Dist.CLIENT)
public final class RealmClient {
    private static final ResourceLocation SILK = SakuraBloom.id("textures/entity/lantern_wisp.png");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Interstice.WISP.get(), ctx -> new AllayRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Allay wisp) {
                return SILK;
            }
        });
    }
}
