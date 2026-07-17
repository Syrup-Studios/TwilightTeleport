package net.ochibo.twilightteleport.client.effect;

import java.util.UUID;

final class TeleportEntityEffect {
    private final UUID sessionId;
    private final TeleportEntityEffectPhase phase;
    private final int delayTicks;
    private final int durationTicks;
    private int elapsedTicks;

    TeleportEntityEffect(
            UUID sessionId,
            TeleportEntityEffectPhase phase,
            int delayTicks,
            int durationTicks,
            int elapsedTicks
    ) {
        this.sessionId = sessionId;
        this.phase = phase;
        this.delayTicks = Math.max(0, delayTicks);
        this.durationTicks = Math.max(0, durationTicks);
        this.elapsedTicks = Math.max(0, elapsedTicks);
    }

    UUID sessionId() {
        return sessionId;
    }

    TeleportEntityEffectPhase phase() {
        return phase;
    }

    int delayTicks() {
        return delayTicks;
    }

    int durationTicks() {
        return durationTicks;
    }

    int elapsedTicks() {
        return elapsedTicks;
    }

    void tick() {
        elapsedTicks++;
    }

    
    float phaseProgress(float tickDelta) {
        if (durationTicks <= 0) {
            return 1.0F;
        }

        float progress =
                (elapsedTicks + tickDelta - delayTicks)
                        / durationTicks;

        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    
    float elapsedProgress(float tickDelta) {
        if (durationTicks <= 0) {
            return 1.0F;
        }

        float progress =
                (elapsedTicks + tickDelta)
                        / durationTicks;

        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    boolean delayFinished() {
        return delayFinished(0.0F);
    }

    boolean delayFinished(float tickDelta) {
        return elapsedTicks + tickDelta >= delayTicks;
    }
}
