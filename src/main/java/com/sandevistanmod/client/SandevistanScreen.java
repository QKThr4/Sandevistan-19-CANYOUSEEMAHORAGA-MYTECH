package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.ActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Aba J: implantes do jogador (so visual), Carga Cibernetica, chips, botoes do HUD e a aba de efeitos de cada implante. */
public class SandevistanScreen extends Screen {
    private Button armBtn;
    private Button tab0;
    private Button tab1;
    private Button tab2;
    private final java.util.List<Button> page0 = new java.util.ArrayList<>();
    /** 0 implantes, 1 efeitos, 2 energia. */
    private static int tab;

    public SandevistanScreen() {
        super(Tx.t("implants.title"));
    }

    private int cx() {
        return this.width / 2;
    }

    private int y0() {
        return this.height / 2 - 102;
    }

    private int lx() {
        return cx() - 100;
    }

    @Override
    protected void init() {
        page0.clear();
        int bx = cx() + 16;
        int y = y0();
        tab0 = addRenderableWidget(Button.builder(Tx.t("tab.implants"), b -> setTab(0))
                .bounds(cx() - 137, y - 22, 90, 18).build());
        tab1 = addRenderableWidget(Button.builder(Tx.t("tab.effects"), b -> setTab(1))
                .bounds(cx() - 45, y - 22, 90, 18).build());
        tab2 = addRenderableWidget(Button.builder(Tx.t("tab.energy"), b -> setTab(2))
                .bounds(cx() + 47, y - 22, 90, 18).build());
        armBtn = addRenderableWidget(Button.builder(Tx.t("btn.visibility"), b -> Minecraft.getInstance().setScreen(new VisibilityScreen(this)))
                .bounds(bx, y + 104, 176, 20).build());
        page0.add(armBtn);
        page0.add(addRenderableWidget(Button.builder(Tx.t("btn.layout"),
                        b -> Minecraft.getInstance().setScreen(new HudLayoutScreen(this)))
                .bounds(bx, y + 126, 176, 20).build()));
        page0.add(addRenderableWidget(Button.builder(musicLabel(), b -> {
                    ClientLayout.nextMusic();
                    b.setMessage(musicLabel());
                })
                .bounds(bx, y + 148, 176, 20).build()));
        page0.add(addRenderableWidget(Button.builder(Tx.t("btn.close"), b -> onClose())
                .bounds(bx, y + 170, 176, 20).build()));
        updateWidgets();
    }

    private void setTab(int t) {
        tab = t;
        updateWidgets();
    }

    private void updateWidgets() {
        for (Button b : page0) b.visible = tab == 0;
        if (armBtn != null) armBtn.visible = tab == 0 && ClientData.DATA.arms() + ClientData.DATA.legs() > 0;
        if (tab0 != null) tab0.active = tab != 0;
        if (tab1 != null) tab1.active = tab != 1;
        if (tab2 != null) tab2.active = tab != 2;
    }

    private static Component musicLabel() {
        return Tx.t("btn.music", Tx.t("music." + ClientLayout.music));
    }

    @Override
    public void tick() {
        updateWidgets();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.SCREEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- posicoes dos encaixes ----
    private int slotX(int slot) {
        return switch (slot) {
            case SandevistanData.SLOT_BACK, SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_BONE,
                    SandevistanData.SLOT_LEG_R -> lx() - 80;
            case SandevistanData.SLOT_HEART -> lx() - 9;
            default -> lx() + 62;
        };
    }

    private int slotY(int slot) {
        return y0() + switch (slot) {
            case SandevistanData.SLOT_BACK, SandevistanData.SLOT_NEURAL -> 26;
            case SandevistanData.SLOT_ARM_R, SandevistanData.SLOT_ARM_L -> 56;
            case SandevistanData.SLOT_BONE, SandevistanData.SLOT_NERVE -> 86;
            default -> 116;
        };
    }

    private int chipX() {
        return cx() + 16;
    }

    private int chipY(int i) {
        return y0() + 58 + i * 22;
    }

    private static boolean in(double mx, double my, int x, int y) {
        return mx >= x && mx < x + 18 && my >= y && my < y + 18;
    }

    private static ItemStack slotStack(int slot) {
        return new ItemStack(SandevistanLogic.implantItem(slot, ClientData.DATA.variant, ClientData.DATA.tierOf(slot)));
    }

    private static ItemStack chipStack(SandevistanData d, int i) {
        if (d.chip[i] == 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(ModRegistries.chipItem(d.chip[i], d.chipTier[i]));
        s.setDamageValue(d.chipDmg[i]);
        return s;
    }

    private int batX() {
        return cx() - 9;
    }

    private int batY() {
        return y0() + 44;
    }

    /** Onde comecam os quadradinhos das baterias do inventario (clicar instala na hora; a atual volta ao inventario). */
    private int pickX() {
        return batX() + 34;
    }

    /** Indices do inventario (0 a 35) que tem uma bateria, no maximo 8. */
    private java.util.List<Integer> batteriesInInventory() {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return out;
        java.util.List<ItemStack> items = mc.player.getInventory().items;
        for (int i = 0; i < items.size() && out.size() < 8; i++) {
            if (items.get(i).getItem() instanceof com.sandevistanmod.energy.BatteryItem) out.add(i);
        }
        return out;
    }

    private int extX(int i) {
        return cx() - 132 + i * 24;
    }

    private int extY() {
        return y0() + 142;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && tab == 2) {
            java.util.List<Integer> inv = batteriesInInventory();
            for (int k = 0; k < inv.size(); k++) {
                if (in(mx, my, pickX() + k * 20, batY())) {
                    PacketDistributor.sendToServer(new ActionPayload(30 + inv.get(k)));
                    return true;
                }
            }
        }
        if (button == 0 && tab == 2 && ClientData.DATA.batType != 0 && in(mx, my, batX(), batY())) {
            PacketDistributor.sendToServer(new ActionPayload(6));
            return true;
        }
        if (button == 0 && tab == 0) {
            for (int i = 0; i < 2; i++) {
                if (ClientData.DATA.chip[i] != 0 && in(mx, my, chipX(), chipY(i))) {
                    PacketDistributor.sendToServer(new ActionPayload(2 + i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static final String[] SLOT_KEYS = {"slot.back", "slot.armr", "slot.arml", "slot.neural", "slot.bone", "slot.nerve", "slot.heart", "slot.legr", "slot.legl"};

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = cx();
        int y0 = y0();
        gg.fill(cx - 200, y0, cx + 200, y0 + 204, 0xC0101410);
        gg.fill(cx - 200, y0, cx + 200, y0 + 2, 0xFF39FF14);
        if (tab == 1) {
            drawEffects(gg, cx, y0);
            return;
        }
        if (tab == 2) {
            drawEnergy(gg, cx, y0);
            return;
        }
        SandevistanData d = ClientData.DATA;
        gg.fill(cx + 5, y0 + 8, cx + 6, y0 + 196, 0x40FFFFFF);
        gg.drawCenteredString(this.font, Tx.t("implants.title"), lx(), y0 + 7, 0x39FF14);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gg, lx() - 45, y0 + 18, lx() + 45, y0 + 112, 40,
                    0.0625f, mouseX, mouseY, mc.player);
        }
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            int x = slotX(s);
            int y = slotY(s);
            boolean on = d.has(s);
            gg.fill(x - 1, y - 1, x + 19, y + 19, on ? 0xFF39FF14 : 0xFF666666);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (on) gg.renderItem(slotStack(s), x + 1, y + 1);
            gg.drawCenteredString(this.font, Tx.t(SLOT_KEYS[s]), x + 9, y + 22, on ? 0xCCCCCC : 0x888888);
        }

        // Carga Cibernetica
        float cap = d.capacity();
        int bx = cx + 16;
        gg.drawCenteredString(this.font, Tx.t("load.title", Math.round(cap)), cx + 104, y0 + 7,
                cap >= d.loadThreshold() ? 0xFFAA33 : 0x88FFAA);
        ClientOverlay.bar(gg, bx, y0 + 20, 176, 9, cap / 100f, ClientOverlay.loadColor(cap));
        gg.drawWordWrap(this.font, Tx.t("load.hint", (int) d.loadThreshold()), bx, y0 + 34, 176, 0x888888);

        // chips
        for (int i = 0; i < 2; i++) {
            int x = chipX();
            int y = chipY(i);
            boolean has = d.chip[i] != 0;
            gg.fill(x - 1, y - 1, x + 19, y + 19, has ? 0xFF55DDFF : 0xFF666666);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (has) {
                ItemStack st = chipStack(d, i);
                gg.renderItem(st, x + 1, y + 1);
                gg.renderItemDecorations(this.font, st, x + 1, y + 1);
                gg.drawString(this.font, st.getHoverName(), x + 24, y + 1, 0xFFFFFF);
                if (d.chipLoad[i] > 0f) {
                    gg.drawString(this.font, Tx.t("chip.dl", (int) Math.ceil(d.chipLoad[i])), x + 24, y + 10, 0x55DDFF);
                } else if (d.chipDmg[i] >= SandevistanData.chipMax(d.chip[i], d.chipTier[i])) {
                    gg.drawString(this.font, Tx.t("chip.brokenshort"), x + 24, y + 10, 0xFF5555);
                } else {
                    gg.drawString(this.font, Tx.t("chip.working"), x + 24, y + 10, 0x55FF55);
                }
            } else {
                gg.drawString(this.font, d.neural ? Tx.t("chip.none") : Tx.t("chip.noneural"), x + 24, y + 5, 0x888888);
            }
        }
    }

    /** Aba de energia: o encaixe da bateria e a barra de Energia. */
    private void drawEnergy(GuiGraphics gg, int cx, int y0) {
        SandevistanData d = ClientData.DATA;
        gg.drawCenteredString(this.font, Tx.t("tab.energy.title"), cx, y0 + 10, 0x33CCFF);
        int x = batX();
        int y = batY();
        boolean has = d.batType != 0;
        gg.fill(x - 1, y - 1, x + 19, y + 19, has ? 0xFF33CCFF : 0xFF666666);
        gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
        if (has) gg.renderItem(SandevistanLogic.batteryStack(d), x + 1, y + 1);
        gg.drawCenteredString(this.font, Tx.t("energy.slot"), cx, y + 22, has ? 0xCCCCCC : 0x888888);
        java.util.List<Integer> inv = batteriesInInventory();
        gg.drawString(this.font, Tx.t(inv.isEmpty() ? "energy.pick.none" : "energy.pick"), pickX(), y - 11, 0x88AACC, false);
        Minecraft mcp = Minecraft.getInstance();
        for (int k = 0; k < inv.size(); k++) {
            int px = pickX() + k * 20;
            gg.fill(px - 1, y - 1, px + 19, y + 19, 0xFF4488AA);
            gg.fill(px, y, px + 18, y + 18, 0xFF1A1E22);
            ItemStack st = mcp.player.getInventory().items.get(inv.get(k));
            gg.renderItem(st, px + 1, y + 1);
            gg.renderItemDecorations(this.font, st, px + 1, y + 1);
        }
        int cap = d.batCap();
        float frac = cap > 0 ? d.batEnergy / (float) cap : 0f;
        ClientOverlay.bar(gg, cx - 88, y + 40, 176, 9, frac, 0xFF33CCFF);
        gg.drawCenteredString(this.font, has ? Tx.t("energy.bar", d.batEnergy, cap, Math.round(frac * 100f)) : Tx.t("energy.nobattery"),
                cx, y + 53, has ? 0x66DDFF : 0x888888);
        gg.drawWordWrap(this.font, Tx.t("energy.hint"), cx - 150, y + 66, 300, 0x888888);
        gg.drawCenteredString(this.font, Tx.t("ext.title"), cx, extY() - 14, 0x33CCFF);
        for (int i = 0; i < SandevistanData.EXT_COUNT; i++) {
            int ex = extX(i);
            boolean on = d.ext[i] != 0;
            gg.fill(ex - 1, extY() - 1, ex + 19, extY() + 19, on ? 0xFF33CCFF : 0xFF666666);
            gg.fill(ex, extY(), ex + 18, extY() + 18, 0xFF1A1E22);
            if (on) gg.renderItem(new ItemStack(ModRegistries.extItem(i, d.ext[i], d.eyeType)), ex + 1, extY() + 1);
        }
        gg.drawCenteredString(this.font, Tx.t("ext.keys"), cx, extY() + 36, 0x777777);
    }

    /** Aba de efeitos: o que cada implante instalado esta causando no corpo do jogador. */
    private void drawEffects(GuiGraphics gg, int cx, int y0) {
        SandevistanData d = ClientData.DATA;
        int x = cx - 190;
        int y = y0 + 10;
        gg.drawCenteredString(this.font, Tx.t("tab.effects.title"), cx, y, 0x39FF14);
        y += 16;
        if (!d.hasAny()) {
            gg.drawCenteredString(this.font, Tx.t("eff.none"), cx, y + 30, 0x999999);
            return;
        }
        if (d.implanted) {
            var v = d.variant();
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_BACK, d.variant).getDescription(),
                    Tx.t("eff.sandevistan", v.slow, (int) v.seconds, Math.round(v.dodge * 100f)));
            y += 11;
            if (d.variant == 3) {
                effect(gg, x + 8, y, Component.empty(), Tx.t("variant.tip.zetatech"));
                y += 11;
            } else if (d.variant == 4) {
                effect(gg, x + 8, y, Component.empty(), Tx.t("variant.tip.qiant"));
                y += 11;
            }
        }
        if (d.arms() > 0) {
            double dmg = (d.armR ? SandevistanData.at(SandevistanData.ARM_DAMAGE, d.tierOf(SandevistanData.SLOT_ARM_R)) : 0.0)
                    + (d.armL ? SandevistanData.at(SandevistanData.ARM_DAMAGE, d.tierOf(SandevistanData.SLOT_ARM_L)) : 0.0);
            int top = Math.max(d.armR ? d.tierOf(SandevistanData.SLOT_ARM_R) : 1, d.armL ? d.tierOf(SandevistanData.SLOT_ARM_L) : 1);
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_ARM_R, 0, top).getDescription(),
                    Tx.t("eff.arm", d.arms(), SandevistanData.num(dmg)));
            y += 11;
        }
        if (d.heart) {
            int t = d.tierOf(SandevistanData.SLOT_HEART);
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_HEART, 0, t).getDescription(),
                    Tx.t("eff.heart", SandevistanData.statArgs(SandevistanData.SLOT_HEART, t)));
            y += 11;
        }
        if (d.legs() > 0) {
            int tr = d.tierOf(SandevistanData.SLOT_LEG_R);
            int tl = d.tierOf(SandevistanData.SLOT_LEG_L);
            double sp = (d.legR ? SandevistanData.at(SandevistanData.LEG_SPEED, tr) : 0.0) + (d.legL ? SandevistanData.at(SandevistanData.LEG_SPEED, tl) : 0.0);
            double fl = (d.legR ? SandevistanData.at(SandevistanData.LEG_FALL, tr) : 0.0) + (d.legL ? SandevistanData.at(SandevistanData.LEG_FALL, tl) : 0.0);
            int top = Math.max(d.legR ? tr : 1, d.legL ? tl : 1);
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_LEG_R, 0, top).getDescription(),
                    Tx.t("eff.leg", d.legs(), SandevistanData.pct(sp), SandevistanData.pct(fl)));
            y += 11;
        }
        if (d.bone) {
            int t = d.tierOf(SandevistanData.SLOT_BONE);
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_BONE, 0, t).getDescription(),
                    Tx.t("eff.bone", SandevistanData.statArgs(SandevistanData.SLOT_BONE, t)));
            y += 11;
        }
        if (d.nerve) {
            int t = d.tierOf(SandevistanData.SLOT_NERVE);
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_NERVE, 0, t).getDescription(),
                    Tx.t("eff.nerve", SandevistanData.statArgs(SandevistanData.SLOT_NERVE, t)));
            y += 11;
        }
        if (d.neural) {
            effect(gg, x, y, SandevistanLogic.implantItem(SandevistanData.SLOT_NEURAL, 0).getDescription(), Tx.t("eff.neural"));
            y += 11;
        }
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] == 0) continue;
            String[] keys = {"tooltip.chip.martial", "tooltip.chip.pain", "tooltip.chip.mining", "tooltip.chip.walker", "tooltip.chip.ram"};
            int rb = (int) SandevistanData.ramBonus(d.chipTier[i]);
            Component status = d.chipLoad[i] > 0f ? Tx.t("chip.dl", (int) Math.ceil(d.chipLoad[i]))
                    : d.chipDmg[i] >= SandevistanData.chipMax(d.chip[i], d.chipTier[i]) ? Tx.t("chip.brokenshort") : Tx.t("chip.working");
            effect(gg, x, y, chipStack(d, i).getHoverName(),
                    Component.empty().append(Tx.t(keys[d.chip[i] - 1], rb, 70 + rb)).append(Component.literal(" (")).append(status)
                            .append(Component.literal(")")));
            y += 11;
        }
        y = y0 + 168;
        float cap = d.capacity();
        gg.drawString(this.font, Tx.t("load.title", Math.round(cap)), x, y, cap >= d.loadThreshold() ? 0xFFAA33 : 0x88FFAA);
        if (d.chipMind() > 0f) {
            gg.drawString(this.font, Tx.t("eff.mind", (int) d.chipMind()), x, y + 11, 0x55AAFF);
        }
    }

    private void effect(GuiGraphics gg, int x, int y, Component name, Component desc) {
        gg.drawString(this.font, name, x, y, 0x55FF99);
        int w = this.font.width(name);
        gg.drawString(this.font, desc, x + w + (w > 0 ? 6 : 0), y, 0xCCCCCC);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        if (tab == 2) {
            SandevistanData dd = ClientData.DATA;
            if (dd.batType != 0 && in(mouseX, mouseY, batX(), batY())) gg.renderTooltip(this.font, SandevistanLogic.batteryStack(dd), mouseX, mouseY);
            java.util.List<Integer> inv = batteriesInInventory();
            for (int k = 0; k < inv.size(); k++) {
                if (in(mouseX, mouseY, pickX() + k * 20, batY())) {
                    gg.renderTooltip(this.font, minecraft.player.getInventory().items.get(inv.get(k)), mouseX, mouseY);
                    return;
                }
            }
            for (int i = 0; i < SandevistanData.EXT_COUNT; i++) {
                if (in(mouseX, mouseY, extX(i), extY())) {
                    if (dd.ext[i] != 0) gg.renderTooltip(this.font, new ItemStack(ModRegistries.extItem(i, dd.ext[i], dd.eyeType)), mouseX, mouseY);
                    else gg.renderTooltip(this.font, Tx.t("ext.slot." + i), mouseX, mouseY);
                }
            }
            return;
        }
        if (tab != 0) return;
        SandevistanData d = ClientData.DATA;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (d.has(s) && in(mouseX, mouseY, slotX(s), slotY(s))) {
                gg.renderTooltip(this.font, slotStack(s), mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (d.chip[i] != 0 && in(mouseX, mouseY, chipX(), chipY(i))) {
                gg.renderTooltip(this.font, chipStack(d, i), mouseX, mouseY);
                return;
            }
        }
    }
}
