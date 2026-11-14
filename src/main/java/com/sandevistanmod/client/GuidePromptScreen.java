package com.sandevistanmod.client;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.PacketDistributor;

/** Primeira vez que o jogador abre a aba J: pergunta se quer ver o livro guia. */
public class GuidePromptScreen extends Screen {
    private final Screen parent;

    public GuidePromptScreen(Screen parent) {
        super(Tx.t("guide.ask.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 20;
        addRenderableWidget(Button.builder(Tx.t("guide.ask.yes"), b -> answer(true)).bounds(cx - 105, y, 100, 20).build());
        addRenderableWidget(Button.builder(Tx.t("guide.ask.no"), b -> answer(false)).bounds(cx + 5, y, 100, 20).build());
    }

    private void answer(boolean yes) {
        ClientData.DATA.guideSeen = true;
        PacketDistributor.sendToServer(new ActionPayload(90));
        if (yes) SandevistanScreen.openGuide();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        answer(false);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mx, int my, float pt) {
        super.renderBackground(gg, mx, my, pt);
        int cx = this.width / 2;
        int y = this.height / 2 - 50;
        gg.fill(cx - 160, y - 14, cx + 160, y + 96, 0xE0101410);
        gg.fill(cx - 160, y - 14, cx + 160, y - 12, 0xFF39FF14);
        gg.drawCenteredString(this.font, Tx.t("guide.ask.title"), cx, y - 4, 0x39FF14);
        int ly = y + 12;
        for (var line : this.font.split(Tx.t("guide.ask.body"), 296)) {
            gg.drawString(this.font, line, cx - 148, ly, 0xFFFFFFFF, false);
            ly += 11;
        }
    }
}
