package com.sandevistanmod.jei;

import com.sandevistanmod.bench.BenchRecipes;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Categoria do JEI para uma bancada: ingredientes numa grade de 3 colunas, seta e resultado. */
public class BenchCategory implements IRecipeCategory<BenchRecipes.Recipe> {
    private static final int W = 116;
    private static final int H = 62;

    private final RecipeType<BenchRecipes.Recipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;

    public BenchCategory(IGuiHelper gui, RecipeType<BenchRecipes.Recipe> type, ItemStack icon, Component title) {
        this.type = type;
        this.title = title;
        this.icon = gui.createDrawableItemStack(icon);
        this.background = gui.createBlankDrawable(W, H);
    }

    @Override
    public RecipeType<BenchRecipes.Recipe> getRecipeType() {
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

    public int getWidth() {
        return W;
    }

    public int getHeight() {
        return H;
    }

    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BenchRecipes.Recipe recipe, IFocusGroup focuses) {
        List<BenchRecipes.Req> in = recipe.in();
        for (int i = 0; i < in.size(); i++) {
            BenchRecipes.Req q = in.get(i);
            ItemStack st = q.icon().copy();
            st.setCount(q.count());
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 1 + (i / 3) * 18).addItemStack(st);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 22).addItemStack(recipe.out());
    }
}
