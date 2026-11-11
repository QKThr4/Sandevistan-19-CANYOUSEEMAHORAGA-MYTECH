package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 0 liga/desliga o Sandevistan, 1 aviso do doutor, 2/3 tira chip, 4/5 visibilidade, 6 tira a bateria. */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
