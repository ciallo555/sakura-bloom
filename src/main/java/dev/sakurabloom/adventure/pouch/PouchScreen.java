package dev.sakurabloom.adventure.pouch;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = SakuraBloom.ID, value = Dist.CLIENT)
public final class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final int CELL = 18, GRID_Y = 20;
    private int turn;

    PouchScreen(PouchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageHeight = 204;
        inventoryLabelY = PouchMenu.INVENTORY_Y - 12;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(PouchContent.MENU.get(), PouchScreen::new);
    }

    private PouchItem pouch() { return menu.bag().getItem() instanceof PouchItem pouch ? pouch : PouchContent.KNOTWORK.get(); }
    private int boardX() { return leftPos + (imageWidth - CELL * pouch().columns) / 2; }
    private int boardY() { return topPos + GRID_Y + (4 - pouch().rows) * 6; }
    private boolean onBoard(double x, double y) {
        return x >= boardX() && x < boardX() + CELL * pouch().columns && y >= boardY() && y < boardY() + CELL * pouch().rows;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        var lining = pouch().rows == 3 ? "knotwork_pouch" : "cloud_brocade_pouch";
        gui.blit(SakuraBloom.id("textures/gui/" + lining + ".png"), leftPos, topPos, 0, 0,
            imageWidth, imageHeight, imageWidth, imageHeight);
        int left = boardX(), top = boardY();
        var layout = PouchItem.layout(menu.bag());
        for (var piece : layout.pieces()) drawPatch(gui, piece);
        var carried = menu.getCarried();
        if (CharmShape.of(carried) != null && onBoard(mouseX, mouseY)) {
            var piece = new PouchLayout.Piece(carried, (mouseX-left)/CELL, (mouseY-top)/CELL, turn);
            boolean fits = layout.fits(piece, pouch());
            gui.enableScissor(left, top, left + pouch().columns * CELL, top + pouch().rows * CELL);
            gui.setColor(fits ? .83F : 1F, fits ? 1F : .48F, fits ? .85F : .48F, .78F);
            drawPatch(gui, piece);
            gui.setColor(1,1,1,1);
            gui.disableScissor();
        }
        gui.drawCenteredString(font, Component.translatable("pouch.sakura_bloom.controls"), leftPos+imageWidth/2, topPos+96, 0xfff8e7cf);
    }

    private void drawPatch(GuiGraphics gui, PouchLayout.Piece piece) {
        var shape = CharmShape.of(piece.stack());
        if (shape == null) return;
        var patch = shape.patch(piece.turn());
        var name = BuiltInRegistries.ITEM.getKey(piece.stack().getItem()).getPath();
        int x = boardX() + piece.x()*CELL, y = boardY() + piece.y()*CELL;
        int width = patch.width()*CELL, height = patch.height()*CELL;
        gui.blit(SakuraBloom.id("textures/gui/patch/" + name + "_" + piece.turn() + ".png"),
            x,y,0,0,width,height,width,height);
        gui.renderItem(piece.stack(), x + patch.anchor().x()*CELL + 1, y + patch.anchor().y()*CELL + 1);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && onBoard(mouseX, mouseY)) {
            var p = PouchItem.layout(menu.bag()).at((mouseX-boardX())/CELL, (mouseY-boardY())/CELL);
            if (p != null) gui.renderTooltip(font, p.stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!onBoard(x, y)) return super.mouseClicked(x, y, button);
        if (button == 1 && !menu.getCarried().isEmpty()) turn = (turn+1)%4;
        if (button == 0 && minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PouchMenu.button(((int)x-boardX())/CELL, ((int)y-boardY())/CELL, turn));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_R && CharmShape.of(menu.getCarried()) != null) {
            turn = (turn+1)%4;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
