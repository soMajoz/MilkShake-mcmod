package com.somajoz.milkshakemod;

import com.somajoz.milkshakemod.Client.ClientModEvents;
import com.somajoz.milkshakemod.Networking.PacketHandler;
import com.somajoz.milkshakemod.Sounds.ModSounds;
import com.somajoz.milkshakemod.data_attachments.ModAttachments; // Импортируем наш класс аттачментов
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(MilkShake.MODID)
public class MilkShake {
    public static final String MODID = "milkshake";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MilkShake(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Шина Forge (GAME BUS) - для игровых событий (ServerStarting, TickEvent и т.д.)
        NeoForge.EVENT_BUS.register(this);

        // 2. Аттачменты (MOD BUS)
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 3. РЕГИСТРАЦИЯ ПАКЕТОВ (MOD BUS)
        // Мы вручную говорим шине мода: "слушай этот метод регистрации"
        modEventBus.addListener(PacketHandler::register);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        // 4. РЕГИСТРАЦИЯ GUI (MOD BUS)
        // Тоже вручную. Это решает проблему с аннотациями раз и навсегда.
        modEventBus.addListener(ClientModEvents::registerGuiLayers);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
