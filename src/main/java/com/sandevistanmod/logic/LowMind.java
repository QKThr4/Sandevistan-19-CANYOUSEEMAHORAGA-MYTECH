package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Sintomas leves da Mente maxima PERMANENTE baixa (abaixo de 60%, 30% e 10%): ciberpsicose que sobe devagar ate um
 * teto baixo, fadiga neural, reflexos falhos, esquecimento e o mundo desconfiando do jogador. Os efeitos de tela e
 * som ficam no cliente (LowMindFx). Tudo e proposital leve: o jogador precisa conseguir jogar mesmo com a Mente em 0%.
 */
public final class LowMind {
    private LowMind() {}

    private static final ResourceLocation FATIGUE =
            ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "low_mind_fatigue");

    /** A Mente maxima baixa NAO mexe mais na ciberpsicose: sem teto que sobe sozinho (era o bug da psicose travada). */
    public static float creepCap(SandevistanData d) {
        return 0f;
    }

    public static float gainFactor(SandevistanData d) {
        return 1f;
    }

    public static float immunoFactor(SandevistanData d) {
        return 1f;
    }

    private static float next(ServerPlayer p, float base) {
        return base * (0.7f + p.getRandom().nextFloat() * 0.6f);
    }

    /** Roda todo tick do jogador; dt em segundos reais. */
    public static void tick(ServerPlayer p, SandevistanData d, float dt) {
        int t = d.lowTier();
        // fadiga neural: ataque um pouco mais lento (4%, 8% ou 12%)
        SandevistanLogic.setMod(p, Attributes.ATTACK_SPEED, FATIGUE, -0.04 * t,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, t > 0);
        // desfaz a troca de hotbar mesmo se a Mente subir de volta
        float[] w = d.lowT;
        if (d.swapA >= 0) {
            w[5] += dt;
            if (w[5] >= 1f) revertSwap(p, d);
        }
        if (t == 0 || !p.isAlive() || p.isSpectator()) {
            w[0] = 0f;
            return;
        }

        // reflexos falhos: o item da mao fica indisponivel por meio segundo (nada cai no chao)
        w[1] += dt;
        if (w[2] <= 0f) w[2] = next(p, t == 1 ? 90f : t == 2 ? 60f : 40f);
        if (w[1] >= w[2]) {
            w[1] = 0f;
            w[2] = next(p, t == 1 ? 90f : t == 2 ? 60f : 40f);
            ItemStack held = p.getMainHandItem();
            if (!held.isEmpty() && !SandevistanLogic.locked(d)) {
                p.stopUsingItem();
                p.getCooldowns().addCooldown(held.getItem(), 10);
            }
        }

        // esquecimento: dois espacos da hotbar trocam por 1 segundo e voltam
        if (t >= 2 && d.swapA < 0) {
            w[3] += dt;
            if (w[4] <= 0f) w[4] = next(p, t == 2 ? 240f : 150f);
            if (w[3] >= w[4]) {
                w[3] = 0f;
                w[4] = next(p, t == 2 ? 240f : 150f);
                swap(p, d);
            }
        }

        // o mundo desconfia (a cada segundo): aldeoes e animais se afastam, mobs hostis notam de mais longe
        if (t >= 2) {
            w[6] += dt;
            if (w[6] >= 1f) {
                w[6] = 0f;
                world(p, t);
            }
        }
    }

    private static void swap(ServerPlayer p, SandevistanData d) {
        if (p.containerMenu != p.inventoryMenu) return;
        int a = p.getRandom().nextInt(9);
        int b = (a + 1 + p.getRandom().nextInt(8)) % 9;
        ItemStack sa = p.getInventory().getItem(a);
        ItemStack sb = p.getInventory().getItem(b);
        if (sa.isEmpty() && sb.isEmpty()) return;
        p.getInventory().setItem(a, sb);
        p.getInventory().setItem(b, sa);
        d.swapA = a;
        d.swapB = b;
        d.swapRefA = sa;
        d.swapRefB = sb;
        d.lowT[5] = 0f;
    }

    /** Volta a troca so se ninguem mexeu nos dois espacos desde entao. */
    private static void revertSwap(ServerPlayer p, SandevistanData d) {
        int a = d.swapA;
        int b = d.swapB;
        d.swapA = -1;
        d.swapB = -1;
        if (a < 0 || b < 0) return;
        if (p.getInventory().getItem(a) == d.swapRefB && p.getInventory().getItem(b) == d.swapRefA) {
            p.getInventory().setItem(a, d.swapRefA);
            p.getInventory().setItem(b, d.swapRefB);
        }
        d.swapRefA = ItemStack.EMPTY;
        d.swapRefB = ItemStack.EMPTY;
    }

    private static void world(ServerPlayer p, int t) {
        double flee = 6.0;
        double aggro = t == 2 ? 24.0 : 28.0;
        boolean harmless = p.isCreative();
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(aggro))) {
            double dist = m.distanceTo(p);
            if (m instanceof Villager v) {
                if (v.getVillagerData().getProfession() == ModRegistries.RIPPERDOC.get()) continue;
                if (dist < flee && p.getRandom().nextFloat() < 0.4f) away(m, p);
            } else if (m instanceof Animal) {
                if (dist < flee && p.getRandom().nextFloat() < 0.4f) away(m, p);
            } else if (m instanceof Monster && !harmless && m.getTarget() == null && dist <= aggro
                    && p.getRandom().nextFloat() < 0.25f) {
                m.setTarget(p);
            }
        }
    }

    private static void away(Mob m, ServerPlayer p) {
        Vec3 dir = m.position().subtract(p.position());
        if (dir.lengthSqr() < 1.0e-4) return;
        dir = dir.normalize().scale(7.0);
        m.getNavigation().moveTo(m.getX() + dir.x, m.getY(), m.getZ() + dir.z, 1.1);
    }
}
