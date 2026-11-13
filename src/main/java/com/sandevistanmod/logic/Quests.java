package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.Net;
import com.sandevistanmod.registry.ModRegistries;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Missoes do Ripperdoc: cada uma libera os crafts de um tier (II, III e IV) na bancada.
 * Sao longas de proposito: matar robos, usar o Sandevistan por bastante tempo, entregar pecas e matar chefes.
 */
public final class Quests {
    private Quests() {}

    public enum Kind { ROBOT, ENTITY, ACTIVE, ITEM }

    /** Um objetivo. Para ACTIVE o alvo e em segundos. */
    public record Obj(Kind kind, int target, EntityType<?> entity, Supplier<Item> item) {
        public Component label() {
            return switch (kind) {
                case ROBOT -> Tx.t("quest.obj.robot", target);
                case ENTITY -> Tx.t("quest.obj.kill", target, entity.getDescription());
                case ACTIVE -> Tx.t("quest.obj.active", Math.max(1, target / 60));
                case ITEM -> Tx.t("quest.obj.item", target, item.get().getDescription());
            };
        }

        /** Valor mostrado na barra (ACTIVE em minutos nao: em segundos, convertido para texto pela tela). */
        public int shown(int progress) {
            return Math.min(progress, target);
        }
    }

    public record Quest(int tier, List<Obj> objs, int emeralds, int levels) {}

    private static Quest[] CACHE;

    private static Obj robots(int n) {
        return new Obj(Kind.ROBOT, n, null, null);
    }

    private static Obj kill(EntityType<?> t, int n) {
        return new Obj(Kind.ENTITY, n, t, null);
    }

    private static Obj active(int seconds) {
        return new Obj(Kind.ACTIVE, seconds, null, null);
    }

    private static Obj item(Supplier<Item> it, int n) {
        return new Obj(Kind.ITEM, n, null, it);
    }

    private static Quest[] build() {
        return new Quest[] {
                new Quest(2, List.of(robots(12), active(180), item(ModRegistries.SUCATA, 20), item(ModRegistries.COMP_MELHORADO, 6),
                        item(ModRegistries.CAPACITOR, 4)), 24, 5),
                new Quest(3, List.of(robots(30), kill(EntityType.ELDER_GUARDIAN, 1), active(600), item(ModRegistries.COMP_ALTA, 6),
                        item(ModRegistries.NUCLEO_COMP, 2), item(ModRegistries.FLUIDO, 4)), 48, 10),
                new Quest(4, List.of(robots(100), kill(EntityType.WARDEN, 1), kill(EntityType.WITHER, 1), active(1800),
                        item(ModRegistries.COMP_ALTA, 24), item(ModRegistries.NUCLEO_COMP, 6)), 96, 20)};
    }

    /** Missao que libera este tier (2 a 4), ou null. */
    public static Quest of(int tier) {
        if (tier < 2 || tier > 4) return null;
        if (CACHE == null) CACHE = build();
        return CACHE[tier - 2];
    }

    /** Missao atual do jogador (a da proxima licenca), ou null se ja tem todas. */
    public static Quest current(SandevistanData d) {
        return of(d.licence + 1);
    }

    public static boolean isRobot(LivingEntity e) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key.getNamespace().equals(SandevistanMod.MODID) && key.getPath().startsWith("robo_");
    }

    // ---------- servidor ----------
    /** Codigos vindos do Ripperdoc: 0 aceitar, 1 entregar pecas, 2 concluir. */
    public static void action(ServerPlayer p, int code) {
        SandevistanData d = SandevistanData.get(p);
        Quest q = current(d);
        if (q == null) {
            RipperdocTalk.line(p, "quest.rip.max");
            return;
        }
        switch (code) {
            case 0 -> {
                if (d.qOn) return;
                d.qOn = true;
                java.util.Arrays.fill(d.qp, 0);
                RipperdocTalk.line(p, "quest.rip.accept." + q.tier());
                SandevistanLogic.send(p, d);
            }
            case 1 -> deliver(p, d, q);
            case 2 -> complete(p, d, q);
            default -> { }
        }
    }

    private static void deliver(ServerPlayer p, SandevistanData d, Quest q) {
        if (!d.qOn) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "quest.notaccepted"));
            return;
        }
        boolean any = false;
        for (int i = 0; i < q.objs().size(); i++) {
            Obj o = q.objs().get(i);
            if (o.kind() != Kind.ITEM) continue;
            int need = o.target() - d.qp[i];
            if (need <= 0) continue;
            int took = take(p, o.item().get(), need);
            if (took > 0) {
                d.qp[i] += took;
                any = true;
            }
        }
        if (any) {
            RipperdocTalk.line(p, "quest.rip.deliver");
            SandevistanLogic.send(p, d);
        } else {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.YELLOW, "quest.nodeliver"));
        }
    }

    private static int take(ServerPlayer p, Item item, int max) {
        int left = max;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (s.is(item)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        return max - left;
    }

    public static boolean done(SandevistanData d, Quest q) {
        for (int i = 0; i < q.objs().size(); i++) if (d.qp[i] < q.objs().get(i).target()) return false;
        return true;
    }

    private static void complete(ServerPlayer p, SandevistanData d, Quest q) {
        if (!d.qOn || !done(d, q)) {
            SandevistanLogic.bar(p, Tx.t(ChatFormatting.RED, "quest.incomplete"));
            return;
        }
        d.licence = q.tier();
        d.qOn = false;
        java.util.Arrays.fill(d.qp, 0);
        SandevistanLogic.give(p, new ItemStack(Items.EMERALD, q.emeralds()));
        p.giveExperienceLevels(q.levels());
        RipperdocTalk.line(p, "quest.rip.done." + q.tier());
        SandevistanLogic.chat(p, Tx.t(ChatFormatting.GREEN, "quest.unlocked", q.tier()));
        SandevistanLogic.send(p, d);
    }

    /** O jogador matou algo: conta para a missao aceita. */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        SandevistanData d = SandevistanData.get(p);
        if (!d.qOn) return;
        Quest q = current(d);
        if (q == null) return;
        boolean changed = false;
        for (int i = 0; i < q.objs().size(); i++) {
            Obj o = q.objs().get(i);
            boolean hit = (o.kind() == Kind.ROBOT && isRobot(victim)) || (o.kind() == Kind.ENTITY && victim.getType() == o.entity());
            if (hit && d.qp[i] < o.target()) {
                d.qp[i]++;
                changed = true;
            }
        }
        if (changed) {
            if (done(d, q)) SandevistanLogic.chat(p, Tx.t(ChatFormatting.AQUA, "quest.ready"));
            Net.sync(p);
        }
    }

    /** Segundos com o Sandevistan ligado contam para a missao. */
    public static void tick(ServerPlayer p, SandevistanData d, float dt) {
        if (!d.qOn || !d.active) return;
        Quest q = current(d);
        if (q == null) return;
        d.qActiveAcc += dt;
        if (d.qActiveAcc < 1f) return;
        int whole = (int) d.qActiveAcc;
        d.qActiveAcc -= whole;
        for (int i = 0; i < q.objs().size(); i++) {
            Obj o = q.objs().get(i);
            if (o.kind() == Kind.ACTIVE && d.qp[i] < o.target()) d.qp[i] = Math.min(o.target(), d.qp[i] + whole);
        }
    }

    /** Tier de craft exigido para este item (1 se nao for implante de tier). */
    public static int needed(ItemStack st) {
        Item it = st.getItem();
        if (it instanceof ExtItem e) return e.tier();
        if (it instanceof ImplantItem im) return im.tier();
        if (it instanceof ChipItem c) return c.tier();
        if (it instanceof MedTier m) return m.tier();
        return 1;
    }
}
