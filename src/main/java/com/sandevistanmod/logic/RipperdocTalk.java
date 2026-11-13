package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** O que o Ripperdoc diz ao jogador, conforme o momento (implantes demais, Mente baixa, ciberpsicose...). */
public final class RipperdocTalk {
    private RipperdocTalk() {}

    /** Quantas variacoes de fala existem para cada contexto. */
    private static int variants(String ctx) {
        return switch (ctx) {
            case "near" -> 3;
            case "sand" -> 1;
            case "load.after" -> 1;
            default -> 2;
        };
    }

    /** Fala fixa do Ripperdoc (uma unica variacao). */
    public static void line(ServerPlayer p, String key) {
        Component name = Component.literal("<").append(Tx.t("doc.name")).append(Component.literal("> "))
                .withStyle(ChatFormatting.RED);
        p.sendSystemMessage(Component.empty().append(name).append(Tx.t(ChatFormatting.WHITE, key)));
    }

    public static void say(ServerPlayer p, String ctx) {
        int n = variants(ctx);
        int pick = 1 + p.getRandom().nextInt(n);
        Component name = Component.literal("<").append(Tx.t("doc.name")).append(Component.literal("> "))
                .withStyle(ChatFormatting.RED);
        p.sendSystemMessage(Component.empty().append(name)
                .append(Tx.t(ChatFormatting.WHITE, "rip." + ctx + "." + pick)));
    }

    private static int implants(SandevistanData d) {
        int n = 0;
        for (int s = 0; s < SandevistanData.SLOT_COUNT; s++) if (d.has(s)) n++;
        for (int v : d.ext) if (v != 0) n++;
        return n;
    }

    /** Fala ao abrir a conversa, pelo contexto mais importante primeiro. */
    public static void greet(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        float perm = d.permMind();
        String ctx;
        if (perm < 10f) ctx = "greet.crit";
        else if (perm < 30f) ctx = "greet.low";
        else if (d.capacity() >= d.loadThreshold()) ctx = "greet.load";
        else if (implants(d) >= 5) ctx = "greet.many";
        else if (perm < 60f) ctx = "greet.mind";
        else if (d.psy > 0f) ctx = "greet.psy";
        else if (d.implanted) ctx = "sand";
        else if (implants(d) > 0) ctx = "greet.some";
        else ctx = "greet.none";
        say(p, ctx);
    }

    /** Fala logo depois de uma cirurgia. */
    public static void afterImplant(ServerPlayer p, SandevistanData d) {
        if (d.capacity() >= d.loadThreshold()) say(p, "load.after");
        else if (implants(d) >= 5) say(p, "after.many");
        else say(p, "after.ok");
    }

    public static void afterRemove(ServerPlayer p) {
        say(p, "remove");
    }
}
