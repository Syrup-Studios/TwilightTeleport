package net.ochibo.twilightteleport;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.player.LocalPlayer;
//? if >=1.20.5 {
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//?} else {
/*import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
//? if >=1.20.5 {
import net.minecraft.world.level.chunk.status.ChunkStatus;
//?} else {
/*import net.minecraft.world.level.chunk.ChunkStatus;
*///?}
import net.minecraft.world.phys.Vec3;
import net.ochibo.twilightteleport.client.network.TeleportClientNetworking;
import net.ochibo.twilightteleport.network.TeleportClientState;

import java.util.UUID;

public final class TeleportCameraController {
    

    private static final int ARRIVAL_CAPTURE_DELAY_TICKS = 2;
    private static final int TELEPORT_TIMEOUT_TICKS = 120;

    
    private static final int MIN_RENDER_WAIT_TICKS = 10;

    
    private static final int REQUIRED_RENDER_READY_STABLE_TICKS = 3;
    private static final float REQUIRED_RENDER_CHUNK_RATIO = 0.50F;

    
    private static final int EMPTY_TERRAIN_SCAN_RADIUS_DIVISOR = 2;
    private static final int MIN_EMPTY_TERRAIN_SCAN_RADIUS = 2;

    
    private static final int REQUIRED_EMPTY_TERRAIN_STABLE_TICKS = 3;

    
    private static final int MAX_RENDER_WAIT_TICKS = 20 * 20;

    
    private static final int LOADING_STATUS_DELAY_TICKS = 40;
    private static final float LOOK_UP_START_PROGRESS = 0.2F;
    
    private static final float ORBIT_ACCELERATION_END_PROGRESS = 0.42F;

    
    private static final float LOOK_UP_ACCELERATION_END_PROGRESS = 0.38F;

    
    private static final float ORBIT_ANGLE_DEGREES = -80.0F;

    
    private static final float ORBIT_END_PITCH = -80.0F;

    
    private static final float FADE_START_PROGRESS = 0.40F;

    private static final double SIDE_DISTANCE = 4.2D;
    private static final double SIDE_FORWARD_OFFSET = -0.55D;
    private static final double SIDE_HEIGHT_OFFSET = 0.30D;



    private static Phase phase = Phase.IDLE;
    private static int phaseTicks;
    private static TeleportTimingProfile activeTimings =
            TeleportTimingProfile.defaults();
    private static Vec3 departureFocus;
    private static Vec3 arrivalFocus;

    private static boolean arrivalCaptureRequested;
    private static int arrivalCaptureDelay;

    

    private static int renderReadyStableTicks;
    private static int emptyRenderQueueStableTicks;
    private static boolean terrainUpdateScheduled;

    private static int blackoutWaitTicks;
    private static float lastLoadedChunkRatio;
    private static LoadingStatus loadingStatus =
            LoadingStatus.NONE;

    private static CameraType previousCameraType;

    private static CameraPose departureFirstPerson;
    private static CameraPose departureSide;

    private static CameraPose arrivalFirstPerson;
    private static CameraPose arrivalSide;

    private static ResourceKey<Level> departureDimension;
    private static Vec3 departureFeet;

    private static Runnable actionWhenFullyBlack;

    private static UUID activeNetworkSessionId;
    private static boolean renderReadySent;

    private TeleportCameraController() {
    }


    
    public static void startNetworked(
            UUID sessionId,
            int moveToSideTicks,
            int dissolveTicks
    ) {
        if (sessionId == null || isRunning()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return;
        }

        activeNetworkSessionId = sessionId;
        renderReadySent = false;

        TeleportTimingProfile timings =
                TeleportTimingProfile
                        .defaults()
                        .withDeparture(
                                moveToSideTicks,
                                dissolveTicks
                        );

        start(() -> TeleportClientNetworking.sendState(
                sessionId,
                TeleportClientState.BLACK_READY
        ), timings);

        
        if (!isRunning()) {
            activeNetworkSessionId = null;
        }
    }

    
    public static void beginNetworkRebuild(
            UUID sessionId,
            int rebuildMeshDelayTicks,
            int rebuildTicks
    ) {
        if (sessionId == null
                || !sessionId.equals(activeNetworkSessionId)) {
            return;
        }

        if (phase == Phase.WAITING_FOR_REBUILD_SIGNAL
                || phase == Phase.WAITING_FOR_RENDER) {
            activeTimings = activeTimings.withRebuild(
                    rebuildMeshDelayTicks,
                    rebuildTicks
            );
            
            setPhase(Phase.FADE_IN_AND_ORBIT_BACK);
        }
    }

    public static void onNetworkClear(UUID sessionId) {
        if (sessionId == null
                || !sessionId.equals(activeNetworkSessionId)) {
            return;
        }

        
        if (phase == Phase.WAITING_FOR_TELEPORT
                || phase == Phase.WAITING_FOR_RENDER
                || phase == Phase.WAITING_FOR_REBUILD_SIGNAL) {
            cancel();
        }
    }

    
    public static void start(Runnable actionWhenBlack) {
        start(
                actionWhenBlack,
                TeleportTimingProfile.defaults()
        );
    }

    private static void start(
            Runnable actionWhenBlack,
            TeleportTimingProfile timings
    ) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (isRunning() || player == null || client.level == null) {
            return;
        }

        activeTimings = timings == null
                ? TeleportTimingProfile.defaults()
                : timings;

        previousCameraType = client.options.getCameraType();

        departureFirstPerson = createFirstPersonPose(player);
        departureFocus = createFocus(player);

        departureSide = createSidePose(
                player,
                departureFirstPerson.yaw(),
                departureFocus
        );

        departureDimension = client.level.dimension();
        departureFeet = player.position();

        arrivalFirstPerson = null;
        arrivalSide = null;
        arrivalFocus = null;

        arrivalCaptureRequested = false;
        arrivalCaptureDelay = 0;

        renderReadyStableTicks = 0;
        emptyRenderQueueStableTicks = 0;
        terrainUpdateScheduled = false;
        renderReadySent = false;

        blackoutWaitTicks = 0;
        lastLoadedChunkRatio = 0.0F;
        loadingStatus = LoadingStatus.NONE;

        actionWhenFullyBlack = actionWhenBlack == null
                ? () -> {
        }
                : actionWhenBlack;

        
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        setPhase(Phase.MOVE_TO_SIDE);
    }

    
    public static void markArrived() {
        if (phase != Phase.WAITING_FOR_TELEPORT) {
            return;
        }

        
        arrivalCaptureRequested = true;
        arrivalCaptureDelay = 0;
    }

    public static void tick(Minecraft client) {
        if (!isRunning()) {
            return;
        }

        LocalPlayer player = client.player;

        if (player == null || client.level == null) {
            cancel();
            return;
        }


        client.options.setCameraType(
                CameraType.THIRD_PERSON_BACK
        );
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.setDeltaMovement(Vec3.ZERO);

        phaseTicks++;

        if (isBlackoutWaitPhase()) {
            blackoutWaitTicks++;
        }

        switch (phase) {
            case MOVE_TO_SIDE -> {
                if (phaseTicks >= activeTimings.moveToSideTicks()) {
                    setPhase(Phase.ORBIT_AND_FADE_OUT);
                }
            }

            case ORBIT_AND_FADE_OUT -> {
                if (phaseTicks >= activeTimings.dissolveTicks()) {
                    
                    setPhase(Phase.WAITING_FOR_TELEPORT);
                    runTeleportAction();
                }
            }

            case WAITING_FOR_TELEPORT -> {
                loadingStatus =
                        LoadingStatus.WAITING_FOR_TELEPORT;

                if (!arrivalCaptureRequested
                        && hasAutomaticallyDetectedArrival(client, player)) {
                    arrivalCaptureRequested = true;
                    arrivalCaptureDelay = 0;
                }

                if (arrivalCaptureRequested) {
                    arrivalCaptureDelay++;

                    if (arrivalCaptureDelay >= ARRIVAL_CAPTURE_DELAY_TICKS) {
                        captureArrival(client);
                    }
                } else if (activeNetworkSessionId == null
                        && phaseTicks >= TELEPORT_TIMEOUT_TICKS) {
                    
                    arrivalFirstPerson = createFirstPersonPose(player);
                    arrivalFocus = createFocus(player);

                    arrivalSide = createSidePose(
                            player,
                            arrivalFirstPerson.yaw(),
                            arrivalFocus
                    );

                    setPhase(Phase.FADE_IN_AND_ORBIT_BACK);
                } else if (activeNetworkSessionId != null
                        && phaseTicks >= TELEPORT_TIMEOUT_TICKS * 5) {
                    cancel();
                }
            }

            case WAITING_FOR_RENDER -> {
                if (loadingStatus
                        == LoadingStatus.NONE) {
                    loadingStatus =
                            LoadingStatus.LOADING_CHUNKS;
                }

                
                if (!terrainUpdateScheduled) {
                    client.levelRenderer.needsUpdate();
                    terrainUpdateScheduled = true;
                }

                if (phaseTicks < MIN_RENDER_WAIT_TICKS) {
                    break;
                }

                if (isDestinationLoadAndRenderComplete(client, player)) {
                    renderReadyStableTicks++;
                } else {
                    renderReadyStableTicks = 0;
                }

                boolean renderWaitTimedOut =
                        phaseTicks >= MAX_RENDER_WAIT_TICKS;

                if (renderReadyStableTicks
                        >= REQUIRED_RENDER_READY_STABLE_TICKS
                        || renderWaitTimedOut) {
                    loadingStatus = LoadingStatus.FINALIZING;
                    finishRenderWait();
                }
            }

            case WAITING_FOR_REBUILD_SIGNAL -> {
                loadingStatus =
                        LoadingStatus.WAITING_FOR_REBUILD_SIGNAL;
            }

            case DESTINATION_HOLD -> {
                if (phaseTicks >= activeTimings.destinationHoldTicks()) {
                    setPhase(Phase.FADE_IN_AND_ORBIT_BACK);
                }
            }

            case FADE_IN_AND_ORBIT_BACK -> {
                if (phaseTicks
                        >= activeTimings.totalRebuildTicks()) {
                    setPhase(Phase.RETURN_TO_FIRST_PERSON);
                }
            }

            case RETURN_TO_FIRST_PERSON -> {
                if (phaseTicks >= activeTimings.returnToFirstPersonTicks()) {
                    finish(client);
                }
            }

            case IDLE -> {
            }
        }
    }

    
    public static CameraFrame getCameraFrame(float tickDelta) {
        if (!isRunning()) {
            return null;
        }

        return switch (phase) {
            case MOVE_TO_SIDE -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.moveToSideTicks()
                );

                
                float eased = easeOutCubic(progress);

                yield departureFirstPerson
                        .interpolate(departureSide, eased)
                        .toFrame();
            }

            case ORBIT_AND_FADE_OUT -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.dissolveTicks()
                );

                yield createOrbitFrame(
                        departureFocus,
                        departureSide,
                        progress,
                        false
                );
            }

            
            case WAITING_FOR_TELEPORT,
                 WAITING_FOR_RENDER,
                 WAITING_FOR_REBUILD_SIGNAL -> null;

            case DESTINATION_HOLD -> {
                if (arrivalFocus == null || arrivalSide == null) {
                    yield null;
                }

                yield createOrbitFrame(
                        arrivalFocus,
                        arrivalSide,
                        1.0F,
                        false
                );
            }

            case FADE_IN_AND_ORBIT_BACK -> {
                if (arrivalFocus == null || arrivalSide == null) {
                    yield null;
                }

                float progress =
                        getRebuildVisualProgress(tickDelta);

                
                yield createOrbitFrame(
                        arrivalFocus,
                        arrivalSide,
                        progress,
                        true
                );
            }

            case RETURN_TO_FIRST_PERSON -> {
                if (arrivalSide == null || arrivalFirstPerson == null) {
                    yield null;
                }

                float progress = progress(
                        tickDelta,
                        activeTimings.returnToFirstPersonTicks()
                );

                float eased = smootherStep(progress);

                yield arrivalSide
                        .interpolate(arrivalFirstPerson, eased)
                        .toFrame();
            }

            case IDLE -> null;
        };
    }

    private static CameraFrame createOrbitFrame(
            Vec3 focus,
            CameraPose sidePose,
            float phaseProgress,
            boolean reverse
    ) {
        float rawProgress = Mth.clamp(
                phaseProgress,
                0.0F,
                1.0F
        );

        
        float easedOrbitProgress =
                accelerationDeceleration(
                        rawProgress,
                        ORBIT_ACCELERATION_END_PROGRESS
                );

        
        float orbitProgress = reverse
                ? 1.0F - easedOrbitProgress
                : easedOrbitProgress;

        float lookUpProgress;

        if (reverse) {
            
            float lookDownRawProgress =
                    Mth.clamp(
                            rawProgress
                                    / (1.0F - LOOK_UP_START_PROGRESS),
                            0.0F,
                            1.0F
                    );

            lookUpProgress =
                    1.0F
                            - accelerationDeceleration(
                            lookDownRawProgress,
                            LOOK_UP_ACCELERATION_END_PROGRESS
                    );
        } else {
            
            float lookUpRawProgress =
                    remapFromStart(
                            rawProgress,
                            LOOK_UP_START_PROGRESS
                    );

            lookUpProgress =
                    accelerationDeceleration(
                            lookUpRawProgress,
                            LOOK_UP_ACCELERATION_END_PROGRESS
                    );
        }

        Vec3 relativePosition =
                sidePose.position().subtract(focus);

        double angleRadians =
                Math.toRadians(
                        ORBIT_ANGLE_DEGREES
                                * orbitProgress
                );

        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        Vec3 rotatedOffset = new Vec3(
                relativePosition.x * cos
                        - relativePosition.z * sin,

                relativePosition.y,

                relativePosition.x * sin
                        + relativePosition.z * cos
        );

        Vec3 cameraPosition =
                focus.add(rotatedOffset);

        CameraPose facingPlayer =
                lookAt(cameraPosition, focus);

        float pitch = Mth.lerp(
                lookUpProgress,
                sidePose.pitch(),
                ORBIT_END_PITCH
        );

        return new CameraFrame(
                cameraPosition,
                facingPlayer.yaw(),
                pitch
        );
    }

    
    private static float accelerationDeceleration(
            float value,
            float accelerationEndProgress
    ) {
        float progress = Mth.clamp(
                value,
                0.0F,
                1.0F
        );

        float accelerationEnd =
                Mth.clamp(
                        accelerationEndProgress,
                        0.05F,
                        0.95F
                );

        if (progress <= accelerationEnd) {
            
            return progress
                    * progress
                    / accelerationEnd;
        }

        
        float decelerationDuration =
                1.0F - accelerationEnd;

        float decelerationTime =
                progress - accelerationEnd;

        return accelerationEnd
                + 2.0F * decelerationTime
                - decelerationTime
                * decelerationTime
                / decelerationDuration;
    }

    private static float remapFromStart(
            float progress,
            float startProgress
    ) {
        float start = Mth.clamp(
                startProgress,
                0.0F,
                0.999F
        );

        return Mth.clamp(
                (progress - start)
                        / (1.0F - start),
                0.0F,
                1.0F
        );
    }


    private static Vec3 createFocus(
            LocalPlayer player
    ) {
        return player.position().add(
                0.0D,
                player.getBbHeight() * 0.55D,
                0.0D
        );
    }

    
    public static float getFadeAlpha(float tickDelta) {
        return switch (phase) {
            case ORBIT_AND_FADE_OUT -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.dissolveTicks()
                );

                yield getOrbitFadeAlpha(progress);
            }

            case WAITING_FOR_TELEPORT,
                 WAITING_FOR_RENDER,
                 WAITING_FOR_REBUILD_SIGNAL,
                 DESTINATION_HOLD -> 1.0F;

            case FADE_IN_AND_ORBIT_BACK -> {
                float progress =
                        getRebuildVisualProgress(tickDelta);

                
                yield getOrbitFadeAlpha(1.0F - progress);
            }

            default -> 0.0F;
        };
    }

    private static float getOrbitFadeAlpha(
            float orbitProgress
    ) {
        float fadeProgress = Mth.clamp(
                (orbitProgress - FADE_START_PROGRESS)
                        / (1.0F - FADE_START_PROGRESS),
                0.0F,
                1.0F
        );

        return smootherStep(fadeProgress);
    }

    
    public static float getLetterboxProgress(float tickDelta) {
        return switch (phase) {
            case MOVE_TO_SIDE -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.moveToSideTicks()
                );

                yield easeOutCubic(progress);
            }

            case ORBIT_AND_FADE_OUT,
                 WAITING_FOR_TELEPORT,
                 WAITING_FOR_RENDER,
                 WAITING_FOR_REBUILD_SIGNAL,
                 DESTINATION_HOLD,
                 FADE_IN_AND_ORBIT_BACK -> 1.0F;

            case RETURN_TO_FIRST_PERSON -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.returnToFirstPersonTicks()
                );

                yield 1.0F - smootherStep(progress);
            }

            case IDLE -> 0.0F;
        };
    }

    public static boolean shouldShowLoadingStatus() {
        return isRunning()
                && isBlackoutWaitPhase()
                && blackoutWaitTicks
                >= LOADING_STATUS_DELAY_TICKS;
    }

    public static Component getLoadingStatusText() {
        return switch (loadingStatus) {
            case WAITING_FOR_WORLD ->
                    Component.translatable(
                            "hud.twilightteleport.loading.waiting_world"
                    );

            case WAITING_FOR_TELEPORT ->
                    Component.translatable(
                            "hud.twilightteleport.loading.waiting_teleport"
                    );

            case LOADING_CHUNKS -> {
                final int percentage =
                        Math.round(
                                Mth.clamp(
                                        lastLoadedChunkRatio,
                                        0.0F,
                                        1.0F
                                ) * 100.0F
                        );

                final int requiredPercentage =
                        Math.round(
                                REQUIRED_RENDER_CHUNK_RATIO
                                        * 100.0F
                        );

                final int finalPercentage = Math.round(((float) percentage / requiredPercentage) * 100);

                yield Component.translatable(
                        "hud.twilightteleport.loading.chunks",
                        finalPercentage + "%"
                );
            }

            case PREPARING_RENDER ->
                    Component.translatable(
                            "hud.twilightteleport.loading.preparing_render"
                    );

            case BUILDING_TERRAIN ->
                    Component.translatable(
                            "hud.twilightteleport.loading.building_terrain"
                    );

            case BUILDING_CHUNK_MESHES ->
                    Component.translatable(
                            "hud.twilightteleport.loading.building_chunk_meshes"
                    );

            case UPLOADING_TO_GPU ->
                    Component.translatable(
                            "hud.twilightteleport.loading.uploading_gpu"
                    );

            case WAITING_FOR_REBUILD_SIGNAL ->
                    Component.translatable(
                            "hud.twilightteleport.loading.waiting_rebuild"
                    );

            case FINALIZING ->
                    Component.translatable(
                            "hud.twilightteleport.loading.finalizing"
                    );

            case NONE -> Component.empty();
        };
    }

    private static boolean isBlackoutWaitPhase() {
        return phase == Phase.WAITING_FOR_TELEPORT
                || phase == Phase.WAITING_FOR_RENDER
                || phase
                == Phase.WAITING_FOR_REBUILD_SIGNAL;
    }

    public static boolean isRunning() {
        return phase != Phase.IDLE;
    }

    
    public static boolean shouldBlockInput() {
        return isRunning();
    }

    private static float smootherStep(float value) {
        float x = Mth.clamp(
                value,
                0.0F,
                1.0F
        );

        
        return x * x * x
                * (x * (x * 6.0F - 15.0F) + 10.0F);
    }

    public static void cancel() {
        Minecraft client = Minecraft.getInstance();

        if (previousCameraType != null) {
            client.options.setCameraType(previousCameraType);
        }

        clear();
    }

    private static void runTeleportAction() {
        Runnable action = actionWhenFullyBlack;
        actionWhenFullyBlack = null;

        if (action != null) {
            action.run();
        }
    }

    private static boolean hasAutomaticallyDetectedArrival(
            Minecraft client,
            LocalPlayer player
    ) {
        if (departureDimension == null || departureFeet == null) {
            return false;
        }

        boolean dimensionChanged =
                !client.level.dimension().equals(departureDimension);

        
        boolean positionChanged =
                player.position().distanceTo(departureFeet) > 4.0D;

        return dimensionChanged || positionChanged;
    }


    private static boolean isDestinationLoadAndRenderComplete(
            Minecraft client,
            LocalPlayer player
    ) {
        if (client.level == null) {
            loadingStatus =
                    LoadingStatus.WAITING_FOR_WORLD;
            return false;
        }

        loadingStatus =
                LoadingStatus.LOADING_CHUNKS;

        
        int renderDistance = Math.max(
                1,
                client.options.getEffectiveRenderDistance()
        );

        ClientChunkCache chunkManager =
                client.level.getChunkSource();

        ChunkPos center =
                player.chunkPosition();

        
        int radiusSquared =
                renderDistance * renderDistance;

        int totalChunkCount = 0;
        int loadedChunkCount = 0;

        for (int offsetX = -renderDistance;
             offsetX <= renderDistance;
             offsetX++) {

            for (int offsetZ = -renderDistance;
                 offsetZ <= renderDistance;
                 offsetZ++) {

                
                if (offsetX * offsetX
                        + offsetZ * offsetZ
                        > radiusSquared) {
                    continue;
                }

                totalChunkCount++;

                if (chunkManager.hasChunk(
                        center.x + offsetX,
                        center.z + offsetZ
                )) {
                    loadedChunkCount++;
                }
            }
        }

        if (totalChunkCount == 0) {
            lastLoadedChunkRatio = 0.0F;
            return false;
        }

        float loadedChunkRatio =
                loadedChunkCount
                        / (float) totalChunkCount;

        lastLoadedChunkRatio =
                Mth.clamp(
                        loadedChunkRatio,
                        0.0F,
                        1.0F
                );

        
        if (loadedChunkRatio
                < REQUIRED_RENDER_CHUNK_RATIO) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        
        if (!chunkManager.hasChunk(center.x, center.z)) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        //? if >=1.20.5 {
        SectionRenderDispatcher chunkBuilder =
                client.levelRenderer.getSectionRenderDispatcher();
        //?} else {
        /*ChunkRenderDispatcher chunkBuilder =
                client.levelRenderer.getChunkRenderDispatcher();
        *///?}

        if (chunkBuilder == null) {
            emptyRenderQueueStableTicks = 0;
            loadingStatus =
                    LoadingStatus.PREPARING_RENDER;
            return false;
        }

        
        loadingStatus =
                LoadingStatus.PREPARING_RENDER;

        //? if >=1.20.5 {
        boolean playerSectionCompiled = client.levelRenderer
                .isSectionCompiled(player.blockPosition());
        //?} else {
        /*boolean playerSectionCompiled = client.levelRenderer
                .isChunkCompiled(player.blockPosition());
        *///?}

        if (!playerSectionCompiled) {
            if (canFinishWithoutRenderableTerrain(
                    client,
                    player,
                    chunkBuilder
            )) {
                loadingStatus =
                        LoadingStatus.FINALIZING;
                return true;
            }

            return false;
        }

        emptyRenderQueueStableTicks = 0;

        
        loadingStatus =
                LoadingStatus.BUILDING_TERRAIN;

        //? if >=1.20.5 {
        boolean renderedAllSections = client.levelRenderer
                .hasRenderedAllSections();
        //?} else {
        /*boolean renderedAllSections = client.levelRenderer
                .hasRenderedAllChunks();
        *///?}

        if (!renderedAllSections) {
            return false;
        }

        
        if (!chunkBuilder.isQueueEmpty()
                || chunkBuilder.getToBatchCount() > 0) {
            loadingStatus =
                    LoadingStatus.BUILDING_CHUNK_MESHES;
            return false;
        }

        
        if (chunkBuilder.getToUpload() > 0) {
            loadingStatus =
                    LoadingStatus.UPLOADING_TO_GPU;
            return false;
        }

        loadingStatus =
                LoadingStatus.FINALIZING;

        return true;
    }


    private static boolean canFinishWithoutRenderableTerrain(
            Minecraft client,
            LocalPlayer player,
            //? if >=1.20.5 {
            SectionRenderDispatcher chunkBuilder
            //?} else {
            /*ChunkRenderDispatcher chunkBuilder
            *///?}
    ) {
        boolean renderQueueIdle =
                chunkBuilder.isQueueEmpty()
                        && chunkBuilder.getToBatchCount() <= 0
                        && chunkBuilder.getToUpload() <= 0;

        if (!renderQueueIdle) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        
        if (!hasNoRenderableTerrainNearby(client, player)) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        emptyRenderQueueStableTicks++;

        return emptyRenderQueueStableTicks
                >= REQUIRED_EMPTY_TERRAIN_STABLE_TICKS;
    }

    
    private static boolean hasNoRenderableTerrainNearby(
            Minecraft client,
            LocalPlayer player
    ) {
        if (client.level == null) {
            return false;
        }

        int renderDistance = Math.max(
                1,
                client.options.getEffectiveRenderDistance()
        );

        int scanRadius = Math.max(
                MIN_EMPTY_TERRAIN_SCAN_RADIUS,
                (renderDistance
                        + EMPTY_TERRAIN_SCAN_RADIUS_DIVISOR - 1)
                        / EMPTY_TERRAIN_SCAN_RADIUS_DIVISOR
        );

        int radiusSquared = scanRadius * scanRadius;
        ChunkPos centerChunk = player.chunkPosition();
        int centerSectionY = Math.floorDiv(
                player.getBlockY(),
                16
        );
        int bottomSectionY = Math.floorDiv(
                client.level.getMinBuildHeight(),
                16
        );

        ClientChunkCache chunkManager =
                client.level.getChunkSource();

        for (int offsetX = -scanRadius;
             offsetX <= scanRadius;
             offsetX++) {

            for (int offsetZ = -scanRadius;
                 offsetZ <= scanRadius;
                 offsetZ++) {

                int horizontalDistanceSquared =
                        offsetX * offsetX
                                + offsetZ * offsetZ;

                if (horizontalDistanceSquared > radiusSquared) {
                    continue;
                }

                LevelChunk chunk = chunkManager.getChunk(
                        centerChunk.x + offsetX,
                        centerChunk.z + offsetZ,
                        ChunkStatus.FULL,
                        false
                );

                
                if (chunk == null) {
                    return false;
                }

                LevelChunkSection[] sections =
                        chunk.getSections();

                for (int sectionIndex = 0;
                     sectionIndex < sections.length;
                     sectionIndex++) {

                    int sectionY =
                            bottomSectionY + sectionIndex;
                    int offsetY =
                            sectionY - centerSectionY;

                    int distanceSquared =
                            horizontalDistanceSquared
                                    + offsetY * offsetY;

                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    LevelChunkSection section =
                            sections[sectionIndex];

                    if (section != null && !section.hasOnlyAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static void finishRenderWait() {
        if (activeNetworkSessionId == null) {
            setPhase(Phase.DESTINATION_HOLD);
            return;
        }

        if (renderReadySent) {
            return;
        }

        renderReadySent = true;

        TeleportClientNetworking.sendState(
                activeNetworkSessionId,
                TeleportClientState.RENDER_READY
        );

        setPhase(Phase.WAITING_FOR_REBUILD_SIGNAL);
    }

    private static void captureArrival(
            Minecraft client
    ) {
        LocalPlayer player = client.player;

        if (player == null || client.level == null) {
            cancel();
            return;
        }

        arrivalFirstPerson = createFirstPersonPose(player);
        arrivalFocus = createFocus(player);

        arrivalSide = createSidePose(
                player,
                arrivalFirstPerson.yaw(),
                arrivalFocus
        );

        arrivalCaptureRequested = false;
        arrivalCaptureDelay = 0;

        renderReadyStableTicks = 0;
        emptyRenderQueueStableTicks = 0;
        terrainUpdateScheduled = false;

        setPhase(Phase.WAITING_FOR_RENDER);
    }

    private static CameraPose createFirstPersonPose(LocalPlayer player) {
        return new CameraPose(
                player.getEyePosition(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private static CameraPose createSidePose(
            LocalPlayer player,
            float playerYaw,
            Vec3 focus
    ) {
        double yawRadians = Math.toRadians(playerYaw);

        Vec3 right = new Vec3(
                Math.cos(yawRadians),
                0.0D,
                Math.sin(yawRadians)
        );

        Vec3 forward = new Vec3(
                -Math.sin(yawRadians),
                0.0D,
                Math.cos(yawRadians)
        );

        Vec3 cameraPosition = focus
                .add(right.scale(SIDE_DISTANCE))
                .add(forward.scale(SIDE_FORWARD_OFFSET))
                .add(0.0D, SIDE_HEIGHT_OFFSET, 0.0D);

        return lookAt(cameraPosition, focus);
    }

    private static CameraPose lookAt(Vec3 cameraPosition, Vec3 target) {
        Vec3 difference = target.subtract(cameraPosition);

        double horizontalDistance = Math.sqrt(
                difference.x * difference.x
                        + difference.z * difference.z
        );

        float yaw = (float) Math.toDegrees(
                Math.atan2(difference.z, difference.x)
        ) - 90.0F;

        float pitch = (float) -Math.toDegrees(
                Math.atan2(difference.y, horizontalDistance)
        );

        return new CameraPose(cameraPosition, yaw, pitch);
    }

    private static float progress(float partialTick, int durationTicks) {
        return Mth.clamp(
                (phaseTicks + partialTick) / durationTicks,
                0.0F,
                1.0F
        );
    }

    
    private static float easeOutCubic(float value) {
        float x = Mth.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - x;

        return 1.0F - inverse * inverse * inverse;
    }

    
    private static float easeInOutCubic(float value) {
        float x = Mth.clamp(value, 0.0F, 1.0F);

        if (x < 0.5F) {
            return 4.0F * x * x * x;
        }

        float inverse = -2.0F * x + 2.0F;

        return 1.0F
                - inverse * inverse * inverse / 2.0F;
    }

    private static void setPhase(Phase newPhase) {
        phase = newPhase;
        phaseTicks = 0;

        if (newPhase == Phase.FADE_IN_AND_ORBIT_BACK
                || newPhase
                == Phase.RETURN_TO_FIRST_PERSON
                || newPhase == Phase.IDLE) {
            loadingStatus =
                    LoadingStatus.NONE;
        }
    }

    private static void finish(Minecraft client) {
        
        client.options.setCameraType(CameraType.FIRST_PERSON);
        clear();
    }

    private static void clear() {
        phase = Phase.IDLE;
        phaseTicks = 0;
        activeTimings = TeleportTimingProfile.defaults();

        previousCameraType = null;

        departureFirstPerson = null;
        departureSide = null;

        arrivalFirstPerson = null;
        arrivalSide = null;

        departureDimension = null;
        departureFeet = null;

        actionWhenFullyBlack = null;

        arrivalCaptureRequested = false;
        arrivalCaptureDelay = 0;

        renderReadyStableTicks = 0;
        emptyRenderQueueStableTicks = 0;
        terrainUpdateScheduled = false;
        renderReadySent = false;
        activeNetworkSessionId = null;

        blackoutWaitTicks = 0;
        lastLoadedChunkRatio = 0.0F;
        loadingStatus = LoadingStatus.NONE;

        departureFocus = null;
        arrivalFocus = null;

    }

    private static float interpolateDegrees(
            float from,
            float to,
            float progress
    ) {
        float difference = Mth.wrapDegrees(to - from);
        return from + difference * progress;
    }

    private enum LoadingStatus {
        NONE,
        WAITING_FOR_WORLD,
        WAITING_FOR_TELEPORT,
        LOADING_CHUNKS,
        PREPARING_RENDER,
        BUILDING_TERRAIN,
        BUILDING_CHUNK_MESHES,
        UPLOADING_TO_GPU,
        WAITING_FOR_REBUILD_SIGNAL,
        FINALIZING
    }

    private enum Phase {
        IDLE,

        
        MOVE_TO_SIDE,

        
        ORBIT_AND_FADE_OUT,

        
        WAITING_FOR_TELEPORT,

        
        WAITING_FOR_RENDER,

        
        WAITING_FOR_REBUILD_SIGNAL,

        
        DESTINATION_HOLD,

        
        FADE_IN_AND_ORBIT_BACK,

        
        RETURN_TO_FIRST_PERSON
    }

    
    private static float getRebuildVisualProgress(float tickDelta) {
        if (phase != Phase.FADE_IN_AND_ORBIT_BACK) {
            return 0.0F;
        }

        return Mth.clamp(
                (
                        phaseTicks
                                + tickDelta
                                - activeTimings.rebuildMeshDelayTicks()
                ) / activeTimings.rebuildTicks(),
                0.0F,
                1.0F
        );
    }

    public static float getRebuildingProgress(float tickDelta) {
        if (phase != Phase.FADE_IN_AND_ORBIT_BACK) {
            return 0.0F;
        }

        
        float rawProgress = progress(
                tickDelta,
                activeTimings.rebuildTicks()
        );

        return smootherStep(rawProgress);
    }

    private record CameraPose(
            Vec3 position,
            float yaw,
            float pitch
    ) {
        private CameraPose interpolate(
                CameraPose target,
                float progress
        ) {
            Vec3 interpolatedPosition =
                    position.lerp(target.position, progress);

            float interpolatedYaw = interpolateDegrees(
                    yaw,
                    target.yaw,
                    progress
            );

            float interpolatedPitch = Mth.lerp(
                    progress,
                    pitch,
                    target.pitch
            );

            return new CameraPose(
                    interpolatedPosition,
                    interpolatedYaw,
                    interpolatedPitch
            );
        }

        private CameraFrame toFrame() {
            return new CameraFrame(position, yaw, pitch);
        }
    }

    public static boolean isWaitingForTeleport() {
        return phase == Phase.WAITING_FOR_TELEPORT;
    }

    public static boolean shouldRenderDissolve() {
        return switch (phase) {
            case ORBIT_AND_FADE_OUT,
                 WAITING_FOR_TELEPORT,
                 WAITING_FOR_RENDER,
                 WAITING_FOR_REBUILD_SIGNAL,
                 DESTINATION_HOLD,
                 FADE_IN_AND_ORBIT_BACK -> true;

            default -> false;
        };
    }

    public static boolean isDissolvingOut() {
        return phase == Phase.ORBIT_AND_FADE_OUT;
    }

    public static boolean isRebuilding() {
        return phase == Phase.FADE_IN_AND_ORBIT_BACK;
    }

    public static float getDissolveProgress(float tickDelta) {
        return switch (phase) {
            
            case ORBIT_AND_FADE_OUT -> {
                float progress = progress(
                        tickDelta,
                        activeTimings.dissolveTicks()
                );

                yield smootherStep(progress);
            }

            
            case WAITING_FOR_TELEPORT,
                 WAITING_FOR_RENDER,
                 WAITING_FOR_REBUILD_SIGNAL,
                 DESTINATION_HOLD -> 1.0F;

            
            case FADE_IN_AND_ORBIT_BACK -> {
                float progress =
                        getRebuildVisualProgress(tickDelta);

                yield 1.0F - smootherStep(progress);
            }

            default -> 0.0F;
        };
    }


    public record CameraFrame(
            Vec3 position,
            float yaw,
            float pitch
    ) {
    }
}
