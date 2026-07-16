package net.ochibo.twilightteleport.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.ochibo.twilightteleport.TwilightTeleport;

import java.util.UUID;

public record TeleportClientStatePayload(
        UUID sessionId,
        TeleportClientState state
) implements CustomPayload {

    public static final Id<TeleportClientStatePayload> ID =
            new Id<>(Identifier.of(
                    TwilightTeleport.MOD_ID,
                    "teleport_client_state"
            ));

    public static final PacketCodec<RegistryByteBuf, TeleportClientStatePayload> CODEC =
            PacketCodec.of(
                    TeleportClientStatePayload::write,
                    TeleportClientStatePayload::new
            );

    private TeleportClientStatePayload(RegistryByteBuf buf) {
        this(
                buf.readUuid(),
                readState(buf)
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(sessionId);
        buf.writeVarInt(state.ordinal());
    }

    private static TeleportClientState readState(RegistryByteBuf buf) {
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
