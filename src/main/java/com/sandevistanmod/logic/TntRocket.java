package com.sandevistanmod.logic;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Foguete de TNT do Canhao de Braco: voa reto deixando fogo e fumaca e explode como TNT (fere o dono e os pets, destroi blocos). */
public class TntRocket extends ThrowableItemProjectile {
    public TntRocket(EntityType<? extends TntRocket> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public TntRocket(Level level, LivingEntity owner) {
        super(ModRegistries.TNT_ROCKET.get(), owner, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.TNT;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0, 0, 0);
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            }
        } else if (tickCount > 200) {
            boom();
        }
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide) boom();
    }

    private void boom() {
        if (isRemoved()) return;
        level().explode(this, getX(), getY(), getZ(), 4.0f, Level.ExplosionInteraction.TNT);
        discard();
    }
}
