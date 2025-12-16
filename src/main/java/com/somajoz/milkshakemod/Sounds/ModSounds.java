package com.somajoz.milkshakemod.Sounds;

import com.somajoz.milkshakemod.MilkShake;
import net.minecraft.core.registries.BuiltInRegistries; // ПРАВИЛЬНЫЙ ИМПОРТ РЕГИСТРОВ
import net.minecraft.resources.Identifier; // Используем тот же класс, что и в PacketHandler
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    // ВАЖНО: Используем BuiltInRegistries.SOUND_EVENT (для ванильных регистров)
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MilkShake.MODID);

    public static final Supplier<SoundEvent> AIR_RAID_SIREN = registerSoundEvent("siren");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        // Используем метод, который сработал в TargetSyncPacket
        Identifier id = Identifier.fromNamespaceAndPath(MilkShake.MODID, name);

        // Создаем событие звука с фиксированной дальностью (или variable range)
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
