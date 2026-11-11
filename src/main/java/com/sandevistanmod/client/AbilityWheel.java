package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.ActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Seletor de habilidades: segure a tecla de selecao e role o mouse para trocar de habilidade; a tecla de uso ativa a escolhida.
 * Cada habilidade e um quadradinho com o icone do implante (a posicao se ajusta no editor do HUD).
 */
@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class AbilityWheel {
    private AbilityWheel() {}

    private static int selected;
    private static long showUntil;

    /** Uma habilidade: icone, nome e o que fazer ao usar. */
    public record Ability(ItemStack icon, Component name, Runnable use) {}

    public static List<Ability> list() {
        SandevistanData d = ClientData.DATA;
        List<Ability> out = new ArrayList<>();
        if (d.implanted) {
            ItemStack st = new ItemStack(SandevistanLogic.implantItem(SandevistanData.SLOT_BACK, d.variant, 1));
            out.add(new Ability(st, st.getHoverName(), () -> PacketDistributor.sendToServer(new ActionPayload(0))));
        }
        if (d.extTier(SandevistanData.EXT_XRAY) != 0) {
            ItemStack st = new ItemStack(ModRegistries.extItem(SandevistanData.EXT_XRAY, d.ext[SandevistanData.EXT_XRAY], 0));
            out.add(new Ability(st, st.getHoverName(), () -> PacketDistributor.sendToServer(new ActionPayload(20))));
        }
        if (d.eye() != 0) {
            ItemStack st = new ItemStack(ModRegistries.extItem(SandevistanData.EXT_EYE, d.ext[SandevistanData.EXT_EYE], d.eyeType));
            out.add(new Ability(st, st.getHoverName(), () -> {
                if (ClientData.DATA.eye() == 1) ExtClient.zoomToggle = !ExtClient.zoomToggle;
                else PacketDistributor.sendToServer(new ActionPayload(21));
            }));
        }
        if (d.extTier(SandevistanData.EXT_INJECTOR) != 0) {
            ItemStack st = new ItemStack(ModRegistries.extItem(SandevistanData.EXT_INJECTOR, d.ext[SandevistanData.EXT_INJECTOR], 0));
            Component name = st.getHoverName().copy().append(" ").append(Tx.t(d.injOn ? "wheel.on" : "wheel.off"));
            out.add(new Ability(st, name, () -> PacketDistributor.sendToServer(new ActionPayload(22))));
        }
        return out;
    }

    public static boolean holding() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.screen == null && ModKeys.SELECT.isDown() && !list().isEmpty();
    }

    private static int clampSel(int n) {
        selected = n <= 0 ? 0 : Math.floorMod(selected, n);
        return selected;
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent e) {
        if (!holding()) return;
        int n = list().size();
        selected = Math.floorMod(selected - (int) Math.signum(e.getScrollDeltaY()), n);
        showUntil = System.currentTimeMillis() + 2500L;
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.SELECT.consumeClick()) {
            if (mc.screen == null) showUntil = System.currentTimeMillis() + 2500L;
        }
        while (ModKeys.USE.consumeClick()) {
            List<Ability> l = list();
            if (mc.screen != null || mc.player == null || l.isEmpty()) continue;
            l.get(clampSel(l.size())).use().run();
            showUntil = System.currentTimeMillis() + 2500L;
        }
    }

    public static void render(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        List<Ability> l = list();
        if (l.isEmpty() || !(holding() || System.currentTimeMillis() < showUntil)) return;
        drawBar(gg, l, clampSel(l.size()), mc.font);
    }

    static void drawBar(GuiGraphics gg, List<Ability> l, int sel, net.minecraft.client.gui.Font font) {
        int[] pos = ClientLayout.abilPos(gg.guiWidth(), gg.guiHeight(), l.size());
        int s = ClientLayout.ABIL_SLOT;
        for (int i = 0; i < l.size(); i++) {
            int x = pos[0] + i * (s + 2);
            int y = pos[1];
            boolean on = i == sel;
            gg.fill(x - 1, y - 1, x + s + 1, y + s + 1, on ? 0xFFFFFFFF : 0xFF555A60);
            gg.fill(x, y, x + s, y + s, on ? 0xFF2A3A44 : 0xFF1A1E22);
            gg.renderItem(l.get(i).icon(), x + 1, y + 1);
        }
        Component name = l.get(sel).name();
        int w = l.size() * (s + 2) - 2;
        gg.drawCenteredString(font, name, pos[0] + w / 2, pos[1] - 11, 0xFFFFFF);
    }
}
