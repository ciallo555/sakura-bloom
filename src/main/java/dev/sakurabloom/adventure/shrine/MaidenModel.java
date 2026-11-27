package dev.sakurabloom.adventure.shrine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sakurabloom.SakuraBloom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/** Reads the keeper's box-UV YSM geometry during the same resource reload as other model layers. */
final class MaidenModel extends HierarchicalModel<ShrineMaiden> {
    static final ResourceLocation SKIN = SakuraBloom.id("ysm/shrine_maiden/textures/keeper.png");
    private final ModelPart root;
    private final Map<String, ModelPart> bones = new HashMap<>();

    MaidenModel(ModelPart root) {
        this.root = root;
        for (String name : new String[]{"Body","Head","LeftArm","RightArm","LeftForeArm","RightForeArm",
            "LeftLeg","RightLeg","LeftHakama","RightHakama","LeftObiTail","RightObiTail","BackHair"}) {
            bones.put(name, getAnyDescendantWithName(name).orElseThrow());
        }
    }

    private record Pivot(float x, float y, float z) {
        private static final Pivot ZERO = new Pivot(0,0,0);
        static Pivot read(JsonArray a) {
            return a == null ? ZERO : new Pivot(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
        }
    }

    static LayerDefinition layer() {
        var resource = SakuraBloom.id("ysm/shrine_maiden/models/main.json");
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var mesh = new MeshDefinition();
            var parts = new HashMap<String, PartDefinition>();
            var pivots = new HashMap<String, Pivot>();
            for (var entry : geometry.getAsJsonArray("bones")) {
                var node = entry.getAsJsonObject();
                String name = node.get("name").getAsString();
                String parent = node.has("parent") ? node.get("parent").getAsString() : "";
                Pivot pivot = Pivot.read(node.getAsJsonArray("pivot"));
                Pivot rotation = Pivot.read(node.getAsJsonArray("rotation"));
                var cubes = CubeListBuilder.create();
                if (node.has("cubes")) {
                    for (var element : node.getAsJsonArray("cubes")) {
                        var cube = element.getAsJsonObject();
                        var uv = cube.getAsJsonArray("uv");
                        var pos = Pivot.read(cube.getAsJsonArray("origin"));
                        var size = Pivot.read(cube.getAsJsonArray("size"));
                        cubes.texOffs(uv.get(0).getAsInt(),uv.get(1).getAsInt())
                            .addBox(pos.x-pivot.x, pivot.y-pos.y-size.y, pos.z-pivot.z, size.x,size.y,size.z);
                    }
                }
                Pivot base = pivots.getOrDefault(parent, new Pivot(0,24,0));
                var pose = PartPose.offsetAndRotation(pivot.x-base.x, base.y-pivot.y, pivot.z-base.z,
                    -rotation.x*Mth.DEG_TO_RAD, rotation.y*Mth.DEG_TO_RAD, -rotation.z*Mth.DEG_TO_RAD);
                parts.put(name, parts.getOrDefault(parent, mesh.getRoot()).addOrReplaceChild(name, cubes, pose));
                pivots.put(name,pivot);
            }
            var description = geometry.getAsJsonObject("description");
            return LayerDefinition.create(mesh,description.get("texture_width").getAsInt(),description.get("texture_height").getAsInt());
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to bake " + resource, ex);
        }
    }

    @Override public ModelPart root() { return root; }

    @Override public void setupAnim(ShrineMaiden maiden,float step,float stride,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        var head = bones.get("Head");
        head.yRot = Mth.clamp(yaw,-65,65)*Mth.DEG_TO_RAD;
        head.xRot = pitch*Mth.DEG_TO_RAD;
        bones.get("BackHair").xRot += Mth.sin(age*.085F)*.025F;
        if (maiden.isShiftKeyDown()) {
            bones.get("Body").xRot = .10F;
            head.xRot += .23F;
        }
        poseSide("Left",1,maiden.isShiftKeyDown(),step,stride,age);
        poseSide("Right",-1,maiden.isShiftKeyDown(),step,stride,age);
    }

    private void poseSide(String side,int sign,boolean pray,float step,float stride,float age) {
        float walk = Mth.cos(step*.6662F)*stride*sign;
        bones.get(side+"Leg").xRot = walk*.90F;
        bones.get(side+"Hakama").xRot = walk*.58F;
        bones.get(side+"ObiTail").xRot += walk*.23F + Mth.sin(age*.08F)*.03F;
        var arm = bones.get(side+"Arm");
        var forearm = bones.get(side+"ForeArm");
        arm.xRot = pray ? -.78F : -walk*.68F;
        if (pray) {
            arm.zRot += sign*.19F;
            arm.yRot = -sign*.18F;
            forearm.xRot = -.65F;
            forearm.yRot = -sign*.41F;
            forearm.zRot = sign*.27F;
        } else {
            forearm.xRot = -.11F - Mth.abs(walk)*.13F;
            arm.zRot += Mth.sin(age*.07F)*sign*.026F;
        }
    }
}
