package com.somajoz.milkshakemod.Networking;

import com.somajoz.milkshakemod.MilkShake;
import com.somajoz.milkshakemod.Sounds.ModSounds;
import com.somajoz.milkshakemod.data_attachments.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TargetSyncPacket(boolean isTargeted) implements CustomPacketPayload {

    public static final Type<TargetSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MilkShake.MODID, "target_sync"));

    public static final StreamCodec<ByteBuf, TargetSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            TargetSyncPacket::isTargeted,
            TargetSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final TargetSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                boolean wasTargeted = player.getData(ModAttachments.IS_TARGETED);
                boolean isNowTargeted = payload.isTargeted;

                player.setData(ModAttachments.IS_TARGETED, isNowTargeted);

                // Если тревога НАЧАЛАСЬ (было false -> стало true)
                if (!wasTargeted && isNowTargeted) {
                    // Играем звук локально
                    // 1.0f = громкость, 1.0f = питч
                    player.playSound(ModSounds.AIR_RAID_SIREN.get(), 1.0f, 1.0f);
                }
            }
        });
    }
}
