package net.ochibo.twilightteleport.network;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.ochibo.twilightteleport.TwilightTeleport;

import java.util.UUID;

public record TeleportClientStatePayload(
        UUID sessionId,
        TeleportClientState state
//? if >=1.20.5
) implements CustomPacketPayload {
//? if <1.20.5
/*) {*/

    //? if >=1.20.5 {
    public static final Type<TeleportClientStatePayload> ID =
            new Type<>(TwilightTeleport.id("teleport_client_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportClientStatePayload> CODEC =
            StreamCodec.ofMember(
                    TeleportClientStatePayload::write,
                    TeleportClientStatePayload::new
            );
    //?} else {
    /*public static final net.minecraft.resources.ResourceLocation ID =
            TwilightTeleport.id("teleport_client_state");
    *///?}

    public TeleportClientStatePayload(FriendlyByteBuf buf) {
        this(
                buf.readUUID(),
                readState(buf)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeVarInt(state.ordinal());
    }

    private static TeleportClientState readState(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        TeleportClientState[] values = TeleportClientState.values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Unknown teleport client state: " + ordinal
            );
        }

        return values[ordinal];
    }

    //? if >=1.20.5 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
    //?}
}
