package com.sandevistanmod.robot;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Raio eletrico disparado pelos robos: reto, sem gravidade, causa dano magico. */
public class ElectricBolt extends ThrowableProjectile {
    public ElectricBolt(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public static void fire(Mob shooter, LivingEntity target, float damageSpeed) {
        Level level = shooter.level();
        ElectricBolt b = new ElectricBolt(ModRegistries.ELECTRIC_BOLT.get(), level);
        b.setOwner(shooter);
        b.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        Vec3 d = target.getEyePosition().subtract(b.position()).normalize().scale(damageSpeed);
        b.setDeltaMovement(d);
        level.addFreshEntity(b);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, 1.0f, 1.6f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        } else if (tickCount > 60) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) result.getEntity().hurt(damageSources().indirectMagic(this, getOwner()), 4.0f);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.3f, 2.0f);
            discard();
        }
    }
}
