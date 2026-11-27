package dev.sakurabloom;

import dev.sakurabloom.adventure.shrine.PrayerRewards.Prize;
import dev.sakurabloom.adventure.shrine.ShrineAdventure;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

final class PrayerCategory implements IRecipeCategory<PrayerCategory.Preview> {
    record Preview(Prize prize, long total) {}
    static final RecipeType<Preview> TYPE = RecipeType.create(SakuraBloom.ID,"shrine_prayer",Preview.class);
    private final IDrawable box, arrow;
    PrayerCategory(IGuiHelper gui) {
        box = gui.createDrawableItemLike(ShrineAdventure.BOX_ITEM.get());
        arrow = gui.getRecipeArrow();
    }
    static List<Preview> previews(List<Prize> prizes) {
        long total = prizes.stream().mapToLong(Prize::weight).sum();
        return prizes.stream().map(p->new Preview(p,total)).toList();
    }
    @Override public RecipeType<Preview> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.sakura_bloom.shrine_prayer"); }
    @Override public IDrawable getIcon() { return box; }
    @Override public int getWidth() { return 152; }
    @Override public int getHeight() { return 39; }
    @Override public void setRecipe(IRecipeLayoutBuilder layout, Preview row, IFocusGroup focuses) {
        layout.addInputSlot(39,2).setStandardSlotBackground().addItemStack(ShrineAdventure.TALISMAN.get().getDefaultInstance());
        layout.addOutputSlot(93,2).setOutputSlotBackground().addItemStack(row.prize().stack());
    }
    @Override public void draw(Preview row,IRecipeSlotsView slots,GuiGraphics gui,double mouseX,double mouseY) {
        arrow.draw(gui,62,2);
        var formatter = NumberFormat.getPercentInstance(Locale.ROOT);
        formatter.setMaximumFractionDigits(2);
        String chance = formatter.format((double)row.prize().weight()/row.total())+" ("+row.prize().weight()+"/"+row.total()+")";
        var label = Component.translatable("jei.sakura_bloom.prayer_odds",chance);
        var font = Minecraft.getInstance().font;
        gui.drawString(font,label,(getWidth()-font.width(label))/2,26,0xff404040,false);
    }
}
