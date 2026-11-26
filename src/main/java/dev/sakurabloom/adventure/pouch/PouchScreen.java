package dev.sakurabloom.adventure.pouch;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
    private int boardY() { return topPos + GRID_Y; }
    private boolean onBoard(double x, double y) {
        return x >= boardX() && x < boardX() + CELL * pouch().columns && y >= boardY() && y < boardY() + CELL * pouch().rows;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(SakuraBloom.id("textures/gui/knotwork_pouch.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int left = boardX(), top = boardY();
        for (int y = 0; y < pouch().rows; y++) {
            for (int x = 0; x < pouch().columns; x++) {
                int px = left + x * CELL, py = top + y * CELL;
                gui.fill(px, py, px + CELL, py + CELL, 0xff765952);
                gui.fill(px+1, py+1, px+CELL-1, py+CELL-1, (x+y)%2 == 0 ? 0xffc1a28c : 0xffc8ad94);
                gui.fill(px+3, py+3, px+4, py+4, 0xffdcc2a3);
            }
        }
        var layout = PouchItem.layout(menu.bag());
        for (var p : layout.pieces()) {
            var shape = CharmShape.of(p.stack());
            if (shape == null) continue;
            for (var cell : shape.cells(p.turn())) {
                int px = left+(p.x()+cell.x())*CELL, py = top+(p.y()+cell.y())*CELL;
                gui.fill(px+1, py+1, px+CELL-1, py+CELL-1, 0xff000000 | shape.color());
                gui.fill(px+2, py+2, px+CELL-2, py+3, 0x5afff0d3);
                gui.fill(px+2, py+CELL-3, px+CELL-2, py+CELL-2, 0x405c3945);
            }
            var first = shape.cells(p.turn()).getFirst();
            gui.renderItem(p.stack(), left+(p.x()+first.x())*CELL+1, top+(p.y()+first.y())*CELL+1);
        }
        var carried = menu.getCarried();
        var shape = CharmShape.of(carried);
        if (shape != null && onBoard(mouseX, mouseY)) {
            int gx = (mouseX-left)/CELL, gy = (mouseY-top)/CELL;
            int color = layout.fits(new PouchLayout.Piece(carried, gx, gy, turn), pouch()) ? 0xa07fe0a7 : 0xa0e36a7b;
            for (var cell : shape.cells(turn)) {
                int px = left+(gx+cell.x())*CELL, py = top+(gy+cell.y())*CELL;
                gui.fill(px+1, py+1, px+CELL-1, py+CELL-1, color);
            }
        }
        gui.drawCenteredString(font, Component.translatable("pouch.sakura_bloom.controls"), leftPos+imageWidth/2, topPos+96, 0xfff8e7cf);
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
