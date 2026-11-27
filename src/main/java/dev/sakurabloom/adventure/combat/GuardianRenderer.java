package dev.sakurabloom.adventure.combat;

import dev.sakurabloom.SakuraBloom;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SakuraBloom.ID, value = Dist.CLIENT)
public final class GuardianRenderer extends MobRenderer<VeilGuardian, HumanoidModel<VeilGuardian>> {
    private final ResourceLocation texture;
    private final float size;

    private GuardianRenderer(EntityRendererProvider.Context ctx, String id, float size) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(layer(id))), .55F);
        texture = SakuraBloom.id("textures/entity/" + id + ".png");
        this.size = size;
    }

    @Override public ResourceLocation getTextureLocation(VeilGuardian entity) { return texture; }
    @Override protected void scale(VeilGuardian entity, PoseStack pose, float frame) { pose.scale(size, size, size); }

    private static ModelLayerLocation layer(String id) { return new ModelLayerLocation(SakuraBloom.id(id), "main"); }

    private static LayerDefinition model(Sanctum theme) {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        var root = mesh.getRoot();
        var head = root.getChild("head");
        var body = root.getChild("body");
        body.addOrReplaceChild("haori", CubeListBuilder.create().texOffs(28,33).addBox(-5,3,-2.6F,10,11,5), PartPose.ZERO);
        if (theme == Sanctum.LANTERN) {
            head.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(0,33).addBox(-5,-10,-5,10,9,10)
                .texOffs(32,51).addBox(-4,-12,-4,8,2,8), PartPose.ZERO);
        } else if (theme == Sanctum.MOONLIT) {
            head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0,45).addBox(-8,-8,-7,16,1,14)
                .texOffs(32,51).addBox(-4,-10,-4,8,2,8), PartPose.ZERO);
        } else if (theme == Sanctum.VERMILION) {
            for (int side : new int[]{-1,1}) head.addOrReplaceChild("horn"+side, CubeListBuilder.create().texOffs(4,37)
                .addBox(-1,-6,-1,2,7,2), PartPose.offsetAndRotation(4*side,-6,0,0,0,side*.32F));
        } else if (theme == Sanctum.ASHEN) {
            for (int side : new int[]{-1,1}) body.addOrReplaceChild("wing"+side, CubeListBuilder.create().texOffs(2,33)
                .addBox(-2,-8,0,5,16,2), PartPose.offsetAndRotation(5*side,3,2,.28F,0,side*.59F));
        } else {
            head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(2,35).addBox(-5,-7,-5,10,3,2), PartPose.ZERO);
        }
        body.addOrReplaceChild("brocade_sash", CubeListBuilder.create().texOffs(64,0)
            .addBox(-4.8F,7,-2.9F,10,3,6), PartPose.ZERO);
        for (int side : new int[]{-1,1}) {
            var arm = root.getChild(side == 1 ? "left_arm" : "right_arm");
            arm.addOrReplaceChild("mantle",CubeListBuilder.create().texOffs(64,52)
                .addBox(side == 1 ? -1.5F : -3.5F,-2.5F,-2.5F,5,5,5),PartPose.ZERO);
            body.addOrReplaceChild("silk_tail"+side,CubeListBuilder.create().texOffs(64,28)
                .addBox(-2,0,0,4,9,1),PartPose.offsetAndRotation(side*2.8F,9,2.2F,.12F,0,side*.14F));
        }
        return LayerDefinition.create(mesh,128,128);
    }

    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var theme : Sanctum.values()) event.registerLayerDefinition(layer(theme.id+"_guardian"), () -> model(theme));
        event.registerLayerDefinition(layer("veil_sentinel"), () -> model(null));
    }

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var theme : Sanctum.values())
            event.registerEntityRenderer(theme.guardian.get(), ctx -> new GuardianRenderer(ctx, theme.id+"_guardian", 1.44F));
        event.registerEntityRenderer(SpiritCombat.SENTINEL.get(), ctx -> new GuardianRenderer(ctx,"veil_sentinel",1.05F));
    }
}
