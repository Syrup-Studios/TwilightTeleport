package net.ochibo.twilightteleport.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ochibo.twilightteleport.server.PendingTeleportManager;

public final class ModNetworking {

    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                TeleportEffectPayload.ID,
                TeleportEffectPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                TeleportClientStatePayload.ID,
                TeleportClientStatePayload.CODEC
        );


        ServerPlayNetworking.registerGlobalReceiver(
                TeleportClientStatePayload.ID,
                (payload, context) ->
                        PendingTeleportManager.handleClientState(
                                context.player(),
                                payload
                        )
        );

    }
}
