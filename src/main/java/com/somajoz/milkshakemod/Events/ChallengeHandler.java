package com.somajoz.milkshakemod.Events;

import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.Networking.PacketHandler;
import com.somajoz.milkshakemod.Networking.TimerSyncPacket;
import com.somajoz.milkshakemod.data_attachments.ChallengeData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = MilkShake.MODID)
public class ChallengeHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        ServerLevel overworld = server.overworld();
        ChallengeData data = ChallengeData.get(overworld);

        // --- РАЗМИНКА ---
        if (!data.isActive && data.isFirstStart) {
            data.warmupTimer--;

            if (data.warmupTimer % 20 == 0) {
                // ДЕБАГ: Видим ли мы тик на сервере?
                System.out.println("[SERVER] Tick Warmup: " + data.warmupTimer);

                PacketHandler.sendToAll(new TimerSyncPacket(data.warmupTimer, true, false), server);
                data.setDirty();

                if (data.warmupTimer == 1200) {
                    server.getPlayerList().broadcastSystemMessage(Component.literal("До начала испытания 1 минута!").withStyle(ChatFormatting.YELLOW), false);
                }
            }

            if (data.warmupTimer <= 0) {
                data.isFirstStart = false;
                startNewChallenge(server, data);
            }
            return;
        }

        // --- АКТИВНЫЙ ЧЕЛЛЕНДЖ ---
        if (data.isActive) {
            data.timer--;

            if (data.timer % 20 == 0) {
                // ДЕБАГ: Видим ли мы тик игры?
                System.out.println("[SERVER] Tick Game: " + data.timer);

                PacketHandler.sendToAll(new TimerSyncPacket(data.timer, false, true), server);
                data.setDirty();

                if (data.timer == 1200) {
                    server.getPlayerList().broadcastSystemMessage(Component.literal("Осталась 1 минута!").withStyle(ChatFormatting.RED), false);
                }
            }

            if (data.timer <= 0) {
                failChallenge(server);
            }
        }
    }

    public static void startNewChallenge(MinecraftServer server, ChallengeData data) {
        data.targetItem = ItemPool.getRandomItem(data.unlockedDimensions);
        data.timer = ChallengeData.CHALLENGE_DURATION;
        data.isActive = true;
        data.setDirty();

        ItemStack stack = new ItemStack(data.targetItem);
        Component itemName = stack.getHoverName();
        String itemId = BuiltInRegistries.ITEM.getKey(data.targetItem).getPath();
        if (itemId.contains("music_disc") || itemId.contains("pottery_sherd")) {
            itemName = itemName.copy().append(Component.literal(" (" + itemId + ")").withStyle(ChatFormatting.GRAY));
        }

        Component msg = Component.literal("НОВОЕ ЗАДАНИЕ: Принеси ")
                .append(itemName)
                .append(" за 10 минут! Напиши /finish, держа его в руке.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        server.getPlayerList().broadcastSystemMessage(msg, false);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("Найди: ").append(itemName).withStyle(ChatFormatting.YELLOW)
            ));
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        }
    }

    private static void failChallenge(MinecraftServer server) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("ВРЕМЯ ВЫШЛО! МИР БУДЕТ УНИЧТОЖЕН...").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false
        );
        server.halt(false);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        ChallengeData data = ChallengeData.get(server.overworld());
        String advId = event.getAdvancement().id().toString();

        if (advId.equals("minecraft:story/enter_the_nether")) {
            if (data.unlockedDimensions.add("minecraft:the_nether")) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("Незер разблокирован!").withStyle(ChatFormatting.RED), false);
                data.setDirty();
            }
        }
        if (advId.equals("minecraft:story/enter_the_end")) {
            if (data.unlockedDimensions.add("minecraft:the_end")) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("Энд разблокирован!").withStyle(ChatFormatting.DARK_PURPLE), false);
                data.setDirty();
            }
        }
    }
}
