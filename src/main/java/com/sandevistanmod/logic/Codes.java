package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.registry.ModRegistries;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Codigos secretos (/sandevistan code ...): cada um da uma recompensa e vale uma vez por jogador em cada mundo. So guardamos o hash. */
public final class Codes {
    private Codes() {}

    private static final Map<String, Consumer<ServerPlayer>> REWARDS = Map.of(
            "0fae9a27a5c01be6ac4f6876606d402c21cf6668ad000b27d790ddda28c63c22", p -> SandevistanLogic.give(p, new ItemStack(ModRegistries.ESTABILIZADOR_3.get(), 6)));

    static String hash(String code) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(("sdv|" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Admin: libera de novo todos os codigos para o jogador. */
    public static boolean reset(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        d.usedCodes.clear();
        SandevistanLogic.send(p, d);
        p.sendSystemMessage(Tx.t(ChatFormatting.YELLOW, "code.reset"));
        return true;
    }

    /** Tenta resgatar o codigo digitado. Devolve true se foi aceito. */
    public static boolean redeem(ServerPlayer p, String raw) {
        String hs = hash(raw == null ? "" : raw.trim());
        Consumer<ServerPlayer> reward = REWARDS.get(hs);
        if (reward == null) {
            p.sendSystemMessage(Tx.t(ChatFormatting.RED, "code.invalid"));
            return false;
        }
        SandevistanData d = SandevistanData.get(p);
        if (!d.usedCodes.add(hs)) {
            p.sendSystemMessage(Tx.t(ChatFormatting.RED, "code.used"));
            return false;
        }
        reward.accept(p);
        SandevistanLogic.send(p, d);
        p.sendSystemMessage(Tx.t(ChatFormatting.GREEN, "code.ok"));
        return true;
    }
}
