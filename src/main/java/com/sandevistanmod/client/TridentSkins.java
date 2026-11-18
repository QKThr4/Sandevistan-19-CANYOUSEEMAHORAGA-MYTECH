package com.sandevistanmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.sandevistanmod.SandevistanMod;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrownTrident;

/** Tridentes de Aco/Ciber arremessados: a textura do tridente vanilla recebe uma tinta (aco azulado / Ciber escuro). */
public final class TridentSkins {
    private TridentSkins() {}

    public static final Map<UUID, Integer> KIND = new ConcurrentHashMap<>();
    private static final Map<Integer, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final ResourceLocation VANILLA = ResourceLocation.withDefaultNamespace("textures/entity/trident.png");

    private static ResourceLocation tex(int kind) {
        return CACHE.computeIfAbsent(kind, k -> {
            try (var in = Minecraft.getInstance().getResourceManager().open(VANILLA)) {
                NativeImage img = NativeImage.read(in);
                float tr = k == 1 ? 0.66f : 0.34f, tg = k == 1 ? 0.74f : 0.36f, tb = k == 1 ? 0.82f : 0.43f;
                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        int p = img.getPixelRGBA(x, y);
                        int a = p >>> 24, b = (p >> 16) & 255, g = (p >> 8) & 255, r = p & 255;
                        // realca o brilho (o tridente vanilla e escuro) antes da tinta
                        r = Math.min(255, (int) ((r * 1.6f + 30) * tr * 1.4f));
                        g = Math.min(255, (int) ((g * 1.6f + 30) * tg * 1.4f));
                        b = Math.min(255, (int) ((b * 1.6f + 30) * tb * 1.4f));
                        img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "dyn/trident_" + k);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
                return id;
            } catch (IOException ex) {
                return VANILLA;
            }
        });
    }

    public static class Renderer extends ThrownTridentRenderer {
        public Renderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(ThrownTrident e) {
            Integer k = KIND.get(e.getUUID());
            return k == null ? VANILLA : tex(k);
        }
    }
}
