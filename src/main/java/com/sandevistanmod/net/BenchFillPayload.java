package com.sandevistanmod.net;

import com.sandevistanmod.SandevistanMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente para o servidor: preencher a bancada aberta com a receita de indice 'index' (usando itens do inventario). */
public record BenchFillPayload(int index) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BenchFillPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, "bench_fill"));
    public static final StreamCodec<ByteBuf, BenchFillPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(BenchFillPayload::new, BenchFillPayload::index);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
