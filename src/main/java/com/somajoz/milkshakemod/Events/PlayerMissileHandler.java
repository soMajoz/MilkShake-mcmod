package com.somajoz.milkshakemod.Events;

import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.data_attachments.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = MilkShake.MODID)
public class PlayerMissileHandler {

    public static boolean launchPlayer(ServerPlayer missile, ServerPlayer target) {
        if (missile.getUUID().equals(target.getUUID())) return false; // Нельзя в себя

        missile.setData(ModAttachments.IS_PLAYER_MISSILE, true);
        missile.setData(ModAttachments.PLAYER_MISSILE_TARGET, target.getUUID());

        // Подкидываем
        missile.setDeltaMovement(0, 2.0, 0);
        missile.hurtMarked = true; // Обновить движение

        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Проверяем, ракета ли это
        if (!player.getData(ModAttachments.IS_PLAYER_MISSILE)) return;

        UUID targetUUID = player.getData(ModAttachments.PLAYER_MISSILE_TARGET);
        if (targetUUID == null) {
            land(player);
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetUUID);

        if (target == null || !target.isAlive()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Цель потеряна!"));
            land(player);
            return;
        }

        // --- ЛОГИКА ПОЛЕТА ---
        Vec3 myPos = player.position();
        Vec3 targetPos = target.position();
        double dist = myPos.distanceTo(targetPos);

        if (dist < 3.0) {
            explode(player); // БУМ!
            return;
        }

        // Вектор к цели
        Vec3 dir = targetPos.subtract(myPos).normalize();

        // Скорость полета (быстрая, но управляемая)
        double speed = 2.5;

        // Игрок всегда летит к цели
        player.setDeltaMovement(dir.scale(speed));
        player.hurtMarked = true;

        // Отключаем урон от падения
        player.resetFallDistance();

        // Партиклы для красоты
        level.sendParticles(ParticleTypes.FLAME, myPos.x, myPos.y, myPos.z, 5, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, myPos.x, myPos.y, myPos.z, 5, 0.2, 0.2, 0.2, 0.05);
    }

    private static void land(ServerPlayer player) {
        player.setData(ModAttachments.IS_PLAYER_MISSILE, false);
        player.setData(ModAttachments.PLAYER_MISSILE_TARGET, null);
    }

    private static void explode(ServerPlayer player) {
        land(player); // Снимаем флаг ДО взрыва, но нужно сохранить иммунитет на тик

        // ВАЖНО: Мы используем специальный трюк.
        // Чтобы защитить игрока от взрыва, мы можем либо дать ему Resistance на секунду,
        // либо отменить урон в событии.
        // Выберем вариант с событием. Для этого снова ставим флаг, но другой (или используем тот же, снимая его позже).

        // Пусть флаг IS_PLAYER_MISSILE висит еще 1 тик (или снимаем его в ExplosionEvent)
        player.setData(ModAttachments.IS_PLAYER_MISSILE, true);

        // Взрыв! (Сильный, ломает блоки)
        player.level().explode(player, player.getX(), player.getY(), player.getZ(), 6.0f, Level.ExplosionInteraction.BLOCK);

        // Снимаем флаг сразу после создания взрыва (событие сработает синхронно внутри explode)
        player.setData(ModAttachments.IS_PLAYER_MISSILE, false);
    }

    // --- ОТМЕНА УРОНА ОТ ВЗРЫВА ---
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getDirectSourceEntity(); // Кто взорвался?

        // Если взорвался игрок (наш метод explode передает player как source)
        if (source instanceof ServerPlayer player) {
            // Проверяем, был ли он ракетой (мы специально оставили флаг true перед вызовом explode)
            if (player.getData(ModAttachments.IS_PLAYER_MISSILE)) {
                // Удаляем самого игрока из списка жертв
                event.getAffectedEntities().remove(player);
            }
        }
    }
}
