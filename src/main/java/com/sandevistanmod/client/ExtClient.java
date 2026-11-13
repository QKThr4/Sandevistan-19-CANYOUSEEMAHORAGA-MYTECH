package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.Tags;

/** Lado do cliente dos implantes extras: Scanner de Minerios, zoom, olho tatico e visao termica. */
@EventBusSubscriber(modid = SandevistanMod.MODID, value = Dist.CLIENT)
public final class ExtClient {
    private ExtClient() {}

    private static float zoom = 3f;
    /** Zoom ligado pela roleta de habilidades (alem de segurar a tecla G). */
    static boolean zoomToggle;
    private static final List<BlockPos> ORES = new ArrayList<>();
    private static final List<float[]> ORE_COLORS = new ArrayList<>();

    private static boolean zooming() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || ClientData.DATA.eye() != 1) {
            zoomToggle = false;
            return false;
        }
        return zoomToggle || ModKeys.EYE.isDown();
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent e) {
        if (zooming()) e.setNewFovModifier(e.getNewFovModifier() / zoom);
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent e) {
        if (AbilityWheel.holding() || !zooming()) return;
        float max = SandevistanData.ZOOM_MAX[Math.max(1, ClientData.DATA.extTier(SandevistanData.EXT_EYE)) - 1];
        zoom = Mth.clamp(zoom + (float) Math.signum(e.getScrollDeltaY()) * 1f, 2f, max);
        e.setCanceled(true);
    }

    private static float[] colorOf(String path) {
        if (path.contains("diamond")) return new float[] {0.3f, 1f, 1f};
        if (path.contains("emerald")) return new float[] {0.2f, 1f, 0.3f};
        if (path.contains("gold")) return new float[] {1f, 0.85f, 0.2f};
        if (path.contains("redstone")) return new float[] {1f, 0.15f, 0.15f};
        if (path.contains("lapis")) return new float[] {0.25f, 0.4f, 1f};
        if (path.contains("coal")) return new float[] {0.6f, 0.6f, 0.6f};
        if (path.contains("iron")) return new float[] {0.9f, 0.7f, 0.55f};
        if (path.contains("copper")) return new float[] {1f, 0.55f, 0.25f};
        if (path.contains("quartz")) return new float[] {1f, 1f, 1f};
        if (path.contains("netherite") || path.contains("debris")) return new float[] {0.6f, 0.35f, 0.2f};
        if (path.contains("sucata")) return new float[] {0.7f, 1f, 0.4f};
        return new float[] {1f, 0.3f, 1f};
    }

    // ---- Olho Tatico: contorno (silhueta) dos seres vivos, visivel atras de paredes (brilho do proprio jogo) ----
    private static final java.util.Map<Integer, net.minecraft.world.scores.PlayerTeam> GLOWED = new java.util.HashMap<>();
    private static final java.util.Map<String, net.minecraft.world.scores.PlayerTeam> TEAMS = new java.util.HashMap<>();

    private static net.minecraft.world.scores.PlayerTeam team(Minecraft mc, String key, net.minecraft.ChatFormatting col) {
        net.minecraft.world.scores.Scoreboard sb = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam t = sb.getPlayerTeam("sdv_tac_" + key);
        if (t == null) {
            t = sb.addPlayerTeam("sdv_tac_" + key);
            t.setColor(col);
        }
        return t;
    }

    private static void clearGlow(Minecraft mc) {
        if (mc.level == null) {
            GLOWED.clear();
            return;
        }
        for (Entity en : mc.level.entitiesForRendering()) {
            net.minecraft.world.scores.PlayerTeam t = GLOWED.get(en.getId());
            if (t == null) continue;
            en.setGlowingTag(false);
            if (en.getTeam() == t) mc.level.getScoreboard().removePlayerFromTeam(en.getScoreboardName(), t);
        }
        GLOWED.clear();
    }

    private static void glowTick(Minecraft mc, SandevistanData d) {
        if (mc.level == null || mc.player == null || !d.eyeOn || d.eye() != 2) {
            if (!GLOWED.isEmpty()) clearGlow(mc);
            return;
        }
        int et = Math.max(1, d.extTier(SandevistanData.EXT_EYE)) - 1;
        double range = SandevistanData.TACTICAL_RANGE[et];
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity) || en == mc.player) continue;
            boolean inRange = en.distanceToSqr(mc.player) <= range * range;
            net.minecraft.world.scores.PlayerTeam mine = GLOWED.get(en.getId());
            if (inRange) {
                en.setGlowingTag(true);
                if (mine == null) {
                    net.minecraft.world.scores.PlayerTeam t;
                    if (BuiltInRegistries.ENTITY_TYPE.getKey(en.getType()).getNamespace().equals(SandevistanMod.MODID)) {
                        t = team(mc, "robo", net.minecraft.ChatFormatting.AQUA);
                    } else if (en instanceof Player) {
                        t = team(mc, "player", net.minecraft.ChatFormatting.WHITE);
                    } else if (en instanceof Enemy) {
                        t = team(mc, "enemy", net.minecraft.ChatFormatting.RED);
                    } else if (en instanceof Animal) {
                        t = team(mc, "animal", net.minecraft.ChatFormatting.GREEN);
                    } else {
                        t = team(mc, "other", net.minecraft.ChatFormatting.YELLOW);
                    }
                    // so colore quem nao esta em nenhum time (nao mexe nos times do servidor)
                    if (en.getTeam() == null) mc.level.getScoreboard().addPlayerToTeam(en.getScoreboardName(), t);
                    GLOWED.put(en.getId(), t);
                }
            } else if (mine != null) {
                en.setGlowingTag(false);
                if (en.getTeam() == mine) mc.level.getScoreboard().removePlayerFromTeam(en.getScoreboardName(), mine);
                GLOWED.remove(en.getId());
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        SandevistanData d = ClientData.DATA;
        glowTick(mc, d);
        if (mc.level == null || mc.player == null || d.xrayOn <= 0f || d.extTier(SandevistanData.EXT_XRAY) == 0) {
            ORES.clear();
            ORE_COLORS.clear();
            return;
        }
        if (mc.level.getGameTime() % 10 != 0) return;
        int r = SandevistanData.XRAY_RADIUS[d.extTier(SandevistanData.EXT_XRAY) - 1];
        BlockPos c = mc.player.blockPosition();
        ORES.clear();
        ORE_COLORS.clear();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r && ORES.size() < 500; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    BlockState st = mc.level.getBlockState(m);
                    if (st.isAir() || !st.is(Tags.Blocks.ORES)) continue;
                    ORES.add(m.immutable());
                    ORE_COLORS.add(colorOf(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath()));
                }
            }
        }
    }

    /** Caixa preenchida (6 faces) sem teste de profundidade. */
    private static void fillBox(PoseStack ps, VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1,
                                float r, float g, float b, float a) {
        org.joml.Matrix4f m = ps.last().pose();
        float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
        float[][] q = {
                {ax, ay, az, bx, ay, az, bx, by, az, ax, by, az}, {ax, ay, bz, ax, by, bz, bx, by, bz, bx, ay, bz},
                {ax, ay, az, ax, by, az, ax, by, bz, ax, ay, bz}, {bx, ay, az, bx, ay, bz, bx, by, bz, bx, by, az},
                {ax, ay, az, ax, ay, bz, bx, ay, bz, bx, ay, az}, {ax, by, az, bx, by, az, bx, by, bz, ax, by, bz}};
        for (float[] f : q) {
            for (int i = 0; i < 4; i++) vc.addVertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).setColor(r, g, b, a);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return; // depois de tudo, no alvo principal: aparece atras de paredes tambem em Fancy/Fabulous
        Minecraft mc = Minecraft.getInstance();
        SandevistanData d = ClientData.DATA;
        if (mc.level == null || mc.player == null) return;
        boolean ores = d.xrayOn > 0f && !ORES.isEmpty();
        int eye = d.eye();
        if (!ores) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(XrayRender.LINES);
        if (ores) {
            for (int i = 0; i < ORES.size(); i++) {
                BlockPos b = ORES.get(i);
                float[] col = ORE_COLORS.get(i);
                LevelRenderer.renderLineBox(ps, vc, b.getX() - cam.x, b.getY() - cam.y, b.getZ() - cam.z,
                        b.getX() + 1 - cam.x, b.getY() + 1 - cam.y, b.getZ() + 1 - cam.z, col[0], col[1], col[2], 1f);
            }
        }
        buf.endBatch(XrayRender.FILL);
        buf.endBatch(XrayRender.LINES);
    }
}
