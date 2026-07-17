package net.ochibo.twilightteleport.network;

//? if >=1.20.5
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ochibo.twilightteleport.server.PendingTeleportManager;

public final class ModNetworking {

    private ModNetworking() {
    }

    public static void register() {
        //? if >=1.20.5 {
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
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(
                TeleportClientStatePayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    TeleportClientStatePayload payload =
                            new TeleportClientStatePayload(buf);

                    server.execute(() ->
                            PendingTeleportManager.handleClientState(
                                    player,
                                    payload
                            )
                    );
                }
        );
        *///?}

    }
}
