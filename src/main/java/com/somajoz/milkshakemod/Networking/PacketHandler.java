package com.somajoz.milkshakemod.Networking;

import com.somajoz.milkshakemod.Client.ClientModEvents;
import com.somajoz.milkshakemod.MilkShake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // Регистрация существующего пакета (TargetSync)
        registrar.playToClient(
                TargetSyncPacket.TYPE,
                TargetSyncPacket.STREAM_CODEC,
                TargetSyncPacket::handle
        );

        // --- НОВЫЙ ПАКЕТ ТАЙМЕРА ---
        registrar.playToClient(
                TimerSyncPacket.TYPE,
                TimerSyncPacket.STREAM_CODEC,
                (payload, context) -> {
                    // Обработка на клиенте
                    context.enqueueWork(() -> {
                        ClientModEvents.updateTimer(
                                payload.timeRemaining(),
                                payload.isWarmup(),
                                payload.isActive()
                        );
                    });
                }
        );
    }

    // Отправка статуса "Ты цель!" (существующий метод)
    public static void sendToPlayer(ServerPlayer player, boolean isTargeted) {
        System.out.println("DEBUG: Sending packet to player " + player.getName().getString() + " -> " + isTargeted);
        PacketDistributor.sendToPlayer(player, new TargetSyncPacket(isTargeted));
    }

    // --- НОВЫЙ МЕТОД ДЛЯ ТАЙМЕРА ---
    // Отправляет время всем игрокам на сервере
    public static void sendToAll(TimerSyncPacket packet, MinecraftServer server) {
        // PacketDistributor.sendToAllAround(...) сложнее, проще перебором или sendToAllPlayers
        // В 1.21.1: PacketDistributor.sendToAllPlayers(packet)

        PacketDistributor.sendToAllPlayers(packet);
    }
}
