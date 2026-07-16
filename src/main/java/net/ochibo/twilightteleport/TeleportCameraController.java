package net.ochibo.twilightteleport;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
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
    private static Vec3d departureFocus;
    private static Vec3d arrivalFocus;

    private static boolean arrivalCaptureRequested;
    private static int arrivalCaptureDelay;

    

    private static int renderReadyStableTicks;
    private static int emptyRenderQueueStableTicks;
    private static boolean terrainUpdateScheduled;

    private static int blackoutWaitTicks;
    private static float lastLoadedChunkRatio;
    private static LoadingStatus loadingStatus =
            LoadingStatus.NONE;

    private static Perspective previousCameraType;

    private static CameraPose departureFirstPerson;
    private static CameraPose departureSide;

    private static CameraPose arrivalFirstPerson;
    private static CameraPose arrivalSide;

    private static RegistryKey<World> departureDimension;
    private static Vec3d departureFeet;

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

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (isRunning() || player == null || client.world == null) {
            return;
        }

        activeTimings = timings == null
                ? TeleportTimingProfile.defaults()
                : timings;

        previousCameraType = client.options.getPerspective();

        departureFirstPerson = createFirstPersonPose(player);
        departureFocus = createFocus(player);

        departureSide = createSidePose(
                player,
                departureFirstPerson.yaw(),
                departureFocus
        );

        departureDimension = client.world.getRegistryKey();
        departureFeet = player.getPos();

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

        
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);

        setPhase(Phase.MOVE_TO_SIDE);
    }

    
    public static void markArrived() {
        if (phase != Phase.WAITING_FOR_TELEPORT) {
            return;
        }

        
        arrivalCaptureRequested = true;
        arrivalCaptureDelay = 0;
    }

    public static void tick(MinecraftClient client) {
        if (!isRunning()) {
            return;
        }

        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) {
            cancel();
            return;
        }


        client.options.setPerspective(
                Perspective.THIRD_PERSON_BACK
        );
        player.setSprinting(false);
        player.setSneaking(false);
        player.setVelocity(Vec3d.ZERO);

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
                    client.worldRenderer.scheduleTerrainUpdate();
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
            Vec3d focus,
            CameraPose sidePose,
            float phaseProgress,
            boolean reverse
    ) {
        float rawProgress = MathHelper.clamp(
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
                    MathHelper.clamp(
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

        Vec3d relativePosition =
                sidePose.position().subtract(focus);

        double angleRadians =
                Math.toRadians(
                        ORBIT_ANGLE_DEGREES
                                * orbitProgress
                );

        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        Vec3d rotatedOffset = new Vec3d(
                relativePosition.x * cos
                        - relativePosition.z * sin,

                relativePosition.y,

                relativePosition.x * sin
                        + relativePosition.z * cos
        );

        Vec3d cameraPosition =
                focus.add(rotatedOffset);

        CameraPose facingPlayer =
                lookAt(cameraPosition, focus);

        float pitch = MathHelper.lerp(
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
        float progress = MathHelper.clamp(
                value,
                0.0F,
                1.0F
        );

        float accelerationEnd =
                MathHelper.clamp(
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
        float start = MathHelper.clamp(
                startProgress,
                0.0F,
                0.999F
        );

        return MathHelper.clamp(
                (progress - start)
                        / (1.0F - start),
                0.0F,
                1.0F
        );
    }


    private static Vec3d createFocus(
            ClientPlayerEntity player
    ) {
        return player.getPos().add(
                0.0D,
                player.getHeight() * 0.55D,
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
        float fadeProgress = MathHelper.clamp(
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

    public static Text getLoadingStatusText() {
        return switch (loadingStatus) {
            case WAITING_FOR_WORLD ->
                    Text.translatable(
                            "hud.twilightteleport.loading.waiting_world"
                    );

            case WAITING_FOR_TELEPORT ->
                    Text.translatable(
                            "hud.twilightteleport.loading.waiting_teleport"
                    );

            case LOADING_CHUNKS -> {
                final int percentage =
                        Math.round(
                                MathHelper.clamp(
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

                yield Text.translatable(
                        "hud.twilightteleport.loading.chunks",
                        finalPercentage + "%"
                );
            }

            case PREPARING_RENDER ->
                    Text.translatable(
                            "hud.twilightteleport.loading.preparing_render"
                    );

            case BUILDING_TERRAIN ->
                    Text.translatable(
                            "hud.twilightteleport.loading.building_terrain"
                    );

            case BUILDING_CHUNK_MESHES ->
                    Text.translatable(
                            "hud.twilightteleport.loading.building_chunk_meshes"
                    );

            case UPLOADING_TO_GPU ->
                    Text.translatable(
                            "hud.twilightteleport.loading.uploading_gpu"
                    );

            case WAITING_FOR_REBUILD_SIGNAL ->
                    Text.translatable(
                            "hud.twilightteleport.loading.waiting_rebuild"
                    );

            case FINALIZING ->
                    Text.translatable(
                            "hud.twilightteleport.loading.finalizing"
                    );

            case NONE -> Text.empty();
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
        float x = MathHelper.clamp(
                value,
                0.0F,
                1.0F
        );

        
        return x * x * x
                * (x * (x * 6.0F - 15.0F) + 10.0F);
    }

    public static void cancel() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (previousCameraType != null) {
            client.options.setPerspective(previousCameraType);
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
            MinecraftClient client,
            ClientPlayerEntity player
    ) {
        if (departureDimension == null || departureFeet == null) {
            return false;
        }

        boolean dimensionChanged =
                !client.world.getRegistryKey().equals(departureDimension);

        
        boolean positionChanged =
                player.getPos().distanceTo(departureFeet) > 4.0D;

        return dimensionChanged || positionChanged;
    }


    private static boolean isDestinationLoadAndRenderComplete(
            MinecraftClient client,
            ClientPlayerEntity player
    ) {
        if (client.world == null) {
            loadingStatus =
                    LoadingStatus.WAITING_FOR_WORLD;
            return false;
        }

        loadingStatus =
                LoadingStatus.LOADING_CHUNKS;

        
        int renderDistance = Math.max(
                1,
                client.options.getClampedViewDistance()
        );

        ClientChunkManager chunkManager =
                client.world.getChunkManager();

        ChunkPos center =
                player.getChunkPos();

        
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

                if (chunkManager.isChunkLoaded(
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
                MathHelper.clamp(
                        loadedChunkRatio,
                        0.0F,
                        1.0F
                );

        
        if (loadedChunkRatio
                < REQUIRED_RENDER_CHUNK_RATIO) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        
        if (!chunkManager.isChunkLoaded(center.x, center.z)) {
            emptyRenderQueueStableTicks = 0;
            return false;
        }

        ChunkBuilder chunkBuilder =
                client.worldRenderer.getChunkBuilder();

        if (chunkBuilder == null) {
            emptyRenderQueueStableTicks = 0;
            loadingStatus =
                    LoadingStatus.PREPARING_RENDER;
            return false;
        }

        
        loadingStatus =
                LoadingStatus.PREPARING_RENDER;

        if (!client.worldRenderer
                .isRenderingReady(player.getBlockPos())) {
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

        if (!client.worldRenderer
                .isTerrainRenderComplete()) {
            return false;
        }

        
        if (!chunkBuilder.isEmpty()
                || chunkBuilder.getToBatchCount() > 0) {
            loadingStatus =
                    LoadingStatus.BUILDING_CHUNK_MESHES;
            return false;
        }

        
        if (chunkBuilder.getChunksToUpload() > 0) {
            loadingStatus =
                    LoadingStatus.UPLOADING_TO_GPU;
            return false;
        }

        loadingStatus =
                LoadingStatus.FINALIZING;

        return true;
    }


    private static boolean canFinishWithoutRenderableTerrain(
            MinecraftClient client,
            ClientPlayerEntity player,
            ChunkBuilder chunkBuilder
    ) {
        boolean renderQueueIdle =
                chunkBuilder.isEmpty()
                        && chunkBuilder.getToBatchCount() <= 0
                        && chunkBuilder.getChunksToUpload() <= 0;

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
            MinecraftClient client,
            ClientPlayerEntity player
    ) {
        if (client.world == null) {
            return false;
        }

        int renderDistance = Math.max(
                1,
                client.options.getClampedViewDistance()
        );

        int scanRadius = Math.max(
                MIN_EMPTY_TERRAIN_SCAN_RADIUS,
                (renderDistance
                        + EMPTY_TERRAIN_SCAN_RADIUS_DIVISOR - 1)
                        / EMPTY_TERRAIN_SCAN_RADIUS_DIVISOR
        );

        int radiusSquared = scanRadius * scanRadius;
        ChunkPos centerChunk = player.getChunkPos();
        int centerSectionY = Math.floorDiv(
                player.getBlockY(),
                16
        );
        int bottomSectionY = Math.floorDiv(
                client.world.getBottomY(),
                16
        );

        ClientChunkManager chunkManager =
                client.world.getChunkManager();

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

                WorldChunk chunk = chunkManager.getChunk(
                        centerChunk.x + offsetX,
                        centerChunk.z + offsetZ,
                        ChunkStatus.FULL,
                        false
                );

                
                if (chunk == null) {
                    return false;
                }

                ChunkSection[] sections =
                        chunk.getSectionArray();

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

                    ChunkSection section =
                            sections[sectionIndex];

                    if (section != null && !section.isEmpty()) {
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
            MinecraftClient client
    ) {
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) {
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

    private static CameraPose createFirstPersonPose(ClientPlayerEntity player) {
        return new CameraPose(
                player.getEyePos(),
                player.getYaw(),
                player.getPitch()
        );
    }

    private static CameraPose createSidePose(
            ClientPlayerEntity player,
            float playerYaw,
            Vec3d focus
    ) {
        double yawRadians = Math.toRadians(playerYaw);

        Vec3d right = new Vec3d(
                Math.cos(yawRadians),
                0.0D,
                Math.sin(yawRadians)
        );

        Vec3d forward = new Vec3d(
                -Math.sin(yawRadians),
                0.0D,
                Math.cos(yawRadians)
        );

        Vec3d cameraPosition = focus
                .add(right.multiply(SIDE_DISTANCE))
                .add(forward.multiply(SIDE_FORWARD_OFFSET))
                .add(0.0D, SIDE_HEIGHT_OFFSET, 0.0D);

        return lookAt(cameraPosition, focus);
    }

    private static CameraPose lookAt(Vec3d cameraPosition, Vec3d target) {
        Vec3d difference = target.subtract(cameraPosition);

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
        return MathHelper.clamp(
                (phaseTicks + partialTick) / durationTicks,
                0.0F,
                1.0F
        );
    }

    
    private static float easeOutCubic(float value) {
        float x = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - x;

        return 1.0F - inverse * inverse * inverse;
    }

    
    private static float easeInOutCubic(float value) {
        float x = MathHelper.clamp(value, 0.0F, 1.0F);

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

    private static void finish(MinecraftClient client) {
        
        client.options.setPerspective(Perspective.FIRST_PERSON);
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
        float difference = MathHelper.wrapDegrees(to - from);
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

        return MathHelper.clamp(
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
            Vec3d position,
            float yaw,
            float pitch
    ) {
        private CameraPose interpolate(
                CameraPose target,
                float progress
        ) {
            Vec3d interpolatedPosition =
                    position.lerp(target.position, progress);

            float interpolatedYaw = interpolateDegrees(
                    yaw,
                    target.yaw,
                    progress
            );

            float interpolatedPitch = MathHelper.lerp(
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
            Vec3d position,
            float yaw,
            float pitch
    ) {
    }
}
