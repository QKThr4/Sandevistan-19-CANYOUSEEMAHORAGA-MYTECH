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
        return !ToolMods.modOf(st).none();
    }

    @Override
    public Component getName(ItemStack st) {
        ToolMods.Mod m = ToolMods.modOf(st);
        if (m.none()) return super.getName(st);
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(m.e());
        return Tx.t("module.ench", Component.translatable("enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.')),
                Component.translatable("enchantment.level." + m.l()));
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Tx.t(ChatFormatting.GRAY, "module.ench.tip"));
    }
}
