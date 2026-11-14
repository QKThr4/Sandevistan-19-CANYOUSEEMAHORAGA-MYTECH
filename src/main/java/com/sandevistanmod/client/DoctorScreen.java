package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.SandevistanVariant;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.DoctorActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela do Ripperdoc: menu (Loja / Implantes), encaixe do Sandevistan na silhueta e loja de remedios. */
public class DoctorScreen extends Screen {
    private static final int MENU = 0;
    private static final int IMPLANTS = 1;
    private static final int SHOP = 2;

    private int mode = MENU;
    /** Implante encaixado no slot (ainda nao implantado) e encaixe selecionado (-1 nenhum). */
    private static final int TOTAL = SandevistanData.SLOT_COUNT + SandevistanData.EXT_COUNT;
    private final boolean[] placed = new boolean[TOTAL];
    private int sel = -1;
    /** Modelo do Sandevistan colocado no encaixe das costas (0 a 4). */
    private int placedVar;
    /** Tier (1 a 3) do implante colocado em cada encaixe (vem do item carregado). */
    private final int[] placedTier = newTiers();

    private static int[] newTiers() {
        int[] a = new int[TOTAL];
        java.util.Arrays.fill(a, 1);
        return a;
    }
    /** Tipo do par de olhos colocado no encaixe de olhos (1 a 4). */
    private int placedEye = 1;
    private ItemStack carried = ItemStack.EMPTY;
    private Button implantBtn;
    private Button removeBtn;
    private Button repairBtn;
    private Button buy1;
    private Button buy2;
    private Button buy3;

    public DoctorScreen() {
        super(Tx.t("doc.name"));
    }

    private static int count(net.minecraft.world.item.Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) if (s.is(item)) n += s.getCount();
        for (ItemStack s : mc.player.getInventory().offhand) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static boolean creative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getAbilities().instabuild;
    }

    private static boolean isExt(int s) {
        return s >= SandevistanData.SLOT_COUNT;
    }

    private static int extIdx(int s) {
        return s - SandevistanData.SLOT_COUNT;
    }

    /** O encaixe (comum ou extra) ja tem implante instalado? */
    private static boolean owned(int s) {
        return isExt(s) ? ClientData.DATA.ext[extIdx(s)] != 0 : ClientData.DATA.has(s);
    }

    /** Item que o encaixe consome agora (com o tier colocado). */
    private net.minecraft.world.item.Item itemFor(int slot) {
        if (isExt(slot)) return ModRegistries.extItem(extIdx(slot), placedTier[slot], placedEye);
        return SandevistanLogic.implantItem(slot, slot == SandevistanData.SLOT_BACK ? sandVar() : 0, placedTier[slot]);
    }

    /** Tiers colocados, 2 bits por encaixe (0 = tier I), para o servidor. */
    private int packedTiers() {
        int packed = 0;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (SandevistanData.slotHasTiers(s)) packed |= (placedTier[s] - 1) << (2 * s);
        }
        return packed;
    }

    /** Encaixes com implante colocado e ainda nao instalado (um bit por encaixe). */
    private int pendingMask() {
        int mask = 0;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if (placed[s] && !ClientData.DATA.has(s)) mask |= 1 << s;
        }
        return mask;
    }

    /** Encaixes extras com implante colocado e ainda nao instalado. */
    private int pendingExt() {
        int n = 0;
        for (int e = 0; e < SandevistanData.EXT_COUNT; e++) {
            if (placed[SandevistanData.SLOT_COUNT + e] && ClientData.DATA.ext[e] == 0) n++;
        }
        return n;
    }

    private int pendingCount() {
        return Integer.bitCount(pendingMask()) + pendingExt();
    }

    /** Esmeraldas que a cirurgia de tudo que esta encaixado custa. */
    private int totalCost() {
        int sum = Integer.bitCount(pendingMask()) * SandevistanLogic.CLINIC_EMERALDS;
        for (int e = 0; e < SandevistanData.EXT_COUNT; e++) {
            int s = SandevistanData.SLOT_COUNT + e;
            if (placed[s] && ClientData.DATA.ext[e] == 0) sum += SandevistanData.extCost(e, placedTier[s], placedEye);
        }
        return sum;
    }

    /** Carga Cibernetica que todos os implantes encaixados somam. */
    private float pendingLoad() {
        float sum = 0f;
        int mask = pendingMask();
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) {
            if ((mask & (1 << s)) == 0) continue;
            sum += s == SandevistanData.SLOT_BACK ? SandevistanVariant.of(placedVar).load : SandevistanData.loadOf(s, placedTier[s]);
        }
        for (int e = 0; e < SandevistanData.EXT_COUNT; e++) {
            int s = SandevistanData.SLOT_COUNT + e;
            if (placed[s] && ClientData.DATA.ext[e] == 0) sum += SandevistanData.extLoad(e, placedTier[s], placedEye);
        }
        return sum;
    }

    private int sandVar() {
        return placedVar;
    }

    private boolean fits(int slot, ItemStack st) {
        if (st.isEmpty()) return false;
        if (isExt(slot)) {
            if (!(st.getItem() instanceof com.sandevistanmod.logic.ExtItem x)) return false;
            int e = extIdx(slot);
            if (e == SandevistanData.EXT_BOOT_R || e == SandevistanData.EXT_BOOT_L) {
                return x.slot() == SandevistanData.EXT_BOOT_R;
            }
            if (e == SandevistanData.EXT_MANTIS_R || e == SandevistanData.EXT_MANTIS_L) {
                return x.slot() == SandevistanData.EXT_MANTIS_R;
            }
            return x.slot() == e;
        }
        if (slot == SandevistanData.SLOT_BACK) return SandevistanLogic.variantOf(st) >= 0;
        if (!SandevistanData.slotHasTiers(slot)) return st.is(SandevistanLogic.implantItem(slot));
        for (int t = 1; t <= SandevistanData.MAX_TIER; t++) {
            if (st.is(SandevistanLogic.implantItem(slot, 0, t))) return true;
        }
        return false;
    }

    private boolean freeSlot(int slot) {
        return !owned(slot) && !placed[slot];
    }

    private boolean fitsAnyFree(ItemStack st) {
        for (int s = 0; s < TOTAL; s++) if (fits(s, st) && freeSlot(s)) return true;
        return false;
    }

    private int panelW() {
        return mode == IMPLANTS ? 360 : 270;
    }

    private int px() {
        return this.width / 2 - panelW() / 2;
    }

    private int panelH() {
        return mode == IMPLANTS ? 326 : 200;
    }

    private int py() {
        return Math.max(2, (this.height - panelH()) / 2);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = py();
        implantBtn = null;
        removeBtn = null;
        repairBtn = null;
        buy1 = null;
        buy2 = null;
        buy3 = null;
        if (mode == MENU) {
            addRenderableWidget(Button.builder(Tx.t("doc.menu.shop"), b -> setMode(SHOP))
                    .bounds(cx - 80, top + 70, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("doc.menu.implants"), b -> setMode(IMPLANTS))
                    .bounds(cx - 80, top + 98, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("doc.menu.quests"), b ->
                            Minecraft.getInstance().setScreen(new QuestScreen()))
                    .bounds(cx - 80, top + 126, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("btn.close"), b -> onClose())
                    .bounds(cx - 80, top + 160, 160, 20).build());
        } else if (mode == IMPLANTS) {
            implantBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.implant"), b -> {
                        // implanta de uma vez TODOS os implantes que estao encaixados
                        int mask = pendingMask();
                        if (mask != 0) PacketDistributor.sendToServer(new DoctorActionPayload(100 + (packedTiers() << 12) + (mask << 3) + placedVar));
                        for (int e = 0; e < SandevistanData.EXT_COUNT; e++) {
                            int s = SandevistanData.SLOT_COUNT + e;
                            if (placed[s] && ClientData.DATA.ext[e] == 0) {
                                PacketDistributor.sendToServer(new DoctorActionPayload(
                                        com.sandevistanmod.logic.ExtLogic.installCode(e, placedTier[s], e == SandevistanData.EXT_EYE ? placedEye : 0)));
                            }
                        }
                    })
                    .bounds(cx - 37, top + 212, 74, 20).build());
            removeBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.remove"), b -> {
                        if (sel >= 0 && isExt(sel)) {
                            PacketDistributor.sendToServer(new DoctorActionPayload(com.sandevistanmod.logic.ExtLogic.removeCode(extIdx(sel))));
                        } else if (sel >= 0) {
                            PacketDistributor.sendToServer(new DoctorActionPayload(20 + sel));
                        }
                    })
                    .bounds(cx - 50, top + 212, 100, 20).build());
            repairBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.repair"), b -> {
                        int side = mantisSide(sel);
                        if (side >= 0) PacketDistributor.sendToServer(new DoctorActionPayload(com.sandevistanmod.logic.ExtLogic.repairCode(side)));
                    })
                    .bounds(cx + 54, top + 212, 80, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        } else {
            buy1 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(3)))
                    .bounds(cx + 50, top + 62, 60, 20).build());
            buy2 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(4)))
                    .bounds(cx + 50, top + 98, 60, 20).build());
            buy3 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(5)))
                    .bounds(cx + 50, top + 134, 60, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        }
        refresh();
    }

    /** 0 direita, 1 esquerda, -1 se o encaixe nao e de Lamina Mantis. */
    private int mantisSide(int s) {
        if (s < 0 || !isExt(s)) return -1;
        int e = extIdx(s);
        return e == SandevistanData.EXT_MANTIS_R ? 0 : e == SandevistanData.EXT_MANTIS_L ? 1 : -1;
    }

    private net.minecraft.world.item.ItemStack[] repairCostOf(int side) {
        int slot = side == 0 ? SandevistanData.EXT_MANTIS_R : SandevistanData.EXT_MANTIS_L;
        return com.sandevistanmod.logic.ExtLogic.repairCost(ClientData.DATA.extTier(slot));
    }

    private boolean canAffordRepair(int side) {
        for (ItemStack c : repairCostOf(side)) if (count(c.getItem()) < c.getCount()) return false;
        return true;
    }

    private void setMode(int m) {
        mode = m;
        carrying = false;
        carried = ItemStack.EMPTY;
        rebuildWidgets();
    }

    private void refresh() {
        SandevistanData d = ClientData.DATA;
        if (implantBtn != null) {
            implantBtn.visible = pendingCount() > 0;
            implantBtn.active = creative() || count(Items.EMERALD) >= totalCost();
        }
        if (removeBtn != null) {
            removeBtn.visible = sel >= 0 && owned(sel);
            removeBtn.active = sel < 0 || isExt(sel) || (sel == SandevistanData.SLOT_BACK ? !d.active
                    : sel != SandevistanData.SLOT_NEURAL || d.chips() == 0);
        }
        if (repairBtn != null) {
            int side = mantisSide(sel);
            repairBtn.visible = side >= 0 && owned(sel);
            repairBtn.active = side >= 0 && d.mantisDur[side] < SandevistanData.MANTIS_MAX && (creative() || canAffordRepair(side));
        }
        if (buy1 != null) buy1.active = creative() || count(Items.EMERALD) >= SandevistanLogic.STABILIZER_PRICE;
        if (buy2 != null) buy2.active = creative() || count(Items.EMERALD) >= SandevistanLogic.IMMUNO_PRICE;
        if (buy3 != null) buy3.active = creative() || count(Items.EMERALD) >= SandevistanLogic.PILL_PRICE;
    }

    @Override
    public void tick() {
        SandevistanData d = ClientData.DATA;
        // cada implante encaixado precisa do seu item no inventario (dois bracos ou pernas do mesmo tier = dois itens)
        java.util.Map<net.minecraft.world.item.Item, Integer> used = new java.util.HashMap<>();
        for (int s = 0; s < TOTAL; s++) {
            if (!placed[s]) continue;
            if (owned(s)) {
                placed[s] = false;
                continue;
            }
            if (creative()) continue;
            net.minecraft.world.item.Item it = itemFor(s);
            int u = used.getOrDefault(it, 0);
            if (count(it) <= u) placed[s] = false;
            else used.put(it, u + 1);
        }
        if (sel >= 0 && !placed[sel] && !owned(sel)) sel = -1;
        if (!carried.isEmpty() && !fitsAnyFree(carried)) {
            carrying = false;
            carried = ItemStack.EMPTY;
        }
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- encaixes e inventario do jogador (arrastar o implante ate o encaixe) ----
    private int slotX(int slot) {
        return ImplantLayout.x(slot, this.width / 2);
    }

    private int slotY(int slot) {
        return ImplantLayout.y(slot, py());
    }

    private int slotAt(double mx, double my) {
        return ImplantLayout.slotAt(mx, my, this.width / 2, py());
    }

    private int invX() {
        return this.width / 2 - 81;
    }

    private int invY() {
        return py() + 242;
    }

    /** Indice do inventario (0 a 8 hotbar, 9 a 35 principal) sob o mouse, ou -1. */
    private int invIndexAt(double mx, double my) {
        int ix = (int) Math.floor((mx - invX()) / 18.0);
        if (ix < 0 || ix > 8 || mx < invX()) return -1;
        double ry = my - invY();
        if (ry >= 0 && ry < 54) return 9 + (int) (ry / 18) * 9 + ix;
        if (ry >= 58 && ry < 76) return ix;
        return -1;
    }

    private ItemStack invStack(int idx) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null || idx < 0 ? ItemStack.EMPTY : mc.player.getInventory().getItem(idx);
    }

    private boolean carrying;
    private double pressX;
    private double pressY;
    private boolean dragged;

    /** Solta o item carregado no encaixe sob o mouse, se ele couber e estiver livre. */
    private void dropOn(double mx, double my) {
        int s = slotAt(mx, my);
        if (s >= 0 && fits(s, carried) && freeSlot(s)) {
            if (s == SandevistanData.SLOT_BACK) placedVar = SandevistanLogic.variantOf(carried);
            if (isExt(s) && carried.getItem() instanceof com.sandevistanmod.logic.ExtItem x) {
                placedTier[s] = x.tier();
                if (extIdx(s) == SandevistanData.EXT_EYE) placedEye = x.eyeType();
            } else {
                placedTier[s] = !isExt(s) && SandevistanData.slotHasTiers(s) ? SandevistanLogic.tierOfStack(carried) : 1;
            }
            placed[s] = true;
            sel = s;
        }
        carrying = false;
        carried = ItemStack.EMPTY;
        refresh();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mode == IMPLANTS && button == 0) {
            if (carrying) {
                dropOn(mx, my);
                return true;
            }
            int s = slotAt(mx, my);
            if (s >= 0) {
                if (placed[s]) {
                    placed[s] = false;
                    if (sel == s) sel = -1;
                } else if (owned(s)) {
                    sel = s;
                }
                refresh();
                return true;
            }
            int idx = invIndexAt(mx, my);
            if (idx >= 0 && fitsAnyFree(invStack(idx))) {
                carrying = true;
                carried = invStack(idx).copy();
                pressX = mx;
                pressY = my;
                dragged = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (carrying && Math.abs(mx - pressX) + Math.abs(my - pressY) > 4) dragged = true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (mode == IMPLANTS && carrying && button == 0 && dragged) {
            dropOn(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int x0 = px();
        int top = py();
        int h = panelH();
        gg.fill(x0, top, x0 + panelW(), top + h, 0xD0101214);
        gg.fill(x0, top, x0 + panelW(), top + 2, 0xFFFF2040);
        gg.drawCenteredString(this.font, Tx.t("doc.name"), cx, top + 8, 0xFF5566);

        if (mode == MENU) {
            gg.drawCenteredString(this.font, Tx.t("doc.menu.title"), cx, top + 40, 0xDDDDDD);
        } else if (mode == IMPLANTS) {
            drawImplants(gg, cx, top, mouseX, mouseY);
        } else {
            drawShop(gg, cx, top);
        }
    }

    private static final String[] SLOT_KEYS = {"slot.back", "slot.armr", "slot.arml", "slot.neural", "slot.bone", "slot.nerve", "slot.heart", "slot.legr", "slot.legl"};

    private int shownTier(int slot) {
        if (isExt(slot)) return ClientData.DATA.ext[extIdx(slot)] != 0 ? ClientData.DATA.ext[extIdx(slot)] : placedTier[slot];
        return ClientData.DATA.has(slot) ? ClientData.DATA.tierOf(slot) : placedTier[slot];
    }

    private int shownEye() {
        return ClientData.DATA.ext[SandevistanData.EXT_EYE] != 0 ? ClientData.DATA.eyeType : placedEye;
    }

    private ItemStack shownStack(int s) {
        if (isExt(s)) return new ItemStack(ModRegistries.extItem(extIdx(s), shownTier(s), shownEye()));
        return new ItemStack(SandevistanLogic.implantItem(s, shownVar(s), shownTier(s)));
    }

    private int shownVar(int slot) {
        if (slot != SandevistanData.SLOT_BACK) return 0;
        return ClientData.DATA.has(slot) ? ClientData.DATA.variant : placedVar;
    }

    private void drawImplants(GuiGraphics gg, int cx, int top, int mouseX, int mouseY) {
        SandevistanData d = ClientData.DATA;
        Minecraft mc = Minecraft.getInstance();
        // o proprio jogador (o mesmo modelo do inventario)
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gg, cx - 40, top + 20, cx + 40, top + 98, 31, 0.0625f,
                    mouseX, mouseY, mc.player);
        }
        int hover = carrying ? slotAt(mouseX, mouseY) : -1;
        for (int s = 0; s < TOTAL; s++) {
            int x = slotX(s);
            int y = slotY(s);
            boolean on = owned(s) || placed[s];
            boolean hot = hover == s && fits(s, carried) && freeSlot(s);
            int border = s == sel ? 0xFFFFFFFF : on || hot ? 0xFF39FF14 : 0xFF777777;
            gg.fill(x - 1, y - 1, x + 19, y + 19, border);
            gg.fill(x, y, x + 18, y + 18, 0xFF1A1E22);
            if (on) gg.renderItem(shownStack(s), x + 1, y + 1);
        }
        for (int g = 0; g < ImplantLayout.GROUPS.length; g++) {
            gg.drawCenteredString(this.font, Tx.t(ImplantLayout.GROUPS[g].key()), ImplantLayout.labelX(g, cx), ImplantLayout.labelY(g, top), 0xAAAAAA);
        }
        boolean preview = pendingCount() > 0;
        int ms = mantisSide(sel);
        if (!preview && ms >= 0 && owned(sel)) {
            int dur = d.mantisDur[ms];
            gg.drawCenteredString(this.font, Tx.t("mantis.dur", dur, SandevistanData.MANTIS_MAX), cx, top + 176,
                    dur <= 0 ? 0xFF5555 : dur < 250 ? 0xFFAA33 : 0x55FF55);
            if (dur < SandevistanData.MANTIS_MAX) {
                gg.drawString(this.font, Tx.t("mantis.cost"), cx - 172, top + 216, 0xAAAAAA);
                int ix = cx - 172 + this.font.width(Tx.t("mantis.cost")) + 4;
                for (ItemStack c : repairCostOf(ms)) {
                    gg.renderItem(c, ix, top + 211);
                    boolean ok = creative() || count(c.getItem()) >= c.getCount();
                    gg.drawString(this.font, "x" + c.getCount(), ix + 17, top + 216, ok ? 0x55FF55 : 0xFF5555);
                    ix += 36;
                }
            }
        } else if (!preview && sel >= 0 && owned(sel)) {
            gg.drawCenteredString(this.font, Tx.t("screen.implanted"), cx, top + 176, 0x55FF55);
        } else if (!preview) {
            gg.drawCenteredString(this.font, Tx.t("doc.place"), cx, top + 176, 0xDDDDDD);
        } else {
            gg.drawCenteredString(this.font, Tx.t("doc.hurt", SandevistanLogic.IMPLANT_HEARTS), cx, top + 176, 0xFF8888);
        }
        // Carga Cibernetica, com quanto vai subir antes de implantar
        float cap = d.capacity();
        float add = pendingLoad();
        gg.drawCenteredString(this.font, add > 0f ? Tx.t("load.preview", Math.round(cap), Math.round(cap + add))
                : Tx.t("load.title", Math.round(cap)), cx, top + 187, cap + add >= d.loadThreshold() ? 0xFFAA33 : 0x88FFAA);
        ClientOverlay.bar(gg, cx - 100, top + 198, 200, 9, cap / 100f, ClientOverlay.loadColor(cap));
        if (add > 0f) {
            int x1 = cx - 100 + (int) (200 * Math.min(1f, cap / 100f));
            int x2 = cx - 100 + (int) (200 * Math.min(1f, (cap + add) / 100f));
            gg.fill(x1, top + 198, x2, top + 207, 0xCCFFFFFF);
        }
        if (preview) {
            gg.renderItem(new ItemStack(Items.EMERALD), cx + 42, top + 214);
            boolean ok = creative() || count(Items.EMERALD) >= totalCost();
            gg.drawString(this.font, "x" + totalCost(), cx + 62, top + 219, ok ? 0x55FF55 : 0xFF5555);
        }
        // inventario do jogador: 3 linhas + hotbar
        int ix = invX();
        int iy = invY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + r * 18, 9 + r * 9 + c);
        }
        for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + 58, c);
    }

    private void drawInvSlot(GuiGraphics gg, int x, int y, int idx) {
        ItemStack st = invStack(idx);
        boolean glow = fitsAnyFree(st);
        gg.fill(x, y, x + 18, y + 18, glow ? 0xFF39FF14 : 0xFF3A3F44);
        gg.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1A1E22);
        if (!st.isEmpty()) {
            gg.renderItem(st, x + 1, y + 1);
            gg.renderItemDecorations(this.font, st, x + 1, y + 1);
        }
    }

    private void drawShop(GuiGraphics gg, int cx, int top) {
        int have = count(Items.EMERALD);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 120, top + 28);
        gg.drawString(this.font, Tx.t("doc.shop.have", have), cx - 98, top + 33, 0x55FF55);
        shopRow(gg, cx, top + 60, new ItemStack(ModRegistries.ESTABILIZADOR.get()), SandevistanLogic.STABILIZER_PRICE);
        shopRow(gg, cx, top + 96, new ItemStack(ModRegistries.IMUNOSSUPRESSOR.get()), SandevistanLogic.IMMUNO_PRICE);
        shopRow(gg, cx, top + 132, new ItemStack(ModRegistries.PILULA.get(), SandevistanLogic.PILL_COUNT), SandevistanLogic.PILL_PRICE);
    }

    private void shopRow(GuiGraphics gg, int cx, int y, ItemStack stack, int price) {
        gg.fill(cx - 124, y - 4, cx + 124, y + 28, 0x60000000);
        gg.renderItem(stack, cx - 118, y + 2);
        gg.renderItemDecorations(this.font, stack, cx - 118, y + 2);
        gg.drawString(this.font, stack.getHoverName(), cx - 96, y + 2, 0xFFFFFF);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 96, y + 12);
        gg.drawString(this.font, "x" + price, cx - 77, y + 17, count(Items.EMERALD) >= price || creative() ? 0x55FF55 : 0xFF5555);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        if (mode != IMPLANTS) return;
        if (carrying) {
            gg.renderItem(carried, mouseX - 8, mouseY - 8);
            return;
        }
        // informacoes do item: so aparecem com o implante no encaixe (ou ao passar o mouse num item do inventario)
        int s = slotAt(mouseX, mouseY);
        if (s >= 0 && (placed[s] || owned(s))) {
            gg.renderTooltip(this.font, shownStack(s), mouseX, mouseY);
            return;
        }
        if (s >= 0) {
            gg.renderTooltip(this.font, isExt(s) ? Tx.t("ext.slot." + extIdx(s)) : Tx.t(SLOT_KEYS[s]), mouseX, mouseY);
            return;
        }
        int idx = invIndexAt(mouseX, mouseY);
        ItemStack st = invStack(idx);
        if (!st.isEmpty()) gg.renderTooltip(this.font, st, mouseX, mouseY);
    }
}
