package com.somajoz.milkshakemod.data_attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class MissileData {
    // Делаем поля public, чтобы к ним был прямой доступ из Handler
    public UUID targetUUID;
    public Vec3 startPos;
    public Vec3 targetPos;
    public long startTime;

    // Конструктор по умолчанию
    public MissileData() {
        this.targetUUID = UUID.randomUUID();
        this.startPos = Vec3.ZERO;
        this.targetPos = Vec3.ZERO;
        this.startTime = 0;
    }

    public MissileData(UUID targetUUID, Vec3 startPos, Vec3 targetPos, long startTime) {
        this.targetUUID = targetUUID;
        this.startPos = startPos;
        this.targetPos = targetPos;
        this.startTime = startTime;
    }

    // Метод для удобной установки параметров (исправляет 'Cannot resolve method setTarget')
    public void setTarget(UUID target, Vec3 start, Vec3 end, long time) {
        this.targetUUID = target;
        this.startPos = start;
        this.targetPos = end;
        this.startTime = time;
    }

    // В файле MissileData.java
    public static final Codec<MissileData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("targetUUID").forGetter(d -> d.targetUUID),
            Vec3.CODEC.fieldOf("startPos").forGetter(d -> d.startPos),
            Vec3.CODEC.fieldOf("targetPos").forGetter(d -> d.targetPos),
            Codec.LONG.fieldOf("startTime").forGetter(d -> d.startTime)
    ).apply(instance, MissileData::new));
}
