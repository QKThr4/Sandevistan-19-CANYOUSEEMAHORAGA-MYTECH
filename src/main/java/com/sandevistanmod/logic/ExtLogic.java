package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Implantes extras (botas, jetpack, XP, scanner, olhos): instalados no Ripperdoc; tudo que gasta energia usa a bateria. */
public final class ExtLogic {
    private ExtLogic() {}

    // ---------- Ripperdoc ----------
    /** Codifica o pedido de instalar (negativo, para nao colidir com os codigos antigos do doutor). */
    public static int installCode(int slot, int tier, int eyeType) {
        return -(1 + slot * 100 + tier * 10 + eyeType);
    }

    public static int removeCode(int slot) {
        return -(10000 + slot);
    }

    /** Trata os codigos negativos de DoctorActionPayload. */
    public static void doctor(ServerPlayer p, int action) {
        int v = -action;
        if (v >= 30000) {
            repair(p, v - 30000);
            return;
        }
        if (v >= 20000) {
            Quests.action(p, v - 20000);
            return;
        }
        if (v >= 10000) {
            remove(p, v - 10000);
            return;
        }
        v -= 1;
        install(p, v / 100, (v / 10) % 10, v % 10);
    }

    private static void install(ServerPlayer p, int slot, int tier, int eyeType) {
        SandevistanData d = SandevistanData.get(p);
        if (slot < 0 || slot >= SandevistanData.EXT_COUNT || tier < 1 || tier > SandevistanData.maxExtTier(slot)) return;
        if (slot == SandevistanData.EXT_EYE && (eyeType < 1 || eyeType > 4)) return;
        if (d.ext[slot] != 0) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "already"));
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        net.minecraft.world.item.Item item = ModRegistries.extItem(slot, tier, eyeType);
        int cost = SandevistanData.extCost(slot, tier, eyeType);
        if (!creative && !SandevistanLogic.hasItem(p, item)) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "clinic.noitem"));
            return;
        }
        if (!creative && p.getHealth() <= SandevistanLogic.IMPLANT_DAMAGE + 1f) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "lowhp", SandevistanLogic.IMPLANT_HEARTS));
            return;
        }
        if (!creative && SandevistanLogic.countEmeralds(p) < cost) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "clinic.noemerald", cost));
            return;
        }
        int dur = SandevistanData.MANTIS_MAX;
        if (!creative) {
            SandevistanLogic.takeEmeralds(p, cost);
            if (slot == SandevistanData.EXT_MANTIS_R || slot == SandevistanData.EXT_MANTIS_L) dur = takeBlade(p, item);
            else SandevistanLogic.takeItem(p, item);
        }
        p.hurt(p.damageSources().magic(), SandevistanLogic.IMPLANT_DAMAGE);
        d.ext[slot] = tier;
        if (slot == SandevistanData.EXT_MANTIS_R) d.mantisDur[0] = dur;
        if (slot == SandevistanData.EXT_MANTIS_L) d.mantisDur[1] = dur;
        if (slot == SandevistanData.EXT_EYE) {
            d.eyeType = eyeType;
            d.eyeOn = false;
        }
        SandevistanLogic.send(p, d);
        SandevistanLogic.bar(p, Tx.t(ChatFormatting.GREEN, "implanted.short"));
        RipperdocTalk.afterImplant(p, d);
    }

    /** Tira do inventario a lamina em melhor estado e devolve a durabilidade que ela tinha. */
    private static int takeBlade(ServerPlayer p, net.minecraft.world.item.Item item) {
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item) && (best.isEmpty() || s.getDamageValue() < best.getDamageValue())) best = s;
        }
        if (best.isEmpty()) {
            for (ItemStack s : p.getInventory().offhand) if (s.is(item)) best = s;
        }
        if (best.isEmpty()) return SandevistanData.MANTIS_MAX;
        int dur = Math.max(0, best.getMaxDamage() - best.getDamageValue());
        best.shrink(1);
        return dur;
    }

    private static void remove(ServerPlayer p, int slot) {
        SandevistanData d = SandevistanData.get(p);
        if (slot < 0 || slot >= SandevistanData.EXT_COUNT || d.ext[slot] == 0) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "noimplant"));
            return;
        }
        ItemStack back = new ItemStack(ModRegistries.extItem(slot, d.ext[slot], d.eyeType));
        if (slot == SandevistanData.EXT_MANTIS_R || slot == SandevistanData.EXT_MANTIS_L) {
            int dur = d.mantisDur[slot == SandevistanData.EXT_MANTIS_R ? 0 : 1];
            back.setDamageValue(SandevistanData.MANTIS_MAX - Math.max(0, dur));
        }
        SandevistanLogic.give(p, back);
        d.ext[slot] = 0;
        if (slot == SandevistanData.EXT_EYE) {
            d.eyeOn = false;
            d.eyeType = 0;
            d.laserCharge = 0f;
            if (!d.active) p.removeEffect(MobEffects.NIGHT_VISION);
        }
        if (slot == SandevistanData.EXT_XRAY) d.xrayOn = 0f;
        SandevistanLogic.send(p, d);
        SandevistanLogic.bar(p, Tx.t(ChatFormatting.YELLOW, "removed"));
        RipperdocTalk.afterRemove(p);
    }

    // ---------- energia ----------
    private static float mult(SandevistanData d) {
        return SandevistanLogic.energyMult(d.energyUsers());
    }

    /** Gasta fe por segundo (vezes o multiplicador de implantes e dt). */
    private static boolean drain(SandevistanData d, float perSecond, float dt) {
        return SandevistanLogic.drainEnergy(d, perSecond * mult(d) * dt);
    }

    private static boolean pay(SandevistanData d, float cost) {
        float c = cost * mult(d);
        if (d.batEnergy < c) return false;
        SandevistanLogic.drainEnergy(d, c);
        return true;
    }

    // ---------- acoes do jogador ----------
    public static void xray(ServerPlayer p, SandevistanData d) {
        int tier = d.extTier(SandevistanData.EXT_XRAY);
        if (tier == 0 || d.xrayCd > 0f || d.xrayOn > 0f) return;
        if (!pay(d, 300f)) {
            p.displayClientMessage(Tx.t(ChatFormatting.RED, "ext.noenergy"), true);
            return;
        }
        d.xrayOn = 10f;
        d.xrayCd = SandevistanData.XRAY_COOLDOWN[tier - 1];
        SandevistanLogic.send(p, d);
    }

    public static void eye(ServerPlayer p, SandevistanData d) {
        int eye = d.eye();
        if (eye == 2 || eye == 4) {
            if (!d.eyeOn && !d.hasEnergy()) {
                p.displayClientMessage(Tx.t(ChatFormatting.RED, "ext.noenergy"), true);
                return;
            }
            d.eyeOn = !d.eyeOn;
            if (!d.eyeOn && eye == 4 && !d.active) p.removeEffect(MobEffects.NIGHT_VISION);
            SandevistanLogic.send(p, d);
        } else if (eye == 3 && d.laserCd <= 0f && d.laserCharge <= 0f) {
            if (d.batEnergy < 400f * mult(d)) {
                p.displayClientMessage(Tx.t(ChatFormatting.RED, "ext.noenergy"), true);
                return;
            }
            d.laserCharge = SandevistanData.LASER_CHARGE[d.extTier(SandevistanData.EXT_EYE) - 1];
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    /** O laser carrega por um instante (os olhos brilham) e depois dispara. */
    private static void laserTick(ServerPlayer p, SandevistanData d, float dt) {
        if (d.laserCharge <= 0f) return;
        ServerLevel level = p.serverLevel();
        Vec3 eyes = p.getEyePosition().add(p.getLookAngle().scale(0.4));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, eyes.x, eyes.y, eyes.z, 1, 0.1, 0.1, 0.1, 0.0);
        d.laserCharge -= dt;
        if (d.laserCharge > 0f) return;
        d.laserCharge = 0f;
        if (d.eye() != 3) return;
        if (!pay(d, 400f)) {
            p.displayClientMessage(Tx.t(ChatFormatting.RED, "ext.noenergy"), true);
            return;
        }
        fireLaser(p, d, level);
    }

    private static void fireLaser(ServerPlayer p, SandevistanData d, ServerLevel level) {
        d.laserCd = 3f;
        int tier = d.extTier(SandevistanData.EXT_EYE);
        Vec3 start = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        Vec3 far = start.add(dir.scale(32.0));
        BlockHitResult bh = level.clip(new ClipContext(start, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? far : bh.getLocation();
        AABB box = p.getBoundingBox().expandTowards(dir.scale(32.0)).inflate(1.0);
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(level, p, start, end, box,
                en -> !en.isSpectator() && en.isPickable() && en != p);
        if (eh != null) {
            end = eh.getLocation();
            eh.getEntity().hurt(p.damageSources().playerAttack(p), SandevistanData.LASER_DAMAGE[tier - 1]);
            eh.getEntity().setRemainingFireTicks(60);
        }
        double len = end.subtract(start).length();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.0f);
        for (double t = 0.8; t < len; t += 0.5) {
            Vec3 pt = start.add(dir.scale(t));
            level.sendParticles(dust, pt.x, pt.y - 0.1, pt.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    // ---------- tick ----------
    public static void tick(ServerPlayer p, SandevistanData d, float dt) {
        if (d.laserCd > 0f) d.laserCd = Math.max(0f, d.laserCd - dt);
        if (d.xrayCd > 0f) d.xrayCd = Math.max(0f, d.xrayCd - dt);
        if (d.xrayOn > 0f) d.xrayOn = Math.max(0f, d.xrayOn - dt);
        Quests.tick(p, d, dt);
        energyTick(p, d, dt);
        mantisTick(d, dt);
        injectorTick(p, d, dt);
        flight(p, d, dt);
        glide(p, d, dt);
        eyeTick(p, d, dt);
        laserTick(p, d, dt);
    }

    /** Lamina ligada gasta energia o tempo todo (4 FE/s por lamina inteira). */
    private static void mantisTick(SandevistanData d, float dt) {
        if (!d.mantisOn) return;
        int n = (d.mantisOk(0) ? 1 : 0) + (d.mantisOk(1) ? 1 : 0);
        if (n > 0) drain(d, 4f * n, dt);
    }

    /** Solar (so de dia, ao ar livre) e Reator Arc (sempre) recarregam a bateria; aviso unico quando a energia acaba. */
    private static void energyTick(ServerPlayer p, SandevistanData d, float dt) {
        if (d.batType != 0) {
            float gain = 0f;
            int st = d.extTier(SandevistanData.EXT_SOLAR);
            if (st != 0) {
                net.minecraft.world.level.Level lv = p.level();
                boolean sun = lv.isDay() && !lv.dimensionType().hasCeiling() && lv.canSeeSky(p.blockPosition().above());
                if (sun) gain += SandevistanData.SOLAR_REGEN[st - 1] * (lv.isRaining() ? 0.5f : 1f);
            }
            int rt = d.extTier(SandevistanData.EXT_REACTOR);
            if (rt != 0) gain += SandevistanData.REACTOR_REGEN[rt - 1];
            SandevistanLogic.addEnergy(d, gain * dt);
        }
        // aviso so quando a bateria estava com energia e acabou (sem HUD piscando, so uma mensagem no chat)
        boolean has = d.hasEnergy();
        if (d.batType == 0) {
            d.lowWarned = false;
        } else if (has) {
            d.lowWarned = true;
        } else if (d.lowWarned) {
            d.lowWarned = false;
            p.sendSystemMessage(Tx.t(ChatFormatting.RED, "battery.empty"));
        }
    }

    // ---------- Injetor automatico ----------
    /** Cada golpe com as laminas gasta 1 de durabilidade de cada lamina inteira. */
    public static void mantisWear(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        boolean broke = false;
        for (int i = 0; i < 2; i++) {
            if (!d.mantisOk(i)) continue;
            if (--d.mantisDur[i] <= 0) {
                d.mantisDur[i] = 0;
                broke = true;
            }
        }
        if (broke) SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "mantis.broken"));
        SandevistanLogic.send(p, d);
    }

    /** Material do reparo (parecido com o do craft da lamina de cada tier). */
    public static ItemStack[] repairCost(int tier) {
        return switch (tier) {
            case 1 -> new ItemStack[] {new ItemStack(ModRegistries.LIGA.get(), 1), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3)};
            case 2 -> new ItemStack[] {new ItemStack(ModRegistries.LIGA.get(), 1), new ItemStack(ModRegistries.COMP_MELHORADO.get(), 2)};
            case 3 -> new ItemStack[] {new ItemStack(ModRegistries.LIGA.get(), 2), new ItemStack(ModRegistries.COMP_ALTA.get(), 1)};
            default -> new ItemStack[] {new ItemStack(ModRegistries.LIGA.get(), 2), new ItemStack(net.minecraft.world.item.Items.ECHO_SHARD, 1)};
        };
    }

    public static int repairCode(int side) {
        return -(30000 + side);
    }

    private static int countOf(ServerPlayer p, net.minecraft.world.item.Item it) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    private static void takeOf(ServerPlayer p, net.minecraft.world.item.Item it, int n) {
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) return;
            if (s.is(it)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    private static void repair(ServerPlayer p, int side) {
        SandevistanData d = SandevistanData.get(p);
        if (side < 0 || side > 1) return;
        int slot = side == 0 ? SandevistanData.EXT_MANTIS_R : SandevistanData.EXT_MANTIS_L;
        if (d.ext[slot] == 0 || d.mantisDur[side] >= SandevistanData.MANTIS_MAX) return;
        boolean creative = p.getAbilities().instabuild;
        ItemStack[] cost = repairCost(d.extTier(slot));
        if (!creative) {
            for (ItemStack c : cost) {
                if (countOf(p, c.getItem()) < c.getCount()) {
                    SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "mantis.nomat"));
                    return;
                }
            }
            for (ItemStack c : cost) takeOf(p, c.getItem(), c.getCount());
        }
        d.mantisDur[side] = SandevistanData.MANTIS_MAX;
        SandevistanLogic.send(p, d);
        SandevistanLogic.bar(p, Tx.t(ChatFormatting.GREEN, "mantis.repaired"));
    }

    public static void toggleMantis(ServerPlayer p, SandevistanData d) {
        if (d.extTier(SandevistanData.EXT_MANTIS_R) == 0 && d.extTier(SandevistanData.EXT_MANTIS_L) == 0) return;
        d.mantisOn = !d.mantisOn;
        p.displayClientMessage(Tx.t(d.mantisOn ? ChatFormatting.GREEN : ChatFormatting.GRAY, d.mantisOn ? "mantis.on" : "mantis.off"), true);
        SandevistanLogic.send(p, d);
    }

    public static void toggleInjector(ServerPlayer p, SandevistanData d) {
        if (d.extTier(SandevistanData.EXT_INJECTOR) == 0) return;
        d.injOn = !d.injOn;
        p.displayClientMessage(Tx.t(d.injOn ? ChatFormatting.GREEN : ChatFormatting.GRAY, d.injOn ? "injector.on" : "injector.off"), true);
        SandevistanLogic.send(p, d);
    }

    /**
     * Confere uma vez por segundo. Seguranca: no maximo UMA injecao por conferencia; a Pilula so com a Insanidade alta, sem pilula
     * agindo e com espera propria; o Imunossupressor so com ciberpsicose alta (nunca com 1), sem dose ou sequencia em andamento
     * e com espera longa.
     */
    private static void injectorTick(ServerPlayer p, SandevistanData d, float dt) {
        if (d.injPillCd > 0f) d.injPillCd = Math.max(0f, d.injPillCd - dt);
        if (d.injImmunoCd > 0f) d.injImmunoCd = Math.max(0f, d.injImmunoCd - dt);
        int t = d.extTier(SandevistanData.EXT_INJECTOR);
        if (t == 0 || !d.injOn || SandevistanLogic.locked(d)) return;
        d.injT += dt;
        if (d.injT < 1f) return;
        d.injT = 0f;
        if (d.psy >= SandevistanData.INJ_PSY[t - 1] && d.injImmunoCd <= 0f && d.doses.isEmpty() && d.immuSeq == 0
                && find(p, ImmunoItem.class) != null) {
            d.injImmunoCd = 180f;
            ItemStack syr = find(p, ImmunoItem.class);
            if (SandevistanLogic.takeImmuno(p, ((ImmunoItem) syr.getItem()).tier())) {
                syr.shrink(1);
                SandevistanLogic.give(p, new ItemStack(ModRegistries.SERINGA.get()));
                p.displayClientMessage(Tx.t(ChatFormatting.AQUA, "injector.immuno"), true);
                p.level().playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.5f);
            }
            return;
        }
        if (d.ins >= SandevistanData.INJ_INS[t - 1] && d.insCalm <= 0f && d.injPillCd <= 0f
                && find(p, PillItem.class) != null) {
            d.injPillCd = 40f;
            ItemStack pill = find(p, PillItem.class);
            if (SandevistanLogic.takePill(p, ((PillItem) pill.getItem()).tier())) {
                pill.shrink(1);
                p.displayClientMessage(Tx.t(ChatFormatting.AQUA, "injector.pill"), true);
                p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
        }
    }

    /** Primeira pilula/seringa desse tipo (qualquer tier) no inventario. */
    private static ItemStack find(ServerPlayer p, Class<? extends net.minecraft.world.item.Item> cls) {
        for (ItemStack s : p.getInventory().items) {
            if (!s.isEmpty() && cls.isInstance(s.getItem())) return s;
        }
        return null;
    }

    // ---------- Laminas Mantis ----------
    /** Dano extra corpo a corpo das laminas (como uma espada de ferro/diamante/netherite); nao soma com ferramenta na mao. */
    public static float mantisBonus(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        int r = d.mantisOk(0) ? d.extTier(SandevistanData.EXT_MANTIS_R) : 0;
        int l = d.mantisOk(1) ? d.extTier(SandevistanData.EXT_MANTIS_L) : 0;
        if ((r == 0 && l == 0) || !d.mantisOn || !d.hasEnergy()) return 0f;
        if (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.TieredItem) return 0f;
        float dmg = SandevistanData.MANTIS_DAMAGE[Math.max(r, l) - 1] + (r != 0 && l != 0 ? 1f : 0f);
        return dmg * p.getAttackStrengthScale(0.5f);
    }

    private static int altitude(ServerPlayer p) {
        BlockPos base = p.blockPosition();
        for (int i = 1; i <= 8; i++) {
            BlockPos b = base.below(i);
            if (!p.level().getBlockState(b).getCollisionShape(p.level(), b).isEmpty()) return i;
        }
        return 99;
    }

    private static int bootTier(SandevistanData d) {
        return Math.max(d.extTier(SandevistanData.EXT_BOOT_R), d.extTier(SandevistanData.EXT_BOOT_L));
    }

    /** Jetpack: voo livre (pular duas vezes); botas nos dois pes: voo curto perto do chao. Gasta energia so voando. */
    private static void flight(ServerPlayer p, SandevistanData d, float dt) {
        d.flyingNow = false;
        if (p.isCreative() || p.isSpectator()) return;
        int jt = d.extTier(SandevistanData.EXT_JET);
        boolean jet = jt != 0;
        boolean boots = d.ext[SandevistanData.EXT_BOOT_R] != 0 && d.ext[SandevistanData.EXT_BOOT_L] != 0;
        boolean can = d.hasEnergy() && (jet || boots);
        Abilities ab = p.getAbilities();
        if (can) {
            if (!ab.mayfly) {
                ab.mayfly = true;
                d.flightGranted = true;
                p.onUpdateAbilities();
            }
            if (ab.flying) {
                d.flyingNow = true;
                int bt = Math.max(1, bootTier(d));
                // com o Sandevistan ligado o voo e mais rapido e gasta metade da energia
                float base = jet ? SandevistanData.JET_SPEED[jt - 1] : SandevistanData.BOOT_FLY_SPEED[bt - 1];
                float speed = d.active ? base * 1.8f : base;
                if (Math.abs(ab.getFlyingSpeed() - speed) > 0.0001f) {
                    ab.setFlyingSpeed(speed);
                    p.onUpdateAbilities();
                }
                if (!jet && altitude(p) > SandevistanData.BOOT_ALTITUDE[bt - 1]) {
                    Vec3 m = p.getDeltaMovement();
                    p.setDeltaMovement(m.x, Math.min(m.y, -0.15), m.z);
                    p.hurtMarked = true;
                }
                drain(d, (jet ? SandevistanData.JET_COST[jt - 1] : 9f) * (d.active ? 0.5f : 1f), dt);
                p.fallDistance = 0f;
            }
        } else if (d.flightGranted) {
            ab.mayfly = false;
            ab.flying = false;
            ab.setFlyingSpeed(0.05f);
            d.flightGranted = false;
            p.onUpdateAbilities();
        }
    }

    /** Botas: planar ao cair de longe (efeito de queda lenta), gastando energia. */
    private static void glide(ServerPlayer p, SandevistanData d, float dt) {
        int boots = (d.ext[SandevistanData.EXT_BOOT_R] != 0 ? 1 : 0) + (d.ext[SandevistanData.EXT_BOOT_L] != 0 ? 1 : 0);
        if (boots == 0 || !d.hasEnergy() || p.onGround() || p.getAbilities().flying) return;
        if (p.fallDistance > (boots == 2 ? 3f : 6f) && p.getDeltaMovement().y < -0.4) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, true, false, false));
            drain(d, SandevistanData.BOOT_GLIDE_COST[Math.max(1, bootTier(d)) - 1] * boots, dt);
        }
    }

    private static void eyeTick(ServerPlayer p, SandevistanData d, float dt) {
        int eye = d.eye();
        if (!d.eyeOn) return;
        if ((eye != 2 && eye != 4) || !drain(d, eye == 2 ? 8f : 6f, dt)) {
            d.eyeOn = false;
            if (eye == 4 && !d.active) p.removeEffect(MobEffects.NIGHT_VISION);
            return;
        }
        if (eye == 4) {
            MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
            // renova antes de cair abaixo de 10 s (abaixo disso o jogo faz a visao noturna piscar)
            if (cur == null || cur.getDuration() < 300) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
        }
    }

    /** Implante de XP: +20/30/40% de experiencia (tier I/II/III), gastando energia por XP ganho. */
    public static int xpAmount(ServerPlayer p, int amount) {
        SandevistanData d = SandevistanData.get(p);
        int tier = d.extTier(SandevistanData.EXT_XP);
        if (tier == 0 || amount <= 0 || !d.hasEnergy()) return amount;
        int bonus = Math.round(amount * SandevistanData.XP_BONUS[tier - 1]);
        if (bonus > 0 && drain(d, bonus * 2f, 1f)) return amount + bonus;
        return amount;
    }

    /** Menos dano de queda com botas (so com energia): um pe 50/60/70%, dois pes 75/80/85% (tier I/II/III). */
    public static float fallMultiplier(SandevistanData d) {
        int boots = (d.ext[SandevistanData.EXT_BOOT_R] != 0 ? 1 : 0) + (d.ext[SandevistanData.EXT_BOOT_L] != 0 ? 1 : 0);
        if (boots == 0 || !d.hasEnergy()) return 1f;
        int i = Math.max(1, bootTier(d)) - 1;
        return (float) (boots == 2 ? SandevistanData.BOOT_FALL_TWO[i] : SandevistanData.BOOT_FALL_ONE[i]);
    }
}
