package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

final class KitchenRenderer implements BlockEntityRenderer<KitchenBlockEntity> {
    static final ModelResourceLocation CAMPFIRE_STAND = ModelResourceLocation.standalone(SakuraBloom.id("block/campfire_cookware_stand"));
    private final BlockRenderDispatcher blocks;
    private final ItemRenderer items;

    KitchenRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
        items = context.getItemRenderer();
    }

    @Override
    public AABB getRenderBoundingBox(KitchenBlockEntity cookware) {
        return new AABB(cookware.getBlockPos()).expandTowards(0, -1, 0);
    }

    @Override
    public void render(KitchenBlockEntity cookware, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var level = cookware.getLevel();
        var state = cookware.getBlockState();
        boolean steamer = state.is(KitchenContent.BAMBOO_STEAMER.get());
        boolean cooking = state.getValue(KitchenBlock.COOKING);
        if (level == null) {
            return;
        }
        var below = cookware.getBlockPos().below();
        var campfire = level.getBlockState(below);
        if (campfire.is(BlockTags.CAMPFIRES)) {
            pose.pushPose();
            pose.translate(0, -1, 0);
            var stand = blocks.getBlockModelShaper().getModelManager().getModel(CAMPFIRE_STAND);
            var layer = Sheets.cutoutBlockSheet();
            blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(layer), campfire, stand,
                1, 1, 1, LevelRenderer.getLightColor(level, below), overlay, ModelData.EMPTY, null);
            pose.popPose();
        }
        if (steamer && cooking) {
            return;
        }

        var contents = cookware.displayItems();
        int visible = (int) contents.stream().filter(item -> !item.isEmpty()).count();
        int rows = (visible + 2) / 3;
        boolean simmer = cooking && KitchenBlock.hasHeat(level, cookware.getBlockPos());
        int slot = 0;
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) {
                continue;
            }
            int row = slot / 3;
            int width = Math.min(3, visible - row * 3);
            float x = 0.5F + (slot % 3 - (width - 1) / 2F) * 0.17F;
            float z = 0.5F + (row - (rows - 1) / 2F) * 0.17F;
            float y = steamer ? 0.42F : 0.28F;
            if (simmer) {
                y += Mth.sin((level.getGameTime() % 120 + partialTick) * 0.32F + slot) * 0.013F;
            }
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(85));
            pose.mulPose(Axis.ZP.rotationDegrees(13F * (slot % 3 - 1)));
            float scale = cookware.ready() ? 0.44F : visible == 1 ? 0.36F : 0.27F;
            pose.scale(scale, scale, scale);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, level, slot);
            pose.popPose();
            slot++;
        }
    }
}
