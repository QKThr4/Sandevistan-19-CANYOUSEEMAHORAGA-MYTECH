package com.sandevistanmod.client;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.tool.UpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Tela da Mesa de Upgrades (desenhada por codigo, no estilo das bancadas). */
public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
    public UpgradeScreen(UpgradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF0B1116);
        gg.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF26323C);
        gg.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF303F4B);
        for (Slot s : menu.slots) {
            int x = leftPos + s.x;
            int y = topPos + s.y;
            boolean mod = s.index >= 1 && s.index <= 4;
            gg.fill(x - 1, y - 1, x + 17, y + 17, mod ? 0xFF2A6F8F : 0xFF0B1116);
            gg.fill(x, y, x + 16, y + 16, 0xFF47596A);
        }
        gg.drawString(font, Tx.t("upgrade.tool"), leftPos + 102, topPos + 24, 0xFF9FD4FF, false);
        gg.drawCenteredString(font, Tx.t("upgrade.mods"), leftPos + 88, topPos + 40, 0xFF9FD4FF);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        this.renderTooltip(gg, mouseX, mouseY);
    }
}
