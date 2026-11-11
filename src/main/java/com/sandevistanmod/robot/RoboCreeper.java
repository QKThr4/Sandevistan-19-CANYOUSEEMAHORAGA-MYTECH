package com.sandevistanmod.robot;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** Robo Creeper: so explode, igual ao creeper normal, com mais vida. */
public class RoboCreeper extends Creeper {
    public RoboCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attrs() {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH, 24.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, RobotUtil.adjust(source, amount));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return RobotUtil.hurt();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return RobotUtil.death();
    }
}
