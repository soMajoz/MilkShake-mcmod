package com.somajoz.milkshakemod.Client;

import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.data_attachments.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public class ClientModEvents {

    public static int clientTimer = 0;
    public static boolean isWarmup = false;
    public static boolean isActive = false;

    public static void updateTimer(int time, boolean warmup, boolean active) {
        clientTimer = time;
        isWarmup = warmup;
        isActive = active;
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Регистрируем ОДИН слой для всего мода (Overlay)
        Identifier overlayId = Identifier.fromNamespaceAndPath(MilkShake.MODID, "main_overlay");

        event.registerAboveAll(overlayId, (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.options.hideGui) return; // Скрываем по F1

            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            // ----------------------------------------------------
            // 1. ОТРИСОВКА ВОЗДУШНОЙ ТРЕВОГИ
            // ----------------------------------------------------
            boolean isTargeted = mc.player.getData(ModAttachments.IS_TARGETED);

            if (isTargeted) {
                String text = "Повітряна тривога";
                int color = 0xFFFF0000;

                int textWidth = mc.font.width(text);
                float targetWidth = width * 0.6f;
                float scale = targetWidth / (float) textWidth;

                float x = (width / 2.0f) / scale - (textWidth / 2.0f);
                float y = (height * 0.2f) / scale;

                var pose = guiGraphics.pose();
                pose.pushMatrix();
                pose.scale(scale, scale);
                guiGraphics.drawString(mc.font, text, (int)x, (int)y, color, true);
                pose.popMatrix();
            }

            // ----------------------------------------------------
            // 2. ОТРИСОВКА ТАЙМЕРА (В том же слое!)
            // ----------------------------------------------------
            if (isActive || isWarmup) {
                int secondsLeft = clientTimer / 20;
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft % 60;

                String timeString = String.format("%02d:%02d", minutes, seconds);
                String label = isWarmup ? "Старт: " : "Время: ";

                // Цвет: Желтый или Красный (с полной непрозрачностью FF)
                int color = isWarmup ? 0xFFFFFF55 : 0xFFFF5555;

                if (secondsLeft <= 60 && !isWarmup) {
                    color = 0xFFFF0000; // Ярко-красный
                    if (secondsLeft % 2 == 0) color = 0xFFFFFFFF; // Мигание белым
                }

                String fullText = label + timeString;
                int textWidth = mc.font.width(fullText);

                // Позиция: Правый верхний угол (10 пикс от края)
                // Опускаем чуть ниже (Y=15), чтобы не слипалось с краем
                int x = width - textWidth - 10;
                int y = 15;

                // Рисуем полупрозрачный фон под таймером (для лучшей читаемости)
                // fill(x1, y1, x2, y2, colorARGB)
                guiGraphics.fill(x - 4, y - 4, x + textWidth + 4, y + 12, 0x80000000);

                // Рисуем текст
                guiGraphics.drawString(mc.font, fullText, x, y, color, true);
            }
        });
    }
}
