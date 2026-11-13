package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.registry.ModRegistries;
import com.sandevistanmod.robot.RoboCreeper;
import com.sandevistanmod.robot.RoboDog;
import com.sandevistanmod.robot.RoboDrone;
import com.sandevistanmod.robot.RoboSecurity;
import com.sandevistanmod.robot.RoboSkeleton;
import com.sandevistanmod.robot.RoboZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderizadores dos robos: modelos do jogo com texturas de metal proprias. */
@EventBusSubscriber(modid = SandevistanMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RobotRenderers {
    private RobotRenderers() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "textures/entity/" + name + ".png");
    }

    static final ModelLayerLocation DOG = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "robo_cao"), "main");
    static final ModelLayerLocation DRONE = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "robo_drone"), "main");

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DOG, () -> DogModel.create());
        e.registerLayerDefinition(DRONE, DroneModel::create);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModRegistries.ROBO_ZUMBI.get(), ctx -> new ZombieRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Zombie z) {
                return tex("robo_zumbi");
            }
        });
        e.registerEntityRenderer(ModRegistries.ROBO_CREEPER.get(), ctx -> new CreeperRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Creeper c) {
                return tex("robo_creeper");
            }
        });
        e.registerEntityRenderer(ModRegistries.ROBO_ESQUELETO.get(), ctx -> new HumanoidMobRenderer<RoboSkeleton, SkeletonModel<RoboSkeleton>>(
                ctx, new SkeletonModel<>(ctx.bakeLayer(ModelLayers.SKELETON)), 0.5f) {
            @Override
            public ResourceLocation getTextureLocation(RoboSkeleton s) {
                return tex("robo_esqueleto");
            }
        });
        e.registerEntityRenderer(ModRegistries.ROBO_SEGURANCA.get(), ctx -> new HumanoidMobRenderer<RoboSecurity, HumanoidModel<RoboSecurity>>(
                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.7f) {
            @Override
            public ResourceLocation getTextureLocation(RoboSecurity s) {
                return tex("robo_seguranca");
            }

            @Override
            protected void scale(RoboSecurity s, PoseStack ps, float partial) {
                ps.scale(1.25f, 1.25f, 1.25f);
            }
        });
        e.registerEntityRenderer(ModRegistries.ROBO_CAO.get(), ctx -> new MobRenderer<RoboDog, DogModel>(
                ctx, new DogModel(ctx.bakeLayer(DOG)), 0.4f) {
            @Override
            public ResourceLocation getTextureLocation(RoboDog d) {
                return tex("robo_cao");
            }

            @Override
            protected void scale(RoboDog d, PoseStack ps, float partial) {
                ps.scale(0.8f, 0.8f, 0.8f);
            }
        });
        e.registerEntityRenderer(ModRegistries.ROBO_DRONE.get(), ctx -> new MobRenderer<RoboDrone, DroneModel>(
                ctx, new DroneModel(ctx.bakeLayer(DRONE)), 0.3f) {
            @Override
            public ResourceLocation getTextureLocation(RoboDrone d) {
                return tex("robo_drone");
            }
        });
        e.registerEntityRenderer(ModRegistries.ELECTRIC_BOLT.get(), NoopRenderer::new);
    }
}
