package com.sandevistanmod.emi;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.bench.BenchBlock;
import com.sandevistanmod.bench.BenchRecipes;
import com.sandevistanmod.registry.ModRegistries;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Mostra no EMI as receitas das bancadas do mod (Montagem, Medicinal e Reciclador). */
@EmiEntrypoint
public class SdvEmiPlugin implements EmiPlugin {
    private static ResourceLocation id(String p) {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, p);
    }

    @Override
    public void register(EmiRegistry registry) {
        add(registry, "assembly", BenchBlock.ASSEMBLY, new ItemStack(ModRegistries.BANCADA_MONTAGEM_ITEM.get()));
        add(registry, "medicinal", BenchBlock.MEDICINAL, new ItemStack(ModRegistries.BANCADA_MEDICINAL_ITEM.get()));
        add(registry, "recycler", BenchBlock.RECYCLER, new ItemStack(ModRegistries.RECICLADOR_ITEM.get()));
    }

    private static void add(EmiRegistry registry, String name, int kind, ItemStack icon) {
        EmiRecipeCategory cat = new EmiRecipeCategory(id(name), EmiStack.of(icon));
        registry.addCategory(cat);
        registry.addWorkstation(cat, EmiStack.of(icon));
        List<BenchRecipes.Recipe> all = BenchRecipes.of(kind);
        for (int i = 0; i < all.size(); i++) registry.addRecipe(new BenchEmiRecipe(cat, id("/" + name + "/" + i), all.get(i)));
    }

    static class BenchEmiRecipe extends BasicEmiRecipe {
        BenchEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, BenchRecipes.Recipe r) {
            super(cat, id, 112, 56);
            for (BenchRecipes.Req q : r.in()) this.inputs.add(EmiStack.of(q.icon()).setAmount(q.count()));
            this.outputs.add(EmiStack.of(r.out()));
        }

        @Override
        public void addWidgets(WidgetHolder w) {
            for (int k = 0; k < inputs.size(); k++) w.addSlot(inputs.get(k), (k % 3) * 18, (k / 3) * 18);
            w.addTexture(EmiTexture.EMPTY_ARROW, 62, 18);
            w.addSlot(outputs.get(0), 90, 14).large(true).recipeContext(this);
        }
    }
}
