package com.sandevistanmod.tool;

import com.sandevistanmod.data.Tx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Modulo de encantamento: guarda um encantamento e o nivel (criado na Mesa de Upgrades a partir de um livro encantado). */
public class EnchantModuleItem extends Item {
    public EnchantModuleItem(Properties p) {
        super(p);
    }

    @Override
    public boolean isFoil(ItemStack st) {
        return ToolMods.moduleId(st) > 0;
    }

    @Override
    public Component getName(ItemStack st) {
        int code = ToolMods.moduleId(st);
        if (code <= 0) return super.getName(st);
        String path = ToolMods.ENCH.get(ToolMods.enchIndex(code)).key().location().getPath();
        return Tx.t("module.ench", Component.translatable("enchantment.minecraft." + path), Component.translatable("enchantment.level." + ToolMods.tier(code)));
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Tx.t(ChatFormatting.GRAY, "module.ench.tip"));
    }
}
