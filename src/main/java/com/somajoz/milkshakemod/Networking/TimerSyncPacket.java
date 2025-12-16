package com.somajoz.milkshakemod.Networking;

import com.somajoz.milkshakemod.MilkShake;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs; // ВАЖНЫЙ ИМПОРТ
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TimerSyncPacket(int timeRemaining, boolean isWarmup, boolean isActive) implements CustomPacketPayload {

    public static final Type<TimerSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MilkShake.MODID, "timer_sync"));

    public static final StreamCodec<ByteBuf, TimerSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TimerSyncPacket::timeRemaining,
            ByteBufCodecs.BOOL, TimerSyncPacket::isWarmup,
            ByteBufCodecs.BOOL, TimerSyncPacket::isActive,
            TimerSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
