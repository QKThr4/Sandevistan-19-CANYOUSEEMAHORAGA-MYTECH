package com.sandevistanmod.client;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.registry.ModRegistries;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.BowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Sintomas leves da Mente maxima permanente baixa que so o proprio jogador sente: mao tremendo ao mirar, dor de
 * cabeca (pulsos na tela), visao que falha, zumbido com som abafado (raro), passos e
 * sussurros que nao existem e lapsos em que o corpo congela por 1 segundo. Tudo usa tempo real (nao os ticks do jogo).
 */
@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class LowMindFx {
    private LowMindFx() {}

    private static final Random R = new Random();
    /** Proximo disparo de cada sintoma (ms): 0 dor de cabeca, 1 visao, 2 (livre), 3 zumbido, 4 sons, 5 lapso. */
    private static final long[] NEXT = new long[6];
    private static long headacheUntil;
    private static long flashUntil;
    private static long muffleUntil;
    private static long freezeUntil;
    private static long headacheStart;
    private static Double origMaster;
    private static int stepsLeft;
    private static long nextStep;
    private static boolean stepStone;
    private static double stepX;
    private static double stepZ;

    private static boolean fire(int i, double minS, double maxS, double scale) {
        long now = System.currentTimeMillis();
        if (NEXT[i] == 0L) {
            NEXT[i] = now + (long) ((minS + R.nextDouble() * (maxS - minS)) * 1000.0 * scale);
            return false;
        }
        if (now < NEXT[i]) return false;
        NEXT[i] = now + (long) ((minS + R.nextDouble() * (maxS - minS)) * 1000.0 * scale);
        return true;
    }

    private static void restoreVolume() {
        if (origMaster == null) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(origMaster);
        } catch (RuntimeException ignored) {
            // nada a fazer: o volume volta na proxima vez
        }
        origMaster = null;
    }

    private static void reset() {
        restoreVolume();
        java.util.Arrays.fill(NEXT, 0L);
        headacheUntil = 0L;
        flashUntil = 0L;
        freezeUntil = 0L;
        muffleUntil = 0L;
        stepsLeft = 0;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        reset();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            reset();
            return;
        }
        int t = ClientData.DATA.lowTier();
        long now = System.currentTimeMillis();
        if (now >= muffleUntil) restoreVolume();
        if (t == 0) {
            if (headacheUntil != 0L || flashUntil != 0L) reset();
            return;
        }
        if (mc.isPaused()) return;

        // maos tremendo: mira de arco/besta e golpes balancam um pouco
        boolean aiming = p.isUsingItem() && (p.getUseItem().getItem() instanceof BowItem
                || p.getUseItem().getItem() instanceof CrossbowItem);
        if (aiming || p.swinging) {
            float amp = t == 1 ? 0.25f : t == 2 ? 0.5f : 0.9f;
            p.setYRot(p.getYRot() + (R.nextFloat() - 0.5f) * 2f * amp);
            p.setXRot(Mth.clamp(p.getXRot() + (R.nextFloat() - 0.5f) * 2f * amp, -90f, 90f));
        }

        double s = t == 1 ? 1.5 : t == 2 ? 1.0 : 0.6;
        if (fire(0, 40, 90, s)) {
            headacheStart = now;
            headacheUntil = now + 3500L;
        }
        if (fire(1, 50, 120, s)) flashUntil = now + 350L;
        // zumbido raro: um apito agudo e o som do jogo abafa por alguns segundos, depois some
        if (fire(3, 120, 240, s)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistries.TINNITUS.get(), 1.0f, 0.5f));
            try {
                var opt = mc.options.getSoundSourceOptionInstance(SoundSource.MASTER);
                if (origMaster == null) origMaster = opt.get();
                opt.set(origMaster * 0.4);
                muffleUntil = now + 3000L;
            } catch (RuntimeException ignored) {
                origMaster = null;
            }
        }
        // sons que nao existem: passos ou sussurros fracos atras do jogador
        if (fire(4, 60, 150, s)) {
            double yaw = Math.toRadians(p.getYRot());
            stepX = p.getX() + Math.sin(yaw) * 3.0;
            stepZ = p.getZ() - Math.cos(yaw) * 3.0;
            if (R.nextBoolean()) {
                stepsLeft = 3;
                nextStep = now;
                stepStone = R.nextBoolean();
            } else {
                SoundEvent ev = (R.nextBoolean() ? ModRegistries.PSY_SND_1 : ModRegistries.PSY_SND_2).get();
                mc.level.playLocalSound(stepX, p.getY(), stepZ, ev, SoundSource.AMBIENT, 0.12f, 1.0f, false);
            }
        }
        if (stepsLeft > 0 && now >= nextStep) {
            stepsLeft--;
            nextStep = now + 380L;
            SoundEvent ev = stepStone ? SoundEvents.STONE_STEP : SoundEvents.GRASS_STEP;
            mc.level.playLocalSound(stepX, p.getY(), stepZ, ev, SoundSource.PLAYERS, 0.35f, 0.9f, false);
        }
        // lapso: o corpo congela por 1 segundo (so com a Mente muito baixa)
        if (t >= 2 && fire(5, 150, 300, t == 3 ? 0.6 : 1.0)) freezeUntil = now + 1000L;
        if (now < freezeUntil) {
            var m = p.getDeltaMovement();
            p.setDeltaMovement(0.0, Math.min(m.y, 0.0), 0.0);
        }
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        if (System.currentTimeMillis() >= freezeUntil) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e.getEntity() != mc.player || ClientData.DATA.lowTier() < 2) return;
        e.getInput().forwardImpulse = 0f;
        e.getInput().leftImpulse = 0f;
        e.getInput().jumping = false;
    }

    // ---------- tela ----------
    private static void vignette(GuiGraphics gg, int w, int h, float alpha, int rgb) {
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float k = 1f - i / (float) steps;
            int a = (int) (alpha * k * k * 255f);
            if (a <= 0) continue;
            int c = (a << 24) | rgb;
            int tx = (int) (w * 0.018f * (i + 1));
            int ty = (int) (h * 0.018f * (i + 1));
            int px = (int) (w * 0.018f * i);
            int py = (int) (h * 0.018f * i);
            gg.fill(px, py, w - px, ty, c);
            gg.fill(px, h - ty, w - px, h - py, c);
            gg.fill(px, ty, tx, h - ty, c);
            gg.fill(w - tx, ty, w - px, h - ty, c);
        }
    }

    public static void render(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int t = ClientData.DATA.lowTier();
        if (t == 0) return;
        int w = gg.guiWidth();
        int h = gg.guiHeight();
        long now = System.currentTimeMillis();
        float lvl = t == 1 ? 0.6f : t == 2 ? 0.85f : 1.1f;
        // dor de cabeca: pulsos suaves como um batimento
        if (now < headacheUntil) {
            double sec = (now - headacheStart) / 1000.0;
            float beat = (float) Math.pow(Math.max(0.0, Math.sin(sec * Math.PI * 2.2)), 3.0);
            float fade = Mth.clamp((headacheUntil - now) / 800f, 0f, 1f);
            vignette(gg, w, h, 0.28f * lvl * beat * fade, 0x300000);
        }
        // visao que falha: cinza rapido e cantos escuros
        if (now < flashUntil) {
            float k = (flashUntil - now) / 350f;
            gg.fill(0, 0, w, h, ((int) (0.35f * k * 255f) << 24) | 0x808080);
            vignette(gg, w, h, 0.55f * k, 0x000000);
        }
    }
}
