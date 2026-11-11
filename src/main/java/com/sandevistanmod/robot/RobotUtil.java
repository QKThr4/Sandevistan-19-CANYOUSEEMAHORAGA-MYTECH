package com.sandevistanmod.robot;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Regras comuns dos robos: sons metalicos e dano dobrado de raio. */
public final class RobotUtil {
    private RobotUtil() {}

    public static float adjust(DamageSource source, float amount) {
        return source.is(DamageTypes.LIGHTNING_BOLT) ? amount * 2f : amount;
    }

    public static SoundEvent hurt() {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    public static SoundEvent death() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    public static SoundEvent step() {
        return SoundEvents.IRON_GOLEM_STEP;
    }
}
