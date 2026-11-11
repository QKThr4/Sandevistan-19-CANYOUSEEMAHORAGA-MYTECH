package com.sandevistanmod.robot;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Robo Zumbi: zumbi resistente que nao queima no sol nem vira afogado. */
public class RoboZombie extends Zombie {
    public RoboZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attrs() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
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

    @Override
    protected SoundEvent getStepSound() {
        return RobotUtil.step();
    }
}
