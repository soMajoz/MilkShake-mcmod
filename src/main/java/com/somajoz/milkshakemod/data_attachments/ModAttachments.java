package com.somajoz.milkshakemod.data_attachments;

import com.mojang.serialization.Codec;
import com.somajoz.milkshakemod.MilkShake;
import net.minecraft.core.UUIDUtil; // Для UUID кодека
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MilkShake.MODID);

    // --- ДАННЫЕ ДЛЯ ОБЫЧНЫХ РАКЕТ (ЖИТЕЛЕЙ) ---
    public static final Supplier<AttachmentType<MissileData>> MISSILE_DATA = ATTACHMENT_TYPES.register(
            "missile_data", () -> AttachmentType.builder(MissileData::new)
                    .serialize(MissileData.CODEC.fieldOf("data"))
                    .build()
    );

    // Флаг цели (для игрока, в которого летит ракета)
    public static final Supplier<AttachmentType<Boolean>> IS_TARGETED = ATTACHMENT_TYPES.register(
            "is_targeted", () -> AttachmentType.<Boolean>builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("value"))
                    .build()
    );

    // --- НОВЫЕ ДАННЫЕ ДЛЯ ИГРОКА-РАКЕТЫ (/JET) ---

    // 1. Время последнего использования /jet (для кулдауна)
    // По умолчанию 0 (никогда не использовал)
    public static final Supplier<AttachmentType<Long>> LAST_JET_TIME = ATTACHMENT_TYPES.register(
            "last_jet_time", () -> AttachmentType.<Long>builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("time"))
                    .build()
    );

    // 2. Флаг: является ли игрок сейчас ракетой?
    public static final Supplier<AttachmentType<Boolean>> IS_PLAYER_MISSILE = ATTACHMENT_TYPES.register(
            "is_player_missile", () -> AttachmentType.<Boolean>builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("active"))
                    .build()
    );

    // 3. Цель полета игрока-ракеты (UUID жертвы)
    // По умолчанию null
    public static final Supplier<AttachmentType<UUID>> PLAYER_MISSILE_TARGET = ATTACHMENT_TYPES.register(
            "player_missile_target", () -> AttachmentType.<UUID>builder(() -> null)
                    .serialize(UUIDUtil.CODEC.optionalFieldOf("targetUUID", null)) // optionalFieldOf для безопасности, если null
                    .build()
    );
}
