package dev.sakurabloom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

final class CherryChestRenderer extends ChestRenderer<CherryChestBlock.Entity> {
    private final Map<Block, EnumMap<ChestType, Material>> materials = new HashMap<>();

    CherryChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        for (var wood : BloomContent.WOODS) {
            String path = "entity/chest/" + wood.chest().getId().getPath();
            var textures = new EnumMap<ChestType, Material>(ChestType.class);
            for (ChestType half : ChestType.values()) {
                String suffix = half == ChestType.SINGLE ? "" : "_" + half.getSerializedName();
                textures.put(half, new Material(Sheets.CHEST_SHEET, SakuraBloom.id(path + suffix)));
            }
            materials.put(wood.chest().get(), textures);
        }
    }

    @Override
    protected Material getMaterial(CherryChestBlock.Entity entity, ChestType half) {
        return materials.get(entity.getBlockState().getBlock()).get(half);
    }

    static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final Map<Item, CherryChestBlock.Entity> chests = new HashMap<>();

        ItemRenderer(Minecraft minecraft) {
            super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            for (var wood : BloomContent.WOODS) {
                var chest = wood.chest().get();
                chests.put(chest.asItem(), chest.newBlockEntity(BlockPos.ZERO, chest.defaultBlockState()));
            }
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chests.get(stack.getItem()), poses, buffers, light, overlay);
        }
    }
}
