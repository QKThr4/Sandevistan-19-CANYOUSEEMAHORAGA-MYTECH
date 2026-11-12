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
        zoom = Mth.clamp(zoom + (float) Math.signum(e.getScrollDeltaY()) * 0.5f, 2f, max);
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

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        SandevistanData d = ClientData.DATA;
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

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        SandevistanData d = ClientData.DATA;
        if (mc.level == null || mc.player == null) return;
        boolean ores = d.xrayOn > 0f && !ORES.isEmpty();
        int eye = d.eye();
        boolean mobs = d.eyeOn && (eye == 2 || eye == 4);
        if (!ores && !mobs) return;
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
        if (mobs) {
            float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
            int et = Math.max(1, d.extTier(SandevistanData.EXT_EYE)) - 1;
            double range = eye == 2 ? SandevistanData.TACTICAL_RANGE[et] : SandevistanData.VISION_RANGE[et];
            for (Entity en : mc.level.entitiesForRendering()) {
                if (!(en instanceof LivingEntity) || en == mc.player || en.distanceToSqr(mc.player) > range * range) continue;
                float[] col;
                if (eye == 4) {
                    col = new float[] {1f, 0.55f, 0.1f};
                } else if (BuiltInRegistries.ENTITY_TYPE.getKey(en.getType()).getNamespace().equals(SandevistanMod.MODID)) {
                    col = new float[] {0.2f, 0.9f, 1f};
                } else if (en instanceof Player) {
                    col = new float[] {1f, 1f, 1f};
                } else if (en instanceof Enemy) {
                    col = new float[] {1f, 0.15f, 0.15f};
                } else if (en instanceof Animal) {
                    col = new float[] {0.3f, 1f, 0.3f};
                } else {
                    col = new float[] {1f, 1f, 0.3f};
                }
                AABB bb = en.getBoundingBox().move(Mth.lerp(pt, en.xo, en.getX()) - en.getX(), Mth.lerp(pt, en.yo, en.getY()) - en.getY(),
                        Mth.lerp(pt, en.zo, en.getZ()) - en.getZ());
                LevelRenderer.renderLineBox(ps, vc, bb.minX - cam.x, bb.minY - cam.y, bb.minZ - cam.z,
                        bb.maxX - cam.x, bb.maxY - cam.y, bb.maxZ - cam.z, col[0], col[1], col[2], 1f);
            }
        }
        buf.endBatch(XrayRender.LINES);
    }
}
