package com.sandevistanmod.client;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Visibilidade dos implantes de ferro no corpo: bracos e pernas, cada um independente. */
public class VisibilityScreen extends Screen {
    private final Screen parent;
    private Button armsBtn;
    private Button legsBtn;

    public VisibilityScreen(Screen parent) {
        super(Tx.t("vis.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        armsBtn = addRenderableWidget(Button.builder(armsLabel(), b -> PacketDistributor.sendToServer(new ActionPayload(4)))
                .bounds(cx - 90, y + 8, 180, 20).build());
        legsBtn = addRenderableWidget(Button.builder(legsLabel(), b -> PacketDistributor.sendToServer(new ActionPayload(5)))
                .bounds(cx - 90, y + 34, 180, 20).build());
        addRenderableWidget(Button.builder(Tx.t("btn.back"), b -> onClose())
                .bounds(cx - 90, y + 66, 180, 20).build());
        refresh();
    }

    private static Component armsLabel() {
        return ClientData.DATA.armsVisible ? Tx.t("arms.visible") : Tx.t("arms.hidden");
    }

    private static Component legsLabel() {
        return ClientData.DATA.legsVisible ? Tx.t("legs.visible") : Tx.t("legs.hidden");
    }

    private void refresh() {
        armsBtn.setMessage(armsLabel());
        legsBtn.setMessage(legsLabel());
        armsBtn.active = ClientData.DATA.arms() > 0;
        legsBtn.active = ClientData.DATA.legs() > 0;
    }

    @Override
    public void tick() {
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int y = this.height / 2 - 40;
        gg.fill(cx - 110, y - 22, cx + 110, y + 96, 0xC0101410);
        gg.fill(cx - 110, y - 22, cx + 110, y - 20, 0xFF39FF14);
        gg.drawCenteredString(this.font, Tx.t("vis.title"), cx, y - 12, 0x39FF14);
    }
}
