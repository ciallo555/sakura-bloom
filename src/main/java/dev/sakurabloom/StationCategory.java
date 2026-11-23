package dev.sakurabloom;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

public abstract class StationCategory<T> implements IRecipeCategory<T> {
    private final RecipeType<T> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable plus;

    protected StationCategory(IGuiHelper gui, RecipeType<T> type, ItemLike station, boolean twoInputs) {
        this.type = type;
        this.title = Component.translatable("jei.sakura_bloom." + type.getUid().getPath());
        this.icon = gui.createDrawableItemLike(station);
        this.arrow = gui.getRecipeArrow();
        this.plus = twoInputs ? gui.getRecipePlusSign() : null;
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 122;
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 66, (getHeight() - 18) / 2);
        if (plus != null) {
            plus.draw(graphics, 26, 7);
        }
    }
}
