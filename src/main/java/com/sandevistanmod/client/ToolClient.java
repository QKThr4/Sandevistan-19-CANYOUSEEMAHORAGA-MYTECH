package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import com.sandevistanmod.tool.ToolMods;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Cores das ferramentas (a textura vanilla de ferro recebe uma tinta: aco azulado, Ciber escuro) e tooltip dos modulos. */
public final class ToolClient {
    private ToolClient() {}

    @EventBusSubscriber(modid = SandevistanMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Colors {
        @SubscribeEvent
        public static void colors(RegisterColorHandlersEvent.Item e) {
            Item[] steel = new Item[ModRegistries.FERRAMENTAS_ACO.size()];
            for (int i = 0; i < steel.length; i++) steel[i] = ModRegistries.FERRAMENTAS_ACO.get(i).get();
            // tridente e desenhado por nos (sem tinta)
            e.register((st, idx) -> idx == 0 ? 0xFFA9BCD0 : -1, steel[0], steel[1], steel[2], steel[3], steel[4], steel[5], steel[6], steel[8], steel[9]);
            Item[] cy = new Item[ModRegistries.FERRAMENTAS_CIBER.size()];
            for (int i = 0; i < cy.length; i++) cy[i] = ModRegistries.FERRAMENTAS_CIBER.get(i).get();
            e.register((st, idx) -> idx == 0 ? 0xFF565B6E : -1, cy[0], cy[1], cy[2], cy[3], cy[4], cy[5], cy[6], cy[8]);
        }
    }

    @EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
    public static final class Tips {
        @SubscribeEvent
        public static void tip(ItemTooltipEvent e) {
            ItemStack st = e.getItemStack();
            if (!ToolMods.isModTool(st)) return;
            int used = 0;
            for (int m : ToolMods.getMods(st)) if (m > 0) used++;
            e.getToolTip().add(Tx.t(ChatFormatting.AQUA, "multi.slots", used, ToolMods.SLOTS));
            for (int t = 1; t <= 9; t++) {
                int n = t >= ToolMods.FREEZE ? ToolMods.maxTier(st, t) : ToolMods.power(st, t);
                if (n > 0) e.getToolTip().add(Tx.t(ChatFormatting.GREEN, "tool.mod." + t, n));
            }
            for (int m : ToolMods.getMods(st)) {
                if (ToolMods.isEnchCode(m)) {
                    String path = ToolMods.ENCH.get(ToolMods.enchIndex(m)).key().location().getPath();
                    e.getToolTip().add(net.minecraft.network.chat.Component.translatable("enchantment.minecraft." + path)
                            .append(" ").append(net.minecraft.network.chat.Component.translatable("enchantment.level." + ToolMods.tier(m)))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }
}
