package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.Quests;
import com.sandevistanmod.net.DoctorActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Missoes do Ripperdoc: cada licenca (tier II, III, IV) libera os crafts daquele tier na bancada. */
public class QuestScreen extends Screen {
    private Button accept;
    private Button deliver;
    private Button complete;

    public QuestScreen() {
        super(Tx.t("quest.title"));
    }

    private int px() {
        return this.width / 2 - 135;
    }

    private int py() {
        return Math.max(2, (this.height - 250) / 2);
    }

    private static void send(int code) {
        PacketDistributor.sendToServer(new DoctorActionPayload(-(20000 + code)));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = py();
        accept = addRenderableWidget(Button.builder(Tx.t("quest.btn.accept"), b -> send(0)).bounds(cx - 120, top + 226, 110, 20).build());
        deliver = addRenderableWidget(Button.builder(Tx.t("quest.btn.deliver"), b -> send(1)).bounds(cx - 120, top + 226, 110, 20).build());
        complete = addRenderableWidget(Button.builder(Tx.t("quest.btn.complete"), b -> send(2)).bounds(cx + 2, top + 226, 118, 20).build());
        addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> Minecraft.getInstance().setScreen(new DoctorScreen()))
                .bounds(px() + 6, top + 6, 50, 16).build());
        refresh();
    }

    private void refresh() {
        SandevistanData d = ClientData.DATA;
        Quests.Quest q = Quests.current(d);
        accept.visible = q != null && !d.qOn;
        deliver.visible = q != null && d.qOn;
        complete.visible = q != null && d.qOn;
        complete.active = q != null && d.qOn && Quests.done(d, q);
    }

    @Override
    public void tick() {
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int count(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static String time(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int x0 = px();
        int top = py();
        SandevistanData d = ClientData.DATA;
        gg.fill(x0, top, x0 + 270, top + 250, 0xD0101214);
        gg.fill(x0, top, x0 + 270, top + 2, 0xFFFF2040);
        gg.drawCenteredString(this.font, Tx.t("quest.title"), cx, top + 8, 0xFF5566);
        gg.drawCenteredString(this.font, Tx.t("quest.current", romano(d.licence)), cx, top + 24, 0x33CCFF);
        Quests.Quest q = Quests.current(d);
        if (q == null) {
            gg.drawCenteredString(this.font, Tx.t("quest.all"), cx, top + 110, 0x55FF55);
            return;
        }
        gg.drawCenteredString(this.font, Tx.t("quest.name." + q.tier()), cx, top + 40, 0xFFFFFF);
        gg.drawWordWrap(this.font, Tx.t("quest.desc." + q.tier()), x0 + 10, top + 54, 250, 0xAAAAAA);
        int y = top + 86;
        for (int i = 0; i < q.objs().size(); i++) {
            Quests.Obj o = q.objs().get(i);
            int prog = d.qOn ? d.qp[i] : 0;
            boolean ok = prog >= o.target();
            gg.drawString(this.font, o.label(), x0 + 10, y, ok ? 0x55FF55 : 0xDDDDDD, false);
            String txt;
            if (o.kind() == Quests.Kind.ACTIVE) txt = time(Math.min(prog, o.target())) + "/" + time(o.target());
            else txt = Math.min(prog, o.target()) + "/" + o.target();
            if (o.kind() == Quests.Kind.ITEM && d.qOn && !ok) txt += " (" + Tx.t("quest.have", count(o.item().get())).getString() + ")";
            gg.drawString(this.font, txt, x0 + 260 - this.font.width(txt), y, ok ? 0x55FF55 : 0x88DDFF, false);
            ClientOverlay.bar(gg, x0 + 10, y + 10, 250, 4, Math.min(1f, prog / (float) o.target()), ok ? 0xFF55FF55 : 0xFF33CCFF);
            y += 20;
        }
        gg.drawCenteredString(this.font, Tx.t("quest.reward", q.emeralds(), q.levels(), romano(q.tier())), cx, top + 208, 0xFFD060);
    }

    private static String romano(int t) {
        return switch (t) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }
}
