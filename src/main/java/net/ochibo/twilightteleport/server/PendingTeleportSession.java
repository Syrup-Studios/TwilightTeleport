package net.ochibo.twilightteleport.server;

import net.ochibo.twilightteleport.TeleportTimingProfile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class PendingTeleportSession {
    private final UUID sessionId;
    private final UUID playerUuid;
    private final Runnable deferredTeleportAction;
    private final TeleportTimingProfile timings;
    private final Set<UUID> observers = new HashSet<>();

    private TeleportServerPhase phase = TeleportServerPhase.DISSOLVING;
    private int phaseTicks;

    
    static PendingTeleportSession deferred(
            UUID sessionId,
            UUID playerUuid,
            Runnable deferredTeleportAction,
            TeleportTimingProfile timings
    ) {
        return new PendingTeleportSession(
                sessionId,
                playerUuid,
                deferredTeleportAction,
                timings
        );
    }

    private PendingTeleportSession(
            UUID sessionId,
            UUID playerUuid,
            Runnable deferredTeleportAction,
            TeleportTimingProfile timings
    ) {
        this.sessionId = sessionId;
        this.playerUuid = playerUuid;
        this.deferredTeleportAction = deferredTeleportAction;
        this.timings = timings == null
                ? TeleportTimingProfile.defaults()
                : timings;
    }

    UUID sessionId() {
        return sessionId;
    }

    UUID playerUuid() {
        return playerUuid;
    }

    boolean deferredDriven() {
        return deferredTeleportAction != null;
    }

    Runnable deferredTeleportAction() {
        return deferredTeleportAction;
    }

    TeleportTimingProfile timings() {
        return timings;
    }

    Set<UUID> observers() {
        return observers;
    }

    TeleportServerPhase phase() {
        return phase;
    }

    int phaseTicks() {
        return phaseTicks;
    }

    void tick() {
        phaseTicks++;
    }

    void setPhase(TeleportServerPhase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }
}
