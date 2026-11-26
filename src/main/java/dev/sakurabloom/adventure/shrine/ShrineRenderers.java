package dev.sakurabloom.adventure.shrine;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=SakuraBloom.ID, value=Dist.CLIENT)
public final class ShrineRenderers {
    private static final ModelLayerLocation MAIDEN = new ModelLayerLocation(SakuraBloom.id("shrine_maiden"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MAIDEN, () -> {
            var mesh = HumanoidModel.createMesh(CubeDeformation.NONE,0);
            var body = mesh.getRoot().getChild("body");
            body.addOrReplaceChild("hakama",CubeListBuilder.create().texOffs(0,32).addBox(-4.5F,9,-2.5F,9,10,5),PartPose.ZERO);
            mesh.getRoot().getChild("head").addOrReplaceChild("ribbon",CubeListBuilder.create().texOffs(32,32)
                .addBox(-4,-5,4,8,3,1).texOffs(32,37).addBox(-2,-3,4,4,7,1),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,64);
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ShrineAdventure.FOX.get(),ctx -> new BeastRenderer(ctx,ModelLayers.FOX,"dusk_fox",.35F));
        event.registerEntityRenderer(ShrineAdventure.WOLF.get(),ctx -> new BeastRenderer(ctx,ModelLayers.WOLF,"ink_wolf",.4F));
        event.registerEntityRenderer(ShrineAdventure.HARE.get(),ctx -> new BeastRenderer(ctx,ModelLayers.RABBIT,"gloom_hare",.23F));
        event.registerEntityRenderer(ShrineAdventure.MAIDEN.get(),ctx -> new MobRenderer<ShrineMaiden,MaidenModel>(ctx,new MaidenModel(ctx.bakeLayer(MAIDEN)),.45F) {
            @Override public ResourceLocation getTextureLocation(ShrineMaiden entity) { return texture("shrine_maiden"); }
        });
    }
    private static ResourceLocation texture(String name) { return SakuraBloom.id("textures/entity/"+name+".png"); }
    private static final class MaidenModel extends HumanoidModel<ShrineMaiden> {
        MaidenModel(ModelPart root) { super(root); }
        @Override public void setupAnim(ShrineMaiden e,float step,float stride,float age,float yaw,float pitch) {
            super.setupAnim(e,step,stride,age,yaw,pitch);
            if (e.isShiftKeyDown()) {
                head.xRot=.34F;
                hat.copyFrom(head);
                leftArm.xRot=rightArm.xRot=-1.16F;
                leftArm.yRot=-.52F;
                rightArm.yRot=.52F;
            }
        }
    }
    private static final class BeastRenderer extends MobRenderer<Yokai,BeastModel> {
        private final ResourceLocation fur;
        private final boolean hare;
        BeastRenderer(EntityRendererProvider.Context ctx,ModelLayerLocation layer,String name,float shadow) {
            super(ctx,new BeastModel(ctx.bakeLayer(layer)),shadow);
            fur=texture(name);
            hare=layer.equals(ModelLayers.RABBIT);
            RenderType glow=RenderType.eyes(texture(name+"_eyes"));
            addLayer(new EyesLayer<Yokai,BeastModel>(this) { @Override public RenderType renderType() { return glow; } });
        }
        @Override public ResourceLocation getTextureLocation(Yokai e) { return fur; }
        @Override protected void scale(Yokai e,PoseStack pose,float tick) { if (hare) pose.scale(.78F,.78F,.78F); }
    }
    private static final class BeastModel extends HierarchicalModel<Yokai> {
        private final ModelPart root;
        BeastModel(ModelPart root) { this.root = root; }
        @Override public ModelPart root() { return root; }
        @Override public void setupAnim(Yokai e,float step,float stride,float age,float yaw,float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            var head=root.getChild("head");
            head.yRot=yaw*Mth.DEG_TO_RAD;
            head.xRot=pitch*Mth.DEG_TO_RAD;
            float swing=Mth.cos(step*.75F)*stride;
            if (root.hasChild("left_haunch")) {
                for (String name : new String[]{"right_ear","left_ear","nose"}) {
                    var part=root.getChild(name);
                    part.yRot+=head.yRot;
                    part.xRot+=head.xRot;
                }
                for (String name : new String[]{"left_haunch","right_haunch","left_hind_foot","right_hind_foot"})
                    root.getChild(name).xRot+=swing*.6F;
                root.getChild("left_front_leg").xRot-=swing*.7F;
                root.getChild("right_front_leg").xRot-=swing*.7F;
            } else {
                root.getChild("left_hind_leg").xRot+=swing;
                root.getChild("right_front_leg").xRot+=swing;
                root.getChild("right_hind_leg").xRot-=swing;
                root.getChild("left_front_leg").xRot-=swing;
                var tail=root.hasChild("tail") ? root.getChild("tail") : root.getChild("body").getChild("tail");
                tail.yRot+=Mth.sin(age*.16F)*.14F;
            }
        }
    }
}
