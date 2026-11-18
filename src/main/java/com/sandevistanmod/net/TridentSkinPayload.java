package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor para o cliente: o tridente arremessado (uuid) e de Aco (1) ou Ciber (2) e deve usar a textura propria. */
public record TridentSkinPayload(UUID uuid, int kind) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TridentSkinPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "trident_skin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TridentSkinPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TridentSkinPayload::uuid, ByteBufCodecs.VAR_INT, TridentSkinPayload::kind, TridentSkinPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
