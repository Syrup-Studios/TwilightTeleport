package net.ochibo.twilightteleport.client.effect;

import net.minecraft.client.MinecraftClient;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.client.render.TeleportDissolveRenderLayer;
import net.ochibo.twilightteleport.client.render.TeleportDissolveRenderState;
import net.ochibo.twilightteleport.client.render.TeleportRenderedHeightManager;
import net.ochibo.twilightteleport.network.TeleportEffectAction;
import net.ochibo.twilightteleport.network.TeleportEffectPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeleportEntityEffectManager {

    private static final Map<UUID, TeleportEntityEffect> EFFECTS =
            new HashMap<>();

    private TeleportEntityEffectManager() {
    }

    public static void apply(TeleportEffectPayload payload) {
        UUID playerUuid = payload.playerUuid();

        switch (payload.action()) {
            case START_DISSOLVE -> {
                
                TeleportRenderedHeightManager
                        .beginEffectCapture(
                                playerUuid,
                                payload.sessionId()
                        );

                EFFECTS.put(
                        playerUuid,
                        new TeleportEntityEffect(
                                payload.sessionId(),
                                TeleportEntityEffectPhase.DISSOLVING,
                                payload.delayTicks(),
                                payload.durationTicks(),
                                payload.elapsedTicks()
                        )
                );
            }

            case HIDDEN -> replaceIfCurrentOrAbsent(
                    playerUuid,
                    payload.sessionId(),
                    new TeleportEntityEffect(
                            payload.sessionId(),
                            TeleportEntityEffectPhase.HIDDEN,
                            0,
                            0,
                            0
                    )
            );

            case START_REBUILD -> replaceIfCurrentOrAbsent(
                    playerUuid,
                    payload.sessionId(),
                    new TeleportEntityEffect(
                            payload.sessionId(),
                            TeleportEntityEffectPhase.REBUILDING,
                            payload.delayTicks(),
                            payload.durationTicks(),
                            payload.elapsedTicks()
                    )
            );

            case CLEAR -> clear(
                    playerUuid,
                    payload.sessionId()
            );
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            return;
        }

        for (TeleportEntityEffect effect : EFFECTS.values()) {
            effect.tick();
        }
    }

    public static boolean shouldRenderDissolve(
            UUID playerUuid
    ) {
        TeleportEntityEffect effect =
                EFFECTS.get(playerUuid);

        
        if (effect != null) {
            if (effect.phase()
                    == TeleportEntityEffectPhase.HIDDEN
                    || effect.phase()
                    == TeleportEntityEffectPhase.REBUILDING) {
                return true;
            }
        }

        if (isLocalControllerTarget(playerUuid)) {
            return TeleportCameraController
                    .shouldRenderDissolve();
        }

        if (effect == null) {
            return false;
        }

        return effect.phase()
                == TeleportEntityEffectPhase.DISSOLVING
                && effect.delayFinished();
    }


    public static boolean isDissolving(UUID playerUuid) {
        if (isLocalControllerTarget(playerUuid)) {
            return TeleportCameraController.isDissolvingOut();
        }

        TeleportEntityEffect effect = EFFECTS.get(playerUuid);

        return effect != null
                && effect.phase()
                == TeleportEntityEffectPhase.DISSOLVING
                && effect.delayFinished()
                && effect.phaseProgress(0.0F) < 1.0F;
    }

    public static boolean isRebuilding(
            UUID playerUuid
    ) {
        TeleportEntityEffect effect =
                EFFECTS.get(playerUuid);

        
        if (effect != null
                && effect.phase()
                == TeleportEntityEffectPhase.REBUILDING) {
            return true;
        }

        return isLocalControllerTarget(playerUuid)
                && TeleportCameraController.isRebuilding();
    }

    public static boolean hasRebuildMeshStarted(
            UUID playerUuid,
            float tickDelta
    ) {
        TeleportEntityEffect effect =
                EFFECTS.get(playerUuid);

        if (effect != null
                && effect.phase()
                == TeleportEntityEffectPhase.REBUILDING) {
            return effect.delayFinished(tickDelta);
        }

        
        if (isLocalControllerTarget(playerUuid)) {
            return TeleportCameraController
                    .getDissolveProgress(tickDelta) < 1.0F;
        }

        return false;
    }

    public static float getDissolveProgress(
            UUID playerUuid,
            float tickDelta
    ) {
        TeleportEntityEffect effect =
                EFFECTS.get(playerUuid);

        
        if (effect != null
                && effect.phase()
                == TeleportEntityEffectPhase.REBUILDING) {
            return 1.0F
                    - smootherStep(
                    effect.phaseProgress(tickDelta)
            );
        }

        if (isLocalControllerTarget(playerUuid)) {
            return TeleportCameraController
                    .getDissolveProgress(tickDelta);
        }

        if (effect == null) {
            return 0.0F;
        }

        return switch (effect.phase()) {
            case DISSOLVING ->
                    smootherStep(
                            effect.phaseProgress(tickDelta)
                    );

            case HIDDEN -> 1.0F;

            case REBUILDING ->
                    1.0F
                            - smootherStep(
                            effect.phaseProgress(tickDelta)
                    );
        };
    }

    public static float getRebuildingProgress(
            UUID playerUuid,
            float tickDelta
    ) {
        TeleportEntityEffect effect =
                EFFECTS.get(playerUuid);

        
        if (effect != null
                && effect.phase()
                == TeleportEntityEffectPhase.REBUILDING) {
            return smootherStep(
                    effect.elapsedProgress(tickDelta)
            );
        }

        if (isLocalControllerTarget(playerUuid)) {
            return TeleportCameraController
                    .getRebuildingProgress(tickDelta);
        }

        return 0.0F;
    }

    public static void clearAll() {
        for (UUID playerUuid : EFFECTS.keySet()) {
            TeleportDissolveRenderLayer.clear(playerUuid);
            TeleportDissolveRenderState.clear(playerUuid);
        }

        EFFECTS.clear();
        TeleportRenderedHeightManager.clearAll();
    }

    private static void replaceIfCurrentOrAbsent(
            UUID playerUuid,
            UUID sessionId,
            TeleportEntityEffect replacement
    ) {
        TeleportEntityEffect current = EFFECTS.get(playerUuid);

        if (current == null
                || current.sessionId().equals(sessionId)) {
            EFFECTS.put(playerUuid, replacement);
        }
    }

    private static void clear(
            UUID playerUuid,
            UUID sessionId
    ) {
        TeleportEntityEffect current = EFFECTS.get(playerUuid);

        if (current == null
                || !current.sessionId().equals(sessionId)) {
            return;
        }

        EFFECTS.remove(playerUuid);
        TeleportDissolveRenderLayer.clear(playerUuid);
        TeleportDissolveRenderState.clear(playerUuid);
        TeleportRenderedHeightManager.clearEffect(playerUuid);
    }

    private static boolean isLocalControllerTarget(UUID playerUuid) {
        MinecraftClient client = MinecraftClient.getInstance();

        return client.player != null
                && client.player.getUuid().equals(playerUuid)
                && TeleportCameraController.isRunning();
    }

    private static float smootherStep(float value) {
        float x = Math.max(0.0F, Math.min(1.0F, value));

        return x * x * x
                * (x * (x * 6.0F - 15.0F) + 10.0F);
    }
}
