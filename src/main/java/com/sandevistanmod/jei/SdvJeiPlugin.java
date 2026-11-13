package com.sandevistanmod.jei;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.bench.BenchBlock;
import com.sandevistanmod.bench.BenchRecipes;
import com.sandevistanmod.registry.ModRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Mostra no JEI as receitas das bancadas do mod (Montagem e Medicinal), com os ingredientes e onde fazer. */
@JeiPlugin
public class SdvJeiPlugin implements IModPlugin {
    public static final RecipeType<BenchRecipes.Recipe> ASSEMBLY =
            RecipeType.create(SandevistanMod.MODID, "assembly", BenchRecipes.Recipe.class);
    public static final RecipeType<BenchRecipes.Recipe> MEDICINAL =
            RecipeType.create(SandevistanMod.MODID, "medicinal", BenchRecipes.Recipe.class);

    public static final RecipeType<BenchRecipes.Recipe> RECYCLER =
            RecipeType.create(SandevistanMod.MODID, "recycler", BenchRecipes.Recipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var gui = reg.getJeiHelpers().getGuiHelper();
        reg.addRecipeCategories(
                new BenchCategory(gui, ASSEMBLY, new ItemStack(ModRegistries.BANCADA_MONTAGEM_ITEM.get()),
                        Component.translatable("block.sandevistan.bancada_montagem")),
                new BenchCategory(gui, MEDICINAL, new ItemStack(ModRegistries.BANCADA_MEDICINAL_ITEM.get()),
                        Component.translatable("block.sandevistan.bancada_medicinal")),
                new BenchCategory(gui, RECYCLER, new ItemStack(ModRegistries.RECICLADOR_ITEM.get()),
                        Component.translatable("block.sandevistan.reciclador_sucata")));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(ASSEMBLY, BenchRecipes.of(BenchBlock.ASSEMBLY));
        reg.addRecipes(MEDICINAL, BenchRecipes.of(BenchBlock.MEDICINAL));
        reg.addRecipes(RECYCLER, BenchRecipes.of(BenchBlock.RECYCLER));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(ModRegistries.BANCADA_MONTAGEM_ITEM.get()), ASSEMBLY);
        reg.addRecipeCatalyst(new ItemStack(ModRegistries.BANCADA_MEDICINAL_ITEM.get()), MEDICINAL);
        reg.addRecipeCatalyst(new ItemStack(ModRegistries.RECICLADOR_ITEM.get()), RECYCLER);
    }
}
