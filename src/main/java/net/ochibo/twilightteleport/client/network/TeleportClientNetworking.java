package net.ochibo.twilightteleport.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <1.20.5
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;*/
import net.minecraft.client.Minecraft;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.sound.TeleportEffectSoundPlayer;
import net.ochibo.twilightteleport.network.TeleportClientState;
import net.ochibo.twilightteleport.network.TeleportClientStatePayload;
import net.ochibo.twilightteleport.network.TeleportEffectAction;
import net.ochibo.twilightteleport.network.TeleportEffectPayload;

import java.util.UUID;

public final class TeleportClientNetworking {

    private TeleportClientNetworking() {
    }

    public static void register() {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(
                TeleportEffectPayload.ID,
                (payload, context) ->
                        context.client().execute(
                                () -> handleEffect(
                                        context.client(),
                                        payload
                                )
                        )
        );
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(
                TeleportEffectPayload.ID,
                (client, handler, buf, responseSender) -> {
                    TeleportEffectPayload payload =
                            new TeleportEffectPayload(buf);

                    client.execute(() ->
                            handleEffect(client, payload)
                    );
                }
        );
        *///?}

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    TeleportEntityEffectManager.clearAll();
                    TeleportCameraController.cancel();
                }
        );
    }

    
    public static boolean isServerInstalled() {
        return ClientPlayNetworking.canSend(
                TeleportClientStatePayload.ID
        );
    }

    public static void sendState(
            UUID sessionId,
            TeleportClientState state
    ) {
        if (sessionId == null
                || !ClientPlayNetworking.canSend(
                TeleportClientStatePayload.ID
        )) {
            return;
        }

        //? if >=1.20.5 {
        ClientPlayNetworking.send(
                new TeleportClientStatePayload(
                        sessionId,
                        state
                )
        );
        //?} else {
        /*var buf = PacketByteBufs.create();
        new TeleportClientStatePayload(
                sessionId,
                state
        ).write(buf);
        ClientPlayNetworking.send(
                TeleportClientStatePayload.ID,
                buf
        );
        *///?}
    }

    private static void handleEffect(
            Minecraft client,
            TeleportEffectPayload payload
    ) {
        TeleportEffectSoundPlayer.play(
                client,
                payload
        );

        TeleportEntityEffectManager.apply(payload);

        if (client.player == null
                || !client.player.getUUID()
                .equals(payload.playerUuid())) {
            return;
        }

        if (payload.action()
                == TeleportEffectAction.START_DISSOLVE) {
            TeleportCameraController.startNetworked(
                    payload.sessionId(),
                    payload.delayTicks(),
                    payload.durationTicks()
            );
            return;
        }

        if (payload.action()
                == TeleportEffectAction.START_REBUILD) {
            TeleportCameraController.beginNetworkRebuild(
                    payload.sessionId(),
                    payload.delayTicks(),
                    payload.durationTicks()
            );
            return;
        }

        if (payload.action()
                == TeleportEffectAction.CLEAR) {
            TeleportCameraController.onNetworkClear(
                    payload.sessionId()
            );
        }
    }
}
