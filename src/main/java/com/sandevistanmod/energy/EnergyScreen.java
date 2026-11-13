package com.sandevistanmod.energy;

import com.sandevistanmod.data.Tx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Tela do carregador e do gerador a combustivel. */
public class EnergyScreen extends AbstractContainerScreen<EnergyMenu> {
    public EnergyScreen(EnergyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float pt, int mx, int my) {
        gg.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF0B1116);
        gg.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF26323C);
        gg.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF303F4B);
        for (Slot s : menu.slots) {
            int x = leftPos + s.x;
            int y = topPos + s.y;
            gg.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0B1116);
            gg.fill(x, y, x + 16, y + 16, 0xFF47596A);
        }
        boolean charger = menu.kind() == EnergyBlock.CHARGER;
        // chama (gerador) ou barra da bateria (carregador)
        if (!charger) {
            int fx = leftPos + 58;
            int fy = topPos + 28;
            gg.fill(fx, fy, fx + 14, fy + 14, 0xFF1A1E22);
            int total = Math.max(1, menu.burnMax());
            int h = menu.burn() > 0 ? Math.max(1, Math.min(14, menu.burn() * 14 / total)) : 0;
            gg.fill(fx, fy + 14 - h, fx + 14, fy + 14, 0xFFFF9A2E);
        } else {
            int bx = leftPos + 58;
            int by = topPos + 28;
            gg.fill(bx, by + 4, bx + 14, by + 12, 0xFF1A1E22);
            gg.fill(bx, by + 4, bx + 14 * menu.batteryPermille() / 1000, by + 12, 0xFF33CCFF);
        }
        if (charger) {
            int bar = Math.round(menu.energy() * 128f / EnergyBlockEntity.BUFFER);
            gg.fill(leftPos + 24, topPos + 54, leftPos + 152, topPos + 62, 0xFF1A1E22);
            gg.fill(leftPos + 24, topPos + 54, leftPos + 24 + bar, topPos + 62, 0xFF33CCFF);
            gg.drawCenteredString(font, Tx.t("energy.buffer", menu.energy(), EnergyBlockEntity.BUFFER).getString(), leftPos + 88, topPos + 65, 0xFF66DDFF);
            gg.drawCenteredString(font, Tx.t("charger.rate", menu.rate(), EnergyBlockEntity.CHARGER_IN).getString(), leftPos + 88, topPos + 75, 0xFFBBCCDD);
        } else {
            gg.drawCenteredString(font, Tx.t("gen.rate", menu.energy()).getString(), leftPos + 88, topPos + 58, 0xFF66DDFF);
            gg.drawCenteredString(font, Tx.t("gen.delivered", menu.rate()).getString(), leftPos + 88, topPos + 70, menu.rate() > 0 ? 0xFF7CFFA0 : 0xFFFF9977);
            if (menu.rate() <= 0 && menu.energy() > 0) gg.drawCenteredString(font, Tx.t("gen.nocharger").getString(), leftPos + 88, topPos + 82, 0xFFFF9977);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        super.render(gg, mx, my, pt);
        renderTooltip(gg, mx, my);
    }
}
