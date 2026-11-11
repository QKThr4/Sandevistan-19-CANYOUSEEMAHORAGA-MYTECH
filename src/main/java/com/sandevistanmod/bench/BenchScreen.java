package com.sandevistanmod.bench;

import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.BenchFillPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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

    public BenchScreen(BenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private List<BenchRecipes.Recipe> recipes() {
        return BenchRecipes.of(menu.kind());
    }

    private int panelX() {
        return leftPos + imageWidth + 4;
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

        // livro de receitas
        int px = panelX();
        gg.fill(px - 1, topPos - 1, px + PANEL_W + 1, topPos + imageHeight + 1, 0xFF0B1116);
        gg.fill(px, topPos, px + PANEL_W, topPos + imageHeight, 0xFF26323C);
        gg.drawString(font, Tx.t(ChatFormatting.AQUA, "bench.book"), px + 4, topPos + 4, 0xFFFFFF, false);
        List<BenchRecipes.Recipe> list = recipes();
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
        if (button == 0 && mx >= px && mx < px + PANEL_W) {
            for (int row = 0; row < ROWS; row++) {
                int idx = scroll + row;
                if (idx >= list.size()) break;
                int ry = topPos + 16 + row * ROW_H;
                if (my >= ry && my < ry + ROW_H) {
                    PacketDistributor.sendToServer(new BenchFillPayload(idx));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= panelX() && mx < panelX() + PANEL_W) {
            scroll -= (int) Math.signum(dy);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
