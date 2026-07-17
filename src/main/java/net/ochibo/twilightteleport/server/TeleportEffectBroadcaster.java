package net.ochibo.twilightteleport.server;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.ochibo.twilightteleport.network.TeleportEffectAction;
import net.ochibo.twilightteleport.network.TeleportEffectPayload;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class TeleportEffectBroadcaster {

    private TeleportEffectBroadcaster() {
    }

    static void sendToCurrentTrackers(
            PendingTeleportSession session,
            ServerPlayer target,
            TeleportEffectAction action,
            int delayTicks,
            int durationTicks,
            int elapsedTicks
    ) {
        Set<ServerPlayer> recipients =
                new HashSet<>(PlayerLookup.tracking(target));

        recipients.add(target);

        for (ServerPlayer recipient : recipients) {
            send(
                    session,
                    recipient,
                    action,
                    delayTicks,
                    durationTicks,
                    elapsedTicks
            );
        }
    }

    static void sendToObserver(
            PendingTeleportSession session,
            ServerPlayer recipient,
            TeleportEffectAction action,
            int delayTicks,
            int durationTicks,
            int elapsedTicks
    ) {
        send(
                session,
                recipient,
                action,
                delayTicks,
                durationTicks,
                elapsedTicks
        );
    }

    static void sendToAllObservers(
            MinecraftServer server,
            PendingTeleportSession session,
            TeleportEffectAction action,
            int delayTicks,
            int durationTicks,
            int elapsedTicks
    ) {
        for (UUID observerUuid : Set.copyOf(session.observers())) {
            ServerPlayer observer =
                    server.getPlayerList().getPlayer(observerUuid);

            if (observer != null) {
                send(
                        session,
                        observer,
                        action,
                        delayTicks,
                        durationTicks,
                        elapsedTicks
                );
            }
        }
    }

    private static void send(
            PendingTeleportSession session,
            ServerPlayer recipient,
            TeleportEffectAction action,
            int delayTicks,
            int durationTicks,
            int elapsedTicks
    ) {
        if (!ServerPlayNetworking.canSend(
                recipient,
                TeleportEffectPayload.ID
        )) {
            return;
        }

        session.observers().add(recipient.getUUID());

        ServerPlayNetworking.send(
                recipient,
                new TeleportEffectPayload(
                        session.sessionId(),
                        session.playerUuid(),
                        action,
                        delayTicks,
                        durationTicks,
                        elapsedTicks
                )
        );
    }
}
