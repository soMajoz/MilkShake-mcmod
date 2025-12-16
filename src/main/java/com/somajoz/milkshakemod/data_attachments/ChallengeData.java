package com.somajoz.milkshakemod.data_attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChallengeData extends SavedData {
    public static final int CHALLENGE_DURATION = 12000;

    public Item targetItem;
    public int timer;
    public Set<String> unlockedDimensions;
    public boolean isActive;

    // --- НОВЫЕ ПОЛЯ ---
    public int warmupTimer;    // Таймер разминки (2 минуты = 2400 тиков)
    public boolean isFirstStart; // Флаг: это самый первый запуск или нет?

    // Конструктор по умолчанию (для создания чистого экземпляра)
    public ChallengeData() {
        this(Items.AIR, 0, new HashSet<>(Set.of("minecraft:overworld")), false, 2400, true);
    }

    // Полный конструктор
    public ChallengeData(Item targetItem, int timer, Set<String> unlockedDimensions, boolean isActive, int warmupTimer, boolean isFirstStart) {
        this.targetItem = targetItem != null ? targetItem : Items.AIR;
        this.timer = timer;
        this.unlockedDimensions = unlockedDimensions != null ? unlockedDimensions : new HashSet<>();
        if (this.unlockedDimensions.isEmpty()) {
            this.unlockedDimensions.add("minecraft:overworld");
        }
        this.isActive = isActive;

        // Инициализация новых полей
        this.warmupTimer = warmupTimer;
        this.isFirstStart = isFirstStart;
    }

    // Обновленный CODEC
    public static final Codec<ChallengeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("targetItem", Items.AIR).forGetter(d -> d.targetItem),
            Codec.INT.optionalFieldOf("timer", 0).forGetter(d -> d.timer),

            Codec.STRING.listOf()
                    .optionalFieldOf("unlockedDimensions", List.of("minecraft:overworld"))
                    .xmap(
                            list -> (Set<String>) new HashSet<>(list),
                            set -> List.copyOf(set)
                    )
                    .forGetter(d -> d.unlockedDimensions),

            Codec.BOOL.optionalFieldOf("isActive", false).forGetter(d -> d.isActive),

            // Новые поля в кодеке
            Codec.INT.optionalFieldOf("warmupTimer", 2400).forGetter(d -> d.warmupTimer),
            Codec.BOOL.optionalFieldOf("isFirstStart", true).forGetter(d -> d.isFirstStart)

    ).apply(instance, ChallengeData::new));

    public static final SavedDataType<ChallengeData> TYPE = new SavedDataType<>(
            "milkshake_challenge",
            ChallengeData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public static ChallengeData get(ServerLevel level) {
        try {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ChallengeData();
        }
    }
}
