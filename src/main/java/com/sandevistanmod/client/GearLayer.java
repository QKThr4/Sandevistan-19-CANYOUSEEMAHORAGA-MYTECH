package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sandevistanmod.SandevistanMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Modelos 3D dos implantes extras no corpo: jetpack (costas), botas, painel solar (cabeca), reator Arc (peito),
 * laminas Mantis (antebracos, os dois lados) e injetor automatico (ombro). Os olhos nao tem modelo.
 */
public class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation GEAR = tex("gear");
    private static final ResourceLocation SOLAR = tex("solar_panel");
    private static final ResourceLocation REACTOR = tex("arc_reactor");
    /** Quatro quadrantes da textura gear: aco, metal escuro, ciano brilhante, chama. */
    private static final float[] STEEL = {0.02f, 0.02f, 0.48f, 0.48f};
    private static final float[] DARK = {0.52f, 0.02f, 0.98f, 0.48f};
    private static final float[] CYAN = {0.02f, 0.52f, 0.48f, 0.98f};
    private static final float[] FLAME = {0.52f, 0.52f, 0.98f, 0.98f};
    private static final float[] FULL = {0f, 0f, 1f, 1f};
    private static final int BRIGHT = LightTexture.FULL_BRIGHT;

    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "textures/entity/" + n + ".png");
    }

    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Caixa em pixels do modelo (x0..x1 etc.) com um retangulo de textura. */
    private static void bx(PoseStack ps, VertexConsumer vc, int light, float[] uv, float x0, float y0, float z0, float x1, float y1, float z1) {
        float s = 1f / 16f;
        BodyLayer.box(ps.last(), vc, x0 * s, y0 * s, z0 * s, x1 * s, y1 * s, z1 * s, light, uv[0], uv[1], uv[2], uv[3]);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        int code = ClientState.get(player.getUUID());
        if ((code >>> 20) == 0) return;
        boolean fly = (code & (1 << 28)) != 0;
        boolean chest = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer g = buffer.getBuffer(RenderType.entityCutoutNoCull(GEAR));

        // ---- corpo: jetpack, reator, injetor ----
        boolean jet = (code & (1 << 20)) != 0;
        boolean reactor = (code & (1 << 24)) != 0;
        boolean injector = (code & (1 << 27)) != 0;
        if (jet || reactor || injector) {
            ps.pushPose();
            m.body.translateAndRotate(ps);
            if (jet) jetpack(ps, buffer, g, light, chest ? 3.2f : 2.5f, fly, ageInTicks);
            if (injector) injector(ps, g, light);
            if (reactor) reactor(ps, buffer, light, chest ? -3.2f : -2.0f);
            ps.popPose();
        }
        // ---- botas ----
        if ((code & (1 << 21)) != 0) boot(ps, g, light, m.rightLeg, fly, ageInTicks, 0f);
        if ((code & (1 << 22)) != 0) boot(ps, g, light, m.leftLeg, fly, ageInTicks, 1.7f);
        // ---- painel solar ----
        if ((code & (1 << 23)) != 0) {
            ps.pushPose();
            m.head.translateAndRotate(ps);
            bx(ps, g, light, DARK, -3.5f, -9.3f, -3.5f, 3.5f, -8.7f, 3.5f);
            VertexConsumer sv = buffer.getBuffer(RenderType.entityCutoutNoCull(SOLAR));
            bx(ps, sv, light, FULL, -3.1f, -9.6f, -3.1f, 3.1f, -9.2f, 3.1f);
            ps.popPose();
        }
        // ---- laminas Mantis ----
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        if ((code & (1 << 25)) != 0) mantis(ps, buffer, light, m.rightArm, true, slim);
        if ((code & (1 << 26)) != 0) mantis(ps, buffer, light, m.leftArm, false, slim);
    }

    private static void jetpack(PoseStack ps, MultiBufferSource buffer, VertexConsumer g, int light, float zb, boolean fly, float age) {
        // modelo enviado pelo jogador (jetpack_utrm): placa encostada nas costas, tanques e bocais
        ps.pushPose();
        ps.translate(0f, 0f, zb / 16f);
        MeshModel.get("jetpack").render(ps, buffer, light);
        ps.popPose();
        if (fly) {
            for (int k = 0; k < 2; k++) {
                float cx = k == 0 ? -2.9f : 2.9f;
                float len = 3.5f + 1.6f * (float) Math.sin(age * 1.9f + k * 1.3f);
                bx(ps, g, BRIGHT, FLAME, cx - 0.9f, 11.6f, zb + 2.2f, cx + 0.9f, 11.6f + len, zb + 4.0f);
                bx(ps, g, BRIGHT, CYAN, cx - 0.45f, 11.6f, zb + 2.65f, cx + 0.45f, 11.6f + len * 0.6f, zb + 3.55f);
            }
        }
    }

    private static void boot(PoseStack ps, VertexConsumer g, int light, ModelPart leg, boolean fly, float age, float phase) {
        ps.pushPose();
        leg.translateAndRotate(ps);
        bx(ps, g, light, STEEL, -2.5f, 7.6f, -2.5f, 2.5f, 12.2f, 2.5f);
        bx(ps, g, light, DARK, -2.6f, 10.2f, -3.0f, 2.6f, 12.3f, -0.4f);
        bx(ps, g, BRIGHT, CYAN, -2.6f, 8.4f, -2.6f, 2.6f, 9.0f, 2.6f);
        bx(ps, g, light, DARK, -1.6f, 12.2f, -1.2f, 1.6f, 13.0f, 1.8f);
        if (fly) {
            float len = 2.4f + 1.1f * (float) Math.sin(age * 2.1f + phase);
            bx(ps, g, BRIGHT, FLAME, -1.0f, 13.0f, -0.6f, 1.0f, 13.0f + len, 1.2f);
        }
        ps.popPose();
    }

    private static void reactor(PoseStack ps, MultiBufferSource buffer, int light, float zf) {
        VertexConsumer g = buffer.getBuffer(RenderType.entityCutoutNoCull(GEAR));
        bx(ps, g, light, DARK, -2.6f, 0.9f, zf - 0.7f, 2.6f, 6.1f, zf + 0.2f);
        VertexConsumer r = buffer.getBuffer(RenderType.entityCutoutNoCull(REACTOR));
        bx(ps, r, BRIGHT, FULL, -1.9f, 1.6f, zf - 0.95f, 1.9f, 5.4f, zf - 0.6f);
    }

    private static void injector(PoseStack ps, VertexConsumer g, int light) {
        bx(ps, g, light, DARK, 4.3f, -2.6f, -1.6f, 7.3f, -0.1f, 1.6f);
        bx(ps, g, BRIGHT, CYAN, 5.0f, -3.0f, -0.8f, 6.6f, -2.6f, 0.8f);
        bx(ps, g, light, STEEL, 5.4f, -5.0f, -0.4f, 6.2f, -3.0f, 0.4f);
    }

    private static void mantis(PoseStack ps, MultiBufferSource buffer, int light, ModelPart arm, boolean right, boolean slim) {
        ps.pushPose();
        arm.translateAndRotate(ps);
        mantisModel(ps, buffer, light, right, slim);
        ps.popPose();
    }

    /** Mecanismo + lamina (modelo enviado) no espaco do braco; compensa o braco fino (slim). */
    static void mantisModel(PoseStack ps, MultiBufferSource buffer, int light, boolean right, boolean slim) {
        float w = slim ? 3f : 4f;
        float x0 = right ? (slim ? -2f : -3f) : -1f;
        float c = x0 + w / 2f;
        float out = right ? -1f : 1f;
        ps.pushPose();
        ps.translate((c + out * (w / 2f - 2f)) / 16f, 0f, 0f);
        MeshModel.get(right ? "mantis_r" : "mantis_l").render(ps, buffer, light);
        ps.popPose();
    }
}
