package dev.sakurabloom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SakuraBloom.ID, value = Dist.CLIENT)
public final class BloomClient {
    private BloomClient() {}

    @SubscribeEvent
    public static void registerSignMaterials(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BloomContent.WOODS.forEach(wood -> Sheets.addWoodType(wood.signs().standing().get().type())));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        for (BloomContent.CherryWood wood : BloomContent.WOODS) {
            event.registerSpriteSet(wood.fallingPetals().get(),
                sprites -> (type, level, x, y, z, vx, vy, vz) -> new PetalParticle(level, x, y, z, sprites));
        }
    }

    @SubscribeEvent
    public static void registerBlockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BloomContent.CHERRY_CHEST.get(), CherryChestRenderer::new);
    }

    @SubscribeEvent
    public static void registerChestItems(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private CherryChestRenderer.ItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new CherryChestRenderer.ItemRenderer(Minecraft.getInstance());
                return renderer;
            }
        }, BloomContent.WOODS.stream().map(wood -> wood.chest().asItem()).toArray(Item[]::new));
    }

    private static final class PetalParticle extends CherryParticle {
        PetalParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites);
        }
    }
}
