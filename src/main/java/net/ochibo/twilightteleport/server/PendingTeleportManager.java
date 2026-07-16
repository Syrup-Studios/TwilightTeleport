package net.ochibo.twilightteleport.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.ochibo.twilightteleport.TeleportTimingProfile;
import net.ochibo.twilightteleport.network.TeleportClientState;
import net.ochibo.twilightteleport.network.TeleportClientStatePayload;
import net.ochibo.twilightteleport.network.TeleportEffectAction;
import net.ochibo.twilightteleport.network.TeleportEffectPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PendingTeleportManager {

    private static final int RENDER_TIMEOUT_TICKS = 20 * 45;

    private static final Map<UUID, PendingTeleportSession> SESSIONS =
            new HashMap<>();

    private static final Set<UUID> INTERCEPTION_BYPASS =
            new HashSet<>();

    
    private static final Map<UUID, Integer>
            TELEPORT_TARGET_BYPASS_DEPTH =
            new HashMap<>();

    private static final double MIN_EXTERNAL_TELEPORT_DISTANCE_SQUARED =
            16.0D;

    private PendingTeleportManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(
                PendingTeleportManager::tick
        );

        EntityTrackingEvents.START_TRACKING.register(
                PendingTeleportManager::onStartTracking
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        cancel(server, handler.player.getUuid())
        );
    }

    
    public static boolean interceptExternalTeleport(
            ServerPlayerEntity player,
            net.minecraft.server.world.ServerWorld destinationWorld,
            double destinationX,
            double destinationY,
            double destinationZ,
            Runnable deferredTeleportAction
    ) {
        UUID playerUuid = player.getUuid();

        if (shouldBypassInterception(player)) {
            return false;
        }

        PendingTeleportSession existing =
                SESSIONS.get(playerUuid);

        if (existing != null) {
            return true;
        }

        if (!player.isAlive()
                || player.isSpectator()
                || deferredTeleportAction == null) {
            return false;
        }

        boolean dimensionChanged =
                player.getServerWorld()
                        != destinationWorld;

        double distanceSquared =
                player.squaredDistanceTo(
                        destinationX,
                        destinationY,
                        destinationZ
                );

        if (!dimensionChanged
                && distanceSquared
                < MIN_EXTERNAL_TELEPORT_DISTANCE_SQUARED) {
            return false;
        }

        if (!ServerPlayNetworking.canSend(
                player,
                TeleportEffectPayload.ID
        )) {
            return false;
        }

        PendingTeleportSession session =
                PendingTeleportSession.deferred(
                        UUID.randomUUID(),
                        playerUuid,
                        deferredTeleportAction,
                        TeleportTimingProfile.defaults()
                );

        SESSIONS.put(playerUuid, session);

        TeleportEffectBroadcaster.sendToCurrentTrackers(
                session,
                player,
                TeleportEffectAction.START_DISSOLVE,
                session.timings().moveToSideTicks(),
                session.timings().dissolveTicks(),
                0
        );

        return true;
    }

    
    public static void beginTeleportTargetTransition(
            ServerPlayerEntity player
    ) {
        TELEPORT_TARGET_BYPASS_DEPTH.merge(
                player.getUuid(),
                1,
                Integer::sum
        );
    }

    public static void endTeleportTargetTransition(
            ServerPlayerEntity player
    ) {
        UUID playerUuid = player.getUuid();

        TELEPORT_TARGET_BYPASS_DEPTH.computeIfPresent(
                playerUuid,
                (uuid, depth) ->
                        depth <= 1
                                ? null
                                : depth - 1
        );
    }

    private static boolean shouldBypassInterception(
            ServerPlayerEntity player
    ) {
        UUID playerUuid = player.getUuid();

        if (INTERCEPTION_BYPASS.contains(playerUuid)) {
            return true;
        }

        if (TELEPORT_TARGET_BYPASS_DEPTH
                .getOrDefault(
                        playerUuid,
                        0
                ) > 0) {
            return true;
        }

        return false;
    }

    private static void runWithoutInterception(
            ServerPlayerEntity player,
            Runnable action
    ) {
        UUID playerUuid = player.getUuid();

        INTERCEPTION_BYPASS.add(playerUuid);

        try {
            action.run();
        } finally {
            INTERCEPTION_BYPASS.remove(playerUuid);
        }
    }

    public static void handleClientState(
            ServerPlayerEntity player,
            TeleportClientStatePayload payload
    ) {
        PendingTeleportSession session =
                SESSIONS.get(player.getUuid());

        if (session == null
                || !session.sessionId().equals(payload.sessionId())) {
            return;
        }

        if (payload.state() == TeleportClientState.BLACK_READY
                && session.phase() == TeleportServerPhase.DISSOLVING) {
            if (session.deferredDriven()) {
                performDeferredTeleport(
                        player,
                        session
                );
            }

            return;
        }

        if (payload.state() == TeleportClientState.RENDER_READY
                && session.phase() == TeleportServerPhase.HIDDEN) {
            beginDestinationHold(player, session);
        }
    }

    private static void enterHiddenState(
            ServerPlayerEntity player,
            PendingTeleportSession session
    ) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return;
        }

        session.setPhase(TeleportServerPhase.HIDDEN);

        
        TeleportEffectBroadcaster.sendToAllObservers(
                server,
                session,
                TeleportEffectAction.HIDDEN,
                0,
                0,
                0
        );
    }

    private static void performDeferredTeleport(
            ServerPlayerEntity player,
            PendingTeleportSession session
    ) {
        Runnable action =
                session.deferredTeleportAction();

        if (action == null) {
            return;
        }

        enterHiddenState(player, session);

        player.setVelocity(Vec3d.ZERO);
        player.setSprinting(false);
        player.setSneaking(false);

        runWithoutInterception(
                player,
                action
        );

        TeleportEffectBroadcaster.sendToCurrentTrackers(
                session,
                player,
                TeleportEffectAction.HIDDEN,
                0,
                0,
                0
        );
    }


    private static void beginDestinationHold(
            ServerPlayerEntity player,
            PendingTeleportSession session
    ) {
        if (session.timings().destinationHoldTicks() <= 0) {
            beginRebuild(player, session);
            return;
        }

        session.setPhase(TeleportServerPhase.DESTINATION_HOLD);

        
        TeleportEffectBroadcaster.sendToCurrentTrackers(
                session,
                player,
                TeleportEffectAction.HIDDEN,
                0,
                0,
                0
        );
    }

    private static void beginRebuild(
            ServerPlayerEntity player,
            PendingTeleportSession session
    ) {
        session.setPhase(TeleportServerPhase.REBUILDING);

        TeleportEffectBroadcaster.sendToCurrentTrackers(
                session,
                player,
                TeleportEffectAction.START_REBUILD,
                session.timings().rebuildMeshDelayTicks(),
                session.timings().rebuildTicks(),
                0
        );
    }

    private static void tick(MinecraftServer server) {
        for (PendingTeleportSession session
                : new ArrayList<>(SESSIONS.values())) {

            ServerPlayerEntity player =
                    server.getPlayerManager()
                            .getPlayer(session.playerUuid());

            if (player == null) {
                clear(server, session);
                continue;
            }

            session.tick();

            player.setVelocity(Vec3d.ZERO);
            player.setSprinting(false);
            player.setSneaking(false);

            switch (session.phase()) {
                case DISSOLVING -> {
                    if (session.phaseTicks()
                            >= session.timings().dissolveTimeoutTicks()) {
                        clear(server, session);
                    }
                }

                case HIDDEN -> {
                    
                    if (session.phaseTicks() % 10 == 0) {
                        TeleportEffectBroadcaster.sendToCurrentTrackers(
                                session,
                                player,
                                TeleportEffectAction.HIDDEN,
                                0,
                                0,
                                0
                        );
                    }

                    
                    if (session.phaseTicks()
                            >= RENDER_TIMEOUT_TICKS) {
                        beginDestinationHold(player, session);
                    }
                }

                case DESTINATION_HOLD -> {
                    if (session.phaseTicks() % 10 == 0) {
                        TeleportEffectBroadcaster.sendToCurrentTrackers(
                                session,
                                player,
                                TeleportEffectAction.HIDDEN,
                                0,
                                0,
                                0
                        );
                    }

                    if (session.phaseTicks()
                            >= session.timings().destinationHoldTicks()) {
                        beginRebuild(player, session);
                    }
                }

                case REBUILDING -> {
                    if (session.phaseTicks()
                            >= session.timings().totalRebuildTicks()) {
                        clear(server, session);
                    }
                }
            }
        }
    }

    private static void onStartTracking(
            Entity trackedEntity,
            ServerPlayerEntity observer
    ) {
        if (!(trackedEntity
                instanceof ServerPlayerEntity target)) {
            return;
        }

        PendingTeleportSession session =
                SESSIONS.get(target.getUuid());

        if (session == null) {
            return;
        }

        switch (session.phase()) {
            case DISSOLVING ->
                    TeleportEffectBroadcaster.sendToObserver(
                            session,
                            observer,
                            TeleportEffectAction.START_DISSOLVE,
                            session.timings().moveToSideTicks(),
                            session.timings().dissolveTicks(),
                            session.phaseTicks()
                    );

            case HIDDEN, DESTINATION_HOLD ->
                    TeleportEffectBroadcaster.sendToObserver(
                            session,
                            observer,
                            TeleportEffectAction.HIDDEN,
                            0,
                            0,
                            0
                    );

            case REBUILDING ->
                    TeleportEffectBroadcaster.sendToObserver(
                            session,
                            observer,
                            TeleportEffectAction.START_REBUILD,
                            session.timings().rebuildMeshDelayTicks(),
                            session.timings().rebuildTicks(),
                            session.phaseTicks()
                    );
        }
    }

    private static void cancel(
            MinecraftServer server,
            UUID playerUuid
    ) {
        PendingTeleportSession session =
                SESSIONS.get(playerUuid);

        if (session != null) {
            clear(server, session);
        }
    }

    private static void clear(
            MinecraftServer server,
            PendingTeleportSession session
    ) {
        TeleportEffectBroadcaster.sendToAllObservers(
                server,
                session,
                TeleportEffectAction.CLEAR,
                0,
                0,
                0
        );

        SESSIONS.remove(
                session.playerUuid(),
                session
        );

        INTERCEPTION_BYPASS.remove(
                session.playerUuid()
        );

        TELEPORT_TARGET_BYPASS_DEPTH.remove(
                session.playerUuid()
        );
    }
}
