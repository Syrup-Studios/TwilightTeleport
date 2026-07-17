package net.ochibo.twilightteleport.network;

import net.minecraft.network.FriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.ochibo.twilightteleport.TwilightTeleport;

import java.util.UUID;

public record TeleportEffectPayload(
        UUID sessionId,
        UUID playerUuid,
        TeleportEffectAction action,
        int delayTicks,
        int durationTicks,
        int elapsedTicks
//? if >=1.20.5
) implements CustomPacketPayload {
//? if <1.20.5
/*) {*/

    //? if >=1.20.5 {
    public static final Type<TeleportEffectPayload> ID =
            new Type<>(TwilightTeleport.id("teleport_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportEffectPayload> CODEC =
            StreamCodec.ofMember(
                    TeleportEffectPayload::write,
                    TeleportEffectPayload::new
            );
    //?} else {
    /*public static final net.minecraft.resources.ResourceLocation ID =
            TwilightTeleport.id("teleport_effect");
    *///?}

    public TeleportEffectPayload(FriendlyByteBuf buf) {
        this(
                buf.readUUID(),
                buf.readUUID(),
                readAction(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeUUID(playerUuid);
        buf.writeVarInt(action.ordinal());
        buf.writeVarInt(Math.max(0, delayTicks));
        buf.writeVarInt(Math.max(0, durationTicks));
        buf.writeVarInt(Math.max(0, elapsedTicks));
    }

    private static TeleportEffectAction readAction(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        TeleportEffectAction[] values = TeleportEffectAction.values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    "Unknown teleport effect action: " + ordinal
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
