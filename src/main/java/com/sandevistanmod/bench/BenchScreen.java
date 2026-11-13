package com.sandevistanmod.bench;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.BenchFillPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela das bancadas: grade 3x3, resultado e, ao lado, o livro de receitas (clique para preencher com o que voce tem). */
public class BenchScreen extends AbstractContainerScreen<BenchMenu> {
    private static final int ROW_H = 22;
    private static final int ROWS = 7;
    private static final int PANEL_W = 136;
    private int scroll;
    private boolean open = true;
    private int selected = -1;
    private Button toggle;
    private Button fill;
    private Button back;

    public BenchScreen(BenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(Tx.t("bench.toggle"), b -> {
            open = !open;
            selected = -1;
            sync();
        }).bounds(leftPos + imageWidth - 58, topPos - 20, 58, 18).build());
        fill = addRenderableWidget(Button.builder(Tx.t("bench.fill"), b -> {
            if (selected >= 0) PacketDistributor.sendToServer(new BenchFillPayload(selected));
        }).bounds(panelX() + 4, topPos + imageHeight - 22, 74, 18).build());
        back = addRenderableWidget(Button.builder(Tx.t("bench.back"), b -> {
            selected = -1;
            sync();
        }).bounds(panelX() + 80, topPos + imageHeight - 22, 52, 18).build());
        sync();
    }

    private void sync() {
        if (fill != null) fill.visible = open && selected >= 0;
        if (back != null) back.visible = open && selected >= 0;
    }

    private List<BenchRecipes.Recipe> recipes() {
        return BenchRecipes.of(menu.kind());
    }

    private int panelX() {
        return leftPos - PANEL_W - 4;
    }

    private int have(BenchRecipes.Req q) {
        int n = 0;
        for (ItemStack st : minecraft.player.getInventory().items) if (!st.isEmpty() && q.test().test(st)) n += st.getCount();
        return n;
    }

    private boolean locked(BenchRecipes.Recipe r) {
        return com.sandevistanmod.logic.Quests.needed(r.out()) > com.sandevistanmod.client.ClientData.DATA.licence;
    }

    private boolean canMake(BenchRecipes.Recipe r) {
        for (BenchRecipes.Req q : r.in()) if (have(q) < q.count()) return false;
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        // fundo da bancada
        gg.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xFF0B1116);
        gg.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF26323C);
        gg.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF303F4B);
        for (Slot s : menu.slots) {
            int x = leftPos + s.x;
            int y = topPos + s.y;
            gg.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0B1116);
            gg.fill(x, y, x + 16, y + 16, 0xFF47596A);
        }
        gg.drawString(font, "->", leftPos + 100, topPos + 39, 0xFF9FD4FF, false);

        if (!open) return;
        // livro de receitas
        int px = panelX();
        gg.fill(px - 1, topPos - 1, px + PANEL_W + 1, topPos + imageHeight + 1, 0xFF0B1116);
        gg.fill(px, topPos, px + PANEL_W, topPos + imageHeight, 0xFF26323C);
        gg.drawString(font, Tx.t(ChatFormatting.AQUA, "bench.book"), px + 4, topPos + 4, 0xFFFFFF, false);
        List<BenchRecipes.Recipe> list = recipes();
        if (selected >= list.size()) selected = -1;
        if (selected >= 0) {
            BenchRecipes.Recipe r = list.get(selected);
            gg.renderItem(r.out(), px + 4, topPos + 18);
            if (r.out().getCount() > 1) gg.renderItemDecorations(font, r.out(), px + 4, topPos + 18);
            gg.drawString(font, font.plainSubstrByWidth(r.out().getHoverName().getString(), PANEL_W - 28), px + 24, topPos + 22, 0xFFFFFFFF, false);
            gg.drawString(font, Tx.t(ChatFormatting.GRAY, "bench.needs"), px + 4, topPos + 40, 0xFFFFFF, false);
            int y = topPos + 52;
            for (BenchRecipes.Req q : r.in()) {
                if (y > topPos + imageHeight - 28) break;
                boolean ok = have(q) >= q.count();
                gg.renderItem(q.icon(), px + 4, y);
                Component nm = q.key() != null ? Component.translatable("sandevistan." + q.key()) : q.icon().getHoverName();
                String line = q.count() + "x " + font.plainSubstrByWidth(nm.getString(), PANEL_W - 52);
                gg.drawString(font, line, px + 24, y + 4, ok ? 0xFF7CFFA0 : 0xFFFF7777, false);
                y += 17;
            }
            if (locked(r)) gg.drawString(font, Tx.t(ChatFormatting.RED, "bench.locked.tip", com.sandevistanmod.logic.Quests.needed(r.out())),
                    px + 4, topPos + imageHeight - 36, 0xFFFFFF, false);
            return;
        }
        scroll = Math.max(0, Math.min(Math.max(0, list.size() - ROWS), scroll));
        for (int row = 0; row < ROWS; row++) {
            int idx = scroll + row;
            if (idx >= list.size()) break;
            BenchRecipes.Recipe r = list.get(idx);
            int ry = topPos + 16 + row * ROW_H;
            boolean hover = mouseX >= px && mouseX < px + PANEL_W && mouseY >= ry && mouseY < ry + ROW_H;
            gg.fill(px + 2, ry, px + PANEL_W - 2, ry + ROW_H - 2, hover ? 0xFF4A6275 : 0xFF3A4C5B);
            gg.renderItem(r.out(), px + 4, ry + 2);
            if (r.out().getCount() > 1) gg.renderItemDecorations(font, r.out(), px + 4, ry + 2);
            String name = font.plainSubstrByWidth(r.out().getHoverName().getString(), PANEL_W - 30);
            boolean lk = locked(r);
            gg.drawString(font, name, px + 24, ry + 7, lk ? 0xFF8A6A6A : canMake(r) ? 0xFF7CFFA0 : 0xFFC8D2DC, false);
            if (lk) gg.drawString(font, "\u00a7c#" + com.sandevistanmod.logic.Quests.needed(r.out()), px + PANEL_W - 18, ry + 7, 0xFFFFFF, false);
        }
        if (list.size() > ROWS) {
            gg.drawString(font, (scroll + 1) + "-" + Math.min(list.size(), scroll + ROWS) + "/" + list.size(),
                    px + PANEL_W - 52, topPos + 4, 0xFF8899AA, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        super.renderTooltip(gg, mouseX, mouseY);
        if (!open || selected >= 0) return;
        int px = panelX();
        List<BenchRecipes.Recipe> list = recipes();
        for (int row = 0; row < ROWS; row++) {
            int idx = scroll + row;
            if (idx >= list.size()) break;
            int ry = topPos + 16 + row * ROW_H;
            if (mouseX >= px && mouseX < px + PANEL_W && mouseY >= ry && mouseY < ry + ROW_H) {
                BenchRecipes.Recipe r = list.get(idx);
                List<Component> tip = new ArrayList<>();
                tip.add(r.out().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                if (locked(r)) tip.add(Tx.t(ChatFormatting.RED, "bench.locked.tip", com.sandevistanmod.logic.Quests.needed(r.out())));
                for (BenchRecipes.Req q : r.in()) {
                    Component name = q.key() != null ? Component.translatable("sandevistan." + q.key()) : q.icon().getHoverName();
                    boolean ok = have(q) >= q.count();
                    tip.add(Component.literal(q.count() + "x ").append(name)
                            .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                }
                tip.add(Tx.t(ChatFormatting.DARK_AQUA, "bench.click"));
                gg.renderComponentTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = panelX();
        List<BenchRecipes.Recipe> list = recipes();
        if (button == 0 && open && selected < 0 && mx >= px && mx < px + PANEL_W) {
            for (int row = 0; row < ROWS; row++) {
                int idx = scroll + row;
                if (idx >= list.size()) break;
                int ry = topPos + 16 + row * ROW_H;
                if (my >= ry && my < ry + ROW_H) {
                    selected = idx;
                    sync();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (open && selected < 0 && mx >= panelX() && mx < panelX() + PANEL_W) {
            scroll -= (int) Math.signum(dy);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
