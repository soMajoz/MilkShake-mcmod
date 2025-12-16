package com.somajoz.milkshakemod.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.somajoz.milkshakemod.Events.ChallengeHandler;
import com.somajoz.milkshakemod.Events.MissileHandler;
import com.somajoz.milkshakemod.Events.PlayerMissileHandler;
import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.data_attachments.ChallengeData;
import com.somajoz.milkshakemod.data_attachments.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MilkShake.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Команда /finish
        event.getDispatcher().register(
                Commands.literal("finish")
                        .executes(ModCommands::executeFinish)
        );

        // Команда /target (Доступ только для Majonezz777)
        event.getDispatcher().register(
                Commands.literal("target")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ModCommands::executeTarget))
        );

        // НОВАЯ КОМАНДА /jet <игрок>
        event.getDispatcher().register(
                Commands.literal("jet")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ModCommands::executeJet))
        );
    }

    // --- ЛОГИКА /jet ---
    private static int executeJet(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer source = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");

            // Проверка кулдауна (5 минут = 6000 тиков)
            long gameTime = source.level().getGameTime(); // Используем serverLevel() для надежности
            long lastUsed = source.getData(ModAttachments.LAST_JET_TIME);

            long cooldown = 6000; // 5 минут
            long timePassed = gameTime - lastUsed;

            if (timePassed < cooldown) {
                long secondsLeft = (cooldown - timePassed) / 20;
                source.sendSystemMessage(Component.literal("Двигатели перегреты! Остывание: " + secondsLeft + " сек.").withStyle(ChatFormatting.RED));
                return 0;
            }

            // Пытаемся запустить
            if (PlayerMissileHandler.launchPlayer(source, target)) {
                // Если успешно запустили - обновляем время последнего использования
                source.setData(ModAttachments.LAST_JET_TIME, gameTime);

                source.sendSystemMessage(Component.literal("🚀 ПОЕХАЛИ! Цель: " + target.getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                target.sendSystemMessage(Component.literal("⚠️ ВНИМАНИЕ! В вас летит игрок-ракета!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

                return Command.SINGLE_SUCCESS;
            } else {
                source.sendSystemMessage(Component.literal("Ошибка запуска: Нельзя запустить в себя!").withStyle(ChatFormatting.RED));
                return 0;
            }

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка: " + e.getMessage()));
            return 0;
        }
    }

    // --- ЛОГИКА /target ---
    private static int executeTarget(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();

            // ТИХАЯ ПРОВЕРКА НИКНЕЙМА
            if (!executor.getName().getString().equals("Majonezz777")) {
                // Молча выходим, если это не Majonezz777
                return 0;
            }

            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            boolean success = MissileHandler.spawnMissileAtEdge(targetPlayer);
            return success ? Command.SINGLE_SUCCESS : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // --- ЛОГИКА /finish ---
    private static int executeFinish(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = player.level().getServer(); // Лучше брать сервер напрямую
            ChallengeData data = ChallengeData.get(player.level()); // Используем serverLevel()

            ItemStack held = player.getMainHandItem();

            if (!data.isActive) {
                player.sendSystemMessage(Component.literal("Челлендж не активен!").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (held.getItem() == data.targetItem) {
                data.isActive = false;
                held.shrink(1);

                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("Игрок " + player.getName().getString() + " выполнил задание!").withStyle(ChatFormatting.GREEN), false
                );

                // Запуск следующего задания
                ChallengeHandler.startNewChallenge(server, data);

                return Command.SINGLE_SUCCESS;
            } else {
                ItemStack targetStack = new ItemStack(data.targetItem);
                Component itemName = targetStack.getHoverName();
                String itemId = BuiltInRegistries.ITEM.getKey(data.targetItem).getPath();
                if (itemId.contains("music_disc") || itemId.contains("pottery_sherd")) {
                    itemName = itemName.copy().append(Component.literal(" (" + itemId + ")").withStyle(ChatFormatting.GRAY));
                }
                player.sendSystemMessage(Component.literal("Не тот предмет! Нужно: ").append(itemName).withStyle(ChatFormatting.RED));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка: " + e.getMessage()));
            return 0;
        }
    }
}
