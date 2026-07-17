package net.ochibo.twilightteleport.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ochibo.twilightteleport.TwilightTeleport;

import java.util.UUID;

public record TeleportEffectPayload(
        UUID sessionId,
        UUID playerUuid,
        TeleportEffectAction action,
        int delayTicks,
        int durationTicks,
        int elapsedTicks
) implements CustomPacketPayload {

    public static final Type<TeleportEffectPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    TwilightTeleport.MOD_ID,
                    "teleport_effect"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportEffectPayload> CODEC =
            StreamCodec.ofMember(
                    TeleportEffectPayload::write,
                    TeleportEffectPayload::new
            );

    private TeleportEffectPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readUUID(),
                buf.readUUID(),
                readAction(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeUUID(playerUuid);
        buf.writeVarInt(action.ordinal());
        buf.writeVarInt(Math.max(0, delayTicks));
        buf.writeVarInt(Math.max(0, durationTicks));
        buf.writeVarInt(Math.max(0, elapsedTicks));
    }

    private static TeleportEffectAction readAction(RegistryFriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        TeleportEffectAction[] values = TeleportEffectAction.values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Unknown teleport effect action: " + ordinal
            );
        }

        return values[ordinal];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
