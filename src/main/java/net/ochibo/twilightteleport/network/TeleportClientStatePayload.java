package net.ochibo.twilightteleport.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ochibo.twilightteleport.TwilightTeleport;

import java.util.UUID;

public record TeleportClientStatePayload(
        UUID sessionId,
        TeleportClientState state
) implements CustomPacketPayload {

    public static final Type<TeleportClientStatePayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    TwilightTeleport.MOD_ID,
                    "teleport_client_state"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportClientStatePayload> CODEC =
            StreamCodec.ofMember(
                    TeleportClientStatePayload::write,
                    TeleportClientStatePayload::new
            );

    private TeleportClientStatePayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readUUID(),
                readState(buf)
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeVarInt(state.ordinal());
    }

    private static TeleportClientState readState(RegistryFriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        TeleportClientState[] values = TeleportClientState.values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Unknown teleport client state: " + ordinal
            );
        }

        return values[ordinal];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
