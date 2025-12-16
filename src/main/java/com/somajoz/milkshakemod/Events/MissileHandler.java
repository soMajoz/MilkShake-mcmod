package com.somajoz.milkshakemod.Events;

import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.Networking.PacketHandler;
import com.somajoz.milkshakemod.data_attachments.MissileData;
import com.somajoz.milkshakemod.data_attachments.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = MilkShake.MODID)
public class MissileHandler {

    private static UUID currentMissileUUID = null;
    private static final Random random = new Random();

    // --- МЕТОД ДЛЯ КОМАНДЫ /target ---
    public static boolean spawnMissileAtEdge(ServerPlayer target) {
        ServerLevel level = target.level(); // Исправлен вызов

        int renderDistance = level.getServer().getPlayerList().getViewDistance();
        int distanceBlocks = (renderDistance * 12);

        double angle = random.nextDouble() * 2 * Math.PI;

        double x = target.getX() + Math.cos(angle) * distanceBlocks;
        double z = target.getZ() + Math.sin(angle) * distanceBlocks;

        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int)x, (int)z);
        BlockPos spawnPos = new BlockPos((int)x, y + 10, (int)z);

        Villager missile = EntityType.VILLAGER.spawn(
                level,
                spawnPos,
                EntitySpawnReason.COMMAND
        );

        if (missile != null) {
            activateMissile(missile, target);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) level;

        if (currentMissileUUID == null && serverLevel.getGameTime() % 100 == 0) {
            for (ServerPlayer player : serverLevel.players()) {
                // Ищем в радиусе 1000 блоков
                AABB searchBox = player.getBoundingBox().inflate(550);

                List<Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, searchBox);

                if (!villagers.isEmpty()) {
                    activateMissile(villagers.get(0), player);
                    break;
                }
            }
        }
    }

    private static void activateMissile(Villager villager, ServerPlayer target) {
        currentMissileUUID = villager.getUUID();

        MissileData data = villager.getData(ModAttachments.MISSILE_DATA);
        data.setTarget(target.getUUID(), villager.position(), target.position(), villager.level().getGameTime());

        target.setData(ModAttachments.IS_TARGETED, true);
        PacketHandler.sendToPlayer(target, true);

        villager.setNoGravity(true);
        villager.noPhysics = true;
    }

    @SubscribeEvent
    public static void onVillagerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        if (currentMissileUUID != null && !villager.getUUID().equals(currentMissileUUID)) return;

        MissileData data = villager.getData(ModAttachments.MISSILE_DATA);
        if (data.targetUUID == null) return;

        long flightTime = villager.level().getGameTime() - data.startTime;

        // --- ФИКСИРОВАННАЯ СКОРОСТЬ (3 СЕКУНДЫ) ---
        // 3 секунды = 60 тиков.
        // Независимо от расстояния, полет займет ровно 60 тиков.
        double duration = 60.0;

        // Если по какой-то причине (лаг) прошло больше времени -> взрыв
        if (flightTime > duration + 20) {
            explode(villager);
            return;
        }

        double progress = (double) flightTime / duration;

        if (progress >= 1.0) {
            explode(villager);
            return;
        }

        // --- ТРАЕКТОРИЯ ---
        Vec3 start = data.startPos;
        Vec3 end = data.targetPos;

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        // Делаем дугу более плоской для скорости
        double arcHeight = 30.0;
        double arcOffset = Math.sin(progress * Math.PI) * arcHeight;

        Vec3 newPos = new Vec3(
                start.x + dx * progress,
                start.y + dy * progress + arcOffset,
                start.z + dz * progress
        );

        villager.setPos(newPos);

        // --- РАЗРУШЕНИЕ ---
        AABB box = villager.getBoundingBox();
        BlockPos.betweenClosedStream(box).forEach(pos -> {
            if (!villager.level().isEmptyBlock(pos)) {
                villager.level().destroyBlock(pos, false);
            }
        });

        // --- СТОЛКНОВЕНИЯ ---
        List<net.minecraft.world.entity.Entity> collisions = villager.level().getEntities(villager, box.inflate(1.0), e -> e != villager);
        if (!collisions.isEmpty()) {
            explode(villager);
        }
    }

    private static void explode(Villager villager) {
        Level level = villager.level();
        MissileData data = villager.getData(ModAttachments.MISSILE_DATA);

        if (data.targetUUID != null && level instanceof ServerLevel serverLevel) {
            ServerPlayer target = serverLevel.getServer().getPlayerList().getPlayer(data.targetUUID);
            if (target != null) {
                target.setData(ModAttachments.IS_TARGETED, false);
                PacketHandler.sendToPlayer(target, false);
            }
        }

        level.explode(villager, villager.getX(), villager.getY(), villager.getZ(), 6.0f, Level.ExplosionInteraction.BLOCK);
        villager.discard();

        if (currentMissileUUID != null && currentMissileUUID.equals(villager.getUUID())) {
            currentMissileUUID = null;
        }
    }
}
