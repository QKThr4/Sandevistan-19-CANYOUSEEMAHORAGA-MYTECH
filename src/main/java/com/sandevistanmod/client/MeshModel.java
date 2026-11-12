package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sandevistanmod.SandevistanMod;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Malha de triangulos vinda dos modelos .glb enviados (jetpack e Lamina Mantis), convertida por tools/gen_models.py
 * para models/gear/*.bin. As posicoes ja estao em unidades do modelo do Minecraft (pixels/16) no espaco da parte do corpo.
 */
public final class MeshModel {
    private record Part(ResourceLocation tex, boolean glow, float[] v, int tris) {}

    private static final Map<String, MeshModel> CACHE = new HashMap<>();
    private final List<Part> parts = new ArrayList<>();

    public static MeshModel get(String name) {
        return CACHE.computeIfAbsent(name, MeshModel::load);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static MeshModel load(String name) {
        MeshModel m = new MeshModel();
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "models/gear/" + name + ".bin");
        try (InputStream raw = Minecraft.getInstance().getResourceManager().open(rl);
             DataInputStream in = new DataInputStream(new BufferedInputStream(raw))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                ResourceLocation tex = ResourceLocation.parse(in.readUTF());
                boolean glow = in.readByte() != 0;
                int tris = in.readInt();
                float[] v = new float[tris * 3 * 8];
                for (int k = 0; k < v.length; k++) v[k] = in.readFloat();
                m.parts.add(new Part(tex, glow, v, tris));
            }
        } catch (Exception e) {
            // sem modelo: simplesmente nao desenha (nunca derruba o jogo)
            m.parts.clear();
        }
        return m;
    }

    /** Desenha a malha na pose atual. */
    public void render(PoseStack ps, MultiBufferSource buffer, int light) {
        PoseStack.Pose pose = ps.last();
        for (Part p : parts) {
            VertexConsumer vc = buffer.getBuffer(p.glow ? RenderType.eyes(p.tex) : RenderType.entityCutoutNoCull(p.tex));
            int l = p.glow ? LightTexture.FULL_BRIGHT : light;
            float[] v = p.v;
            for (int t = 0; t < p.tris; t++) {
                int base = t * 24;
                // triangulo enviado como quad (o 4o vertice repete o 3o)
                for (int k = 0; k < 4; k++) {
                    int o = base + Math.min(k, 2) * 8;
                    vc.addVertex(pose, v[o], v[o + 1], v[o + 2])
                            .setColor(255, 255, 255, 255)
                            .setUv(v[o + 3], v[o + 4])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(l)
                            .setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
                }
            }
        }
    }
}
