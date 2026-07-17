package net.ochibo.twilightteleport.client.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;


public final class TeleportRenderedHeightManager {

    
    private static final float MAX_EXTRA_RENDER_HEIGHT = 8.0F;

    
    private static final float MAX_HEIGHT_GUARD_EPSILON = 0.08F;

    private static final float MAX_CROSS_SECTION_RADIUS = 4.0F;
    private static final float LOCAL_POSITION_EPSILON = 0.25F;

    
    private static final int MIN_TOP_SUPPORT_POINTS = 4;

    
    private static final int MIN_VALID_MESH_POINTS = 24;

    
    private static final int MAX_INITIAL_CAPTURE_ATTEMPTS = 3;

    
    private static final int MIN_REJECTED_VERTICES_FOR_FAILURE = 8;
    private static final float MAX_REJECTED_VERTEX_RATIO = 0.20F;

    private static final int MAX_MESH_POINTS = 4096;
    private static final float MESH_POINT_QUANTIZATION = 64.0F;

    
    private static final Map<UUID, Float> MEASURED_HEIGHTS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, ItemStack> MEASURED_HEAD_STACKS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, List<MeshPoint>> MEASURED_MESH_POINTS =
            new ConcurrentHashMap<>();

    
    private static final Map<UUID, Float> EFFECT_HEIGHTS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, ItemStack> EFFECT_HEAD_STACKS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, List<MeshPoint>> EFFECT_MESH_POINTS =
            new ConcurrentHashMap<>();

    
    private static final Map<UUID, Float> EFFECT_CAPTURE_YAWS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> CAPTURE_FAILURE_COUNTS =
            new ConcurrentHashMap<>();

    
    private static final Map<UUID, UUID> EFFECT_SESSION_IDS =
            new ConcurrentHashMap<>();

    
    private static final Set<UUID> EFFECT_CAPTURE_REQUESTS =
            ConcurrentHashMap.newKeySet();

    private static final Map<UUID, PendingSnapshot> PENDING_EFFECT_SNAPSHOTS =
            new ConcurrentHashMap<>();

    private static final Map<Item, Float> ITEM_EXTRA_HEIGHT_OVERRIDES =
            new ConcurrentHashMap<>();

    private static final Map<UUID, ActiveMeasurement> ACTIVE_MEASUREMENTS =
            new HashMap<>();

    private TeleportRenderedHeightManager() {
    }

    
    public static void beginEffectCapture(
            UUID playerUuid,
            UUID sessionId
    ) {
        UUID currentSession =
                EFFECT_SESSION_IDS.get(playerUuid);

        if (sessionId != null
                && sessionId.equals(currentSession)
                && (
                EFFECT_HEIGHTS.containsKey(playerUuid)
                        || EFFECT_CAPTURE_REQUESTS.contains(playerUuid)
                        || PENDING_EFFECT_SNAPSHOTS.containsKey(playerUuid)
        )) {
            return;
        }

        clearMeasured(playerUuid);
        clearEffectSnapshotOnly(playerUuid);

        ACTIVE_MEASUREMENTS.remove(playerUuid);
        PENDING_EFFECT_SNAPSHOTS.remove(playerUuid);
        CAPTURE_FAILURE_COUNTS.remove(playerUuid);

        if (sessionId != null) {
            EFFECT_SESSION_IDS.put(
                    playerUuid,
                    sessionId
            );
        } else {
            EFFECT_SESSION_IDS.remove(playerUuid);
        }

        EFFECT_CAPTURE_REQUESTS.add(playerUuid);
    }

    
    public static void beginPlayerRender(
            AbstractClientPlayer player,
            float tickDelta,
            float renderYaw
    ) {
        UUID playerUuid = player.getUUID();

        
        if (TeleportShaderPackCompat.isShadowPass()) {
            ACTIVE_MEASUREMENTS.remove(playerUuid);
            return;
        }

        if (!EFFECT_CAPTURE_REQUESTS.contains(playerUuid)
                && !PENDING_EFFECT_SNAPSHOTS.containsKey(playerUuid)) {
            ACTIVE_MEASUREMENTS.remove(playerUuid);
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        if (client.gameRenderer == null) {
            return;
        }

        Vec3 cameraPosition =
                client.gameRenderer.getMainCamera().getPosition();

        Vec3 playerPosition =
                player.getPosition(tickDelta);

        Vec3 cameraRelativePlayerOrigin =
                playerPosition.subtract(cameraPosition);

        ACTIVE_MEASUREMENTS.put(
                playerUuid,
                new ActiveMeasurement(
                        (float) cameraRelativePlayerOrigin.x,
                        (float) cameraRelativePlayerOrigin.y,
                        (float) cameraRelativePlayerOrigin.z,
                        player.getBbHeight(),
                        copyHeadStack(player),
                        renderYaw
                )
        );
    }

    public static boolean isMeasurementActive(
            UUID playerUuid
    ) {
        return ACTIVE_MEASUREMENTS.containsKey(playerUuid);
    }

    
    public static void updateMeasurementRenderYaw(
            UUID playerUuid,
            float renderYaw
    ) {
        ActiveMeasurement measurement =
                ACTIVE_MEASUREMENTS.get(playerUuid);

        if (measurement != null) {
            measurement.setRenderYaw(renderYaw);
        }
    }

    public static void recordVertex(
            UUID playerUuid,
            float transformedX,
            float transformedY,
            float transformedZ
    ) {
        ActiveMeasurement measurement =
                ACTIVE_MEASUREMENTS.get(playerUuid);

        if (measurement == null) {
            return;
        }

        measurement.record(
                transformedX,
                transformedY,
                transformedZ
        );
    }

    
    public static void endPlayerRender(
            AbstractClientPlayer player
    ) {
        UUID playerUuid = player.getUUID();

        ActiveMeasurement measurement =
                ACTIVE_MEASUREMENTS.remove(playerUuid);

        if (measurement == null) {
            return;
        }

        if (!EFFECT_CAPTURE_REQUESTS.contains(playerUuid)
                && !PENDING_EFFECT_SNAPSHOTS.containsKey(playerUuid)) {
            return;
        }

        ItemStack currentHeadStack =
                copyHeadStack(player);

        
        if (!sameHeadStack(
                measurement.headStack(),
                currentHeadStack
        )) {
            clearMeasured(playerUuid);

            PENDING_EFFECT_SNAPSHOTS.put(
                    playerUuid,
                    new PendingSnapshot(currentHeadStack)
            );

            EFFECT_CAPTURE_REQUESTS.add(playerUuid);
            return;
        }

        float baseHeight =
                Math.max(
                        0.01F,
                        player.getBbHeight()
                );

        float fallbackHeight =
                getSafeFallbackHeight(player);

        List<MeshPoint> measuredMeshPoints =
                measurement.createLocalMeshSnapshot();

        float rawMeasuredHeight =
                Math.max(
                        measurement.supportedMaximumLocalY(),
                        getOverrideHeight(player)
                );

        boolean invalidMeasurement =
                !measurement.hasValidVertex()
                        || measuredMeshPoints.size()
                        < MIN_VALID_MESH_POINTS
                        || measurement.hasTooManyRejectedVertices()
                        || measurement.reachesMaximumHeightGuard();

        
        float maximumAllowedHeight =
                baseHeight + MAX_EXTRA_RENDER_HEIGHT;

        if (rawMeasuredHeight
                >= maximumAllowedHeight
                - MAX_HEIGHT_GUARD_EPSILON) {
            invalidMeasurement = true;
        }

        if (invalidMeasurement) {
            clearMeasured(playerUuid);

            int failedAttempts =
                    CAPTURE_FAILURE_COUNTS.merge(
                            playerUuid,
                            1,
                            Integer::sum
                    );

            
            if (failedAttempts < MAX_INITIAL_CAPTURE_ATTEMPTS) {
                PENDING_EFFECT_SNAPSHOTS.put(
                        playerUuid,
                        new PendingSnapshot(currentHeadStack)
                );
                EFFECT_CAPTURE_REQUESTS.add(playerUuid);
                return;
            }

            CAPTURE_FAILURE_COUNTS.remove(playerUuid);

            
            commitEffectSnapshot(
                    playerUuid,
                    currentHeadStack,
                    fallbackHeight,
                    List.of(),
                    measurement.renderYaw()
            );

            return;
        }

        float measuredHeight =
                Mth.clamp(
                        rawMeasuredHeight,
                        baseHeight,
                        maximumAllowedHeight
                );

        CAPTURE_FAILURE_COUNTS.remove(playerUuid);

        storeMeasuredSnapshot(
                playerUuid,
                currentHeadStack,
                measuredHeight,
                measuredMeshPoints
        );

        commitEffectSnapshot(
                playerUuid,
                currentHeadStack,
                measuredHeight,
                measuredMeshPoints,
                measurement.renderYaw()
        );

        
    }

    
    public static float getEffectHeight(
            AbstractClientPlayer player
    ) {
        Float effectHeight =
                EFFECT_HEIGHTS.get(
                        player.getUUID()
                );

        if (effectHeight != null) {
            return effectHeight;
        }

        return getSafeFallbackHeight(player);
    }

    
    public static Vec3 sampleCrossSectionPoint(
            AbstractClientPlayer player,
            float localBoundaryY,
            float tolerance,
            RandomSource random
    ) {
        List<MeshPoint> points =
                EFFECT_MESH_POINTS.getOrDefault(
                        player.getUUID(),
                        List.of()
                );

        float resolvedTolerance =
                Math.max(0.01F, tolerance);

        float minimumX = Float.POSITIVE_INFINITY;
        float maximumX = Float.NEGATIVE_INFINITY;
        float minimumZ = Float.POSITIVE_INFINITY;
        float maximumZ = Float.NEGATIVE_INFINITY;

        int matchingPointCount = 0;

        float captureYaw =
                EFFECT_CAPTURE_YAWS.getOrDefault(
                        player.getUUID(),
                        player.getVisualRotationYInDegrees()
                );

        float currentYaw = player.getVisualRotationYInDegrees();

        for (MeshPoint point : points) {
            if (Math.abs(point.y() - localBoundaryY)
                    > resolvedTolerance) {
                continue;
            }

            Vec3 rotated = rotateHorizontalOffset(
                    point.x(),
                    point.z(),
                    captureYaw,
                    currentYaw
            );

            matchingPointCount++;

            minimumX = Math.min(minimumX, (float) rotated.x);
            maximumX = Math.max(maximumX, (float) rotated.x);
            minimumZ = Math.min(minimumZ, (float) rotated.z);
            maximumZ = Math.max(maximumZ, (float) rotated.z);
        }

        if (matchingPointCount < 2) {
            float halfWidth =
                    Math.max(
                            0.08F,
                            player.getBbWidth() * 0.52F
                    );

            minimumX = -halfWidth;
            maximumX = halfWidth;
            minimumZ = -halfWidth;
            maximumZ = halfWidth;
        } else {
            minimumX = Mth.clamp(
                    minimumX,
                    -MAX_CROSS_SECTION_RADIUS,
                    MAX_CROSS_SECTION_RADIUS
            );

            maximumX = Mth.clamp(
                    maximumX,
                    -MAX_CROSS_SECTION_RADIUS,
                    MAX_CROSS_SECTION_RADIUS
            );

            minimumZ = Mth.clamp(
                    minimumZ,
                    -MAX_CROSS_SECTION_RADIUS,
                    MAX_CROSS_SECTION_RADIUS
            );

            maximumZ = Mth.clamp(
                    maximumZ,
                    -MAX_CROSS_SECTION_RADIUS,
                    MAX_CROSS_SECTION_RADIUS
            );

            float minimumSectionWidth = 0.08F;

            if (maximumX - minimumX < minimumSectionWidth) {
                float centerX =
                        (minimumX + maximumX) * 0.5F;

                minimumX =
                        centerX - minimumSectionWidth * 0.5F;

                maximumX =
                        centerX + minimumSectionWidth * 0.5F;
            }

            if (maximumZ - minimumZ < minimumSectionWidth) {
                float centerZ =
                        (minimumZ + maximumZ) * 0.5F;

                minimumZ =
                        centerZ - minimumSectionWidth * 0.5F;

                maximumZ =
                        centerZ + minimumSectionWidth * 0.5F;
            }
        }

        double localX =
                minimumX
                        + random.nextDouble()
                        * (maximumX - minimumX);

        double localZ =
                minimumZ
                        + random.nextDouble()
                        * (maximumZ - minimumZ);

        Vec3 playerPosition =
                player.position();

        return new Vec3(
                playerPosition.x + localX,
                playerPosition.y + localBoundaryY,
                playerPosition.z + localZ
        );
    }

    public static Vec3 sampleMeshPointNearHeight(
            AbstractClientPlayer player,
            float localBoundaryY,
            float tolerance,
            RandomSource random
    ) {
        List<MeshPoint> points =
                EFFECT_MESH_POINTS.getOrDefault(
                        player.getUUID(),
                        List.of()
                );

        if (points.isEmpty()) {
            return null;
        }

        float resolvedTolerance =
                Math.max(0.01F, tolerance);

        MeshPoint selected = null;
        int matchingCount = 0;

        for (MeshPoint point : points) {
            if (Math.abs(point.y() - localBoundaryY)
                    > resolvedTolerance) {
                continue;
            }

            matchingCount++;

            if (random.nextInt(matchingCount) == 0) {
                selected = point;
            }
        }

        if (selected == null) {
            return null;
        }

        Vec3 playerPosition =
                player.position();

        float captureYaw =
                EFFECT_CAPTURE_YAWS.getOrDefault(
                        player.getUUID(),
                        player.getVisualRotationYInDegrees()
                );

        Vec3 rotated = rotateHorizontalOffset(
                selected.x(),
                selected.z(),
                captureYaw,
                player.getVisualRotationYInDegrees()
        );

        return new Vec3(
                playerPosition.x + rotated.x,
                playerPosition.y + selected.y(),
                playerPosition.z + rotated.z
        );
    }

    
    public static Vec3 rotateHorizontalOffset(
            double offsetX,
            double offsetZ,
            float fromYaw,
            float toYaw
    ) {
        float angle = (float) Math.toRadians(
                Mth.wrapDegrees(fromYaw - toYaw)
        );

        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);

        return new Vec3(
                offsetX * cosine + offsetZ * sine,
                0.0D,
                -offsetX * sine + offsetZ * cosine
        );
    }

    public static void registerHeadItemExtraHeight(
            Item item,
            float extraHeight
    ) {
        ITEM_EXTRA_HEIGHT_OVERRIDES.put(
                item,
                Math.max(0.0F, extraHeight)
        );
    }

    
    public static boolean ensureEffectSnapshotReady(
            AbstractClientPlayer player
    ) {
        UUID playerUuid = player.getUUID();

        if (EFFECT_HEIGHTS.containsKey(playerUuid)) {
            return true;
        }

        EFFECT_CAPTURE_REQUESTS.add(playerUuid);

        PENDING_EFFECT_SNAPSHOTS.computeIfAbsent(
                playerUuid,
                uuid -> new PendingSnapshot(
                        copyHeadStack(player)
                )
        );

        return false;
    }

    public static boolean isEffectSnapshotReady(
            AbstractClientPlayer player
    ) {
        return EFFECT_HEIGHTS.containsKey(
                player.getUUID()
        );
    }

    public static boolean isUsingMeasuredMesh(
            UUID playerUuid
    ) {
        return !EFFECT_MESH_POINTS
                .getOrDefault(
                        playerUuid,
                        List.of()
                )
                .isEmpty();
    }

    public static void clearEffect(
            UUID playerUuid
    ) {
        clearEffectSnapshotOnly(playerUuid);

        ACTIVE_MEASUREMENTS.remove(playerUuid);
        EFFECT_CAPTURE_REQUESTS.remove(playerUuid);
        PENDING_EFFECT_SNAPSHOTS.remove(playerUuid);
        EFFECT_SESSION_IDS.remove(playerUuid);
        EFFECT_CAPTURE_YAWS.remove(playerUuid);
        CAPTURE_FAILURE_COUNTS.remove(playerUuid);
    }

    public static void clearAll() {
        ACTIVE_MEASUREMENTS.clear();

        MEASURED_HEIGHTS.clear();
        MEASURED_HEAD_STACKS.clear();
        MEASURED_MESH_POINTS.clear();

        EFFECT_HEIGHTS.clear();
        EFFECT_HEAD_STACKS.clear();
        EFFECT_MESH_POINTS.clear();
        EFFECT_CAPTURE_YAWS.clear();
        EFFECT_SESSION_IDS.clear();
        CAPTURE_FAILURE_COUNTS.clear();

        EFFECT_CAPTURE_REQUESTS.clear();
        PENDING_EFFECT_SNAPSHOTS.clear();
    }

    private static float getSafeFallbackHeight(
            AbstractClientPlayer player
    ) {
        float baseHeight =
                Math.max(
                        0.01F,
                        player.getBbHeight()
                );

        return Mth.clamp(
                Math.max(
                        baseHeight,
                        getOverrideHeight(player)
                ),
                baseHeight,
                baseHeight + MAX_EXTRA_RENDER_HEIGHT
        );
    }

    private static void storeMeasuredSnapshot(
            UUID playerUuid,
            ItemStack headStack,
            float height,
            List<MeshPoint> meshPoints
    ) {
        MEASURED_HEIGHTS.put(
                playerUuid,
                height
        );

        MEASURED_HEAD_STACKS.put(
                playerUuid,
                headStack.copy()
        );

        MEASURED_MESH_POINTS.put(
                playerUuid,
                List.copyOf(meshPoints)
        );
    }

    private static void commitEffectSnapshot(
            UUID playerUuid,
            ItemStack headStack,
            float height,
            List<MeshPoint> meshPoints,
            float captureYaw
    ) {
        EFFECT_HEIGHTS.put(
                playerUuid,
                height
        );

        EFFECT_HEAD_STACKS.put(
                playerUuid,
                headStack.copy()
        );

        EFFECT_MESH_POINTS.put(
                playerUuid,
                meshPoints.isEmpty()
                        ? List.of()
                        : List.copyOf(meshPoints)
        );

        EFFECT_CAPTURE_YAWS.put(
                playerUuid,
                captureYaw
        );

        EFFECT_CAPTURE_REQUESTS.remove(playerUuid);
        PENDING_EFFECT_SNAPSHOTS.remove(playerUuid);
        ACTIVE_MEASUREMENTS.remove(playerUuid);
    }

    private static void clearMeasured(
            UUID playerUuid
    ) {
        MEASURED_HEIGHTS.remove(playerUuid);
        MEASURED_HEAD_STACKS.remove(playerUuid);
        MEASURED_MESH_POINTS.remove(playerUuid);
    }

    private static void clearEffectSnapshotOnly(
            UUID playerUuid
    ) {
        EFFECT_HEIGHTS.remove(playerUuid);
        EFFECT_HEAD_STACKS.remove(playerUuid);
        EFFECT_MESH_POINTS.remove(playerUuid);
        EFFECT_CAPTURE_YAWS.remove(playerUuid);
    }

    private static ItemStack copyHeadStack(
            AbstractClientPlayer player
    ) {
        ItemStack stack =
                player.getItemBySlot(
                        EquipmentSlot.HEAD
                );

        return stack.isEmpty()
                ? ItemStack.EMPTY
                : stack.copy();
    }

    private static boolean sameHeadStack(
            ItemStack left,
            ItemStack right
    ) {
        if (left == null || right == null) {
            return false;
        }

        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty()
                    && right.isEmpty();
        }

        //? if >=1.20.5 {
        return ItemStack.isSameItemSameComponents(
                left,
                right
        );
        //?} else {
        /*return ItemStack.isSameItemSameTags(
                left,
                right
        );
        *///?}
    }

    private static float getOverrideHeight(
            AbstractClientPlayer player
    ) {
        ItemStack headStack =
                player.getItemBySlot(
                        EquipmentSlot.HEAD
                );

        if (headStack.isEmpty()) {
            return Math.max(
                    0.01F,
                    player.getBbHeight()
            );
        }

        float extraHeight =
                ITEM_EXTRA_HEIGHT_OVERRIDES.getOrDefault(
                        headStack.getItem(),
                        0.0F
                );

        return Math.max(
                0.01F,
                player.getBbHeight()
        ) + extraHeight;
    }

    private static final class ActiveMeasurement {
        private final float playerOriginX;
        private final float playerOriginY;
        private final float playerOriginZ;
        private final float fallbackHeight;
        private final ItemStack headStack;
        private float renderYaw;

        private final List<MeshPoint> transformedPoints =
                new ArrayList<>();

        private final Set<QuantizedPoint> uniquePoints =
                new HashSet<>();

        private int acceptedVertexCount;
        private int rejectedVertexCount;

        private ActiveMeasurement(
                float playerOriginX,
                float playerOriginY,
                float playerOriginZ,
                float fallbackHeight,
                ItemStack headStack,
                float renderYaw
        ) {
            this.playerOriginX = playerOriginX;
            this.playerOriginY = playerOriginY;
            this.playerOriginZ = playerOriginZ;
            this.fallbackHeight = fallbackHeight;
            this.headStack = headStack.copy();
            this.renderYaw = renderYaw;
        }

        private void record(
                float transformedX,
                float transformedY,
                float transformedZ
        ) {
            if (!Float.isFinite(transformedX)
                    || !Float.isFinite(transformedY)
                    || !Float.isFinite(transformedZ)) {
                rejectedVertexCount++;
                return;
            }

            float localX =
                    transformedX - playerOriginX;

            float localY =
                    transformedY - playerOriginY;

            float localZ =
                    transformedZ - playerOriginZ;

            float maximumAllowedY =
                    fallbackHeight
                            + MAX_EXTRA_RENDER_HEIGHT
                            + LOCAL_POSITION_EPSILON;

            boolean plausible =
                    Math.abs(localX)
                            <= MAX_CROSS_SECTION_RADIUS
                            + LOCAL_POSITION_EPSILON
                            && Math.abs(localZ)
                            <= MAX_CROSS_SECTION_RADIUS
                            + LOCAL_POSITION_EPSILON
                            && localY >= -LOCAL_POSITION_EPSILON
                            && localY <= maximumAllowedY;

            if (!plausible) {
                rejectedVertexCount++;
                return;
            }

            acceptedVertexCount++;

            if (transformedPoints.size()
                    >= MAX_MESH_POINTS) {
                return;
            }

            QuantizedPoint key =
                    new QuantizedPoint(
                            Math.round(
                                    localX
                                            * MESH_POINT_QUANTIZATION
                            ),
                            Math.round(
                                    localY
                                            * MESH_POINT_QUANTIZATION
                            ),
                            Math.round(
                                    localZ
                                            * MESH_POINT_QUANTIZATION
                            )
                    );

            if (!uniquePoints.add(key)) {
                return;
            }

            transformedPoints.add(
                    new MeshPoint(
                            localX,
                            localY,
                            localZ
                    )
            );
        }

        private boolean hasValidVertex() {
            return !transformedPoints.isEmpty();
        }

        private boolean hasTooManyRejectedVertices() {
            int total =
                    acceptedVertexCount
                            + rejectedVertexCount;

            if (total <= 0
                    || rejectedVertexCount
                    < MIN_REJECTED_VERTICES_FOR_FAILURE) {
                return false;
            }

            return rejectedVertexCount
                    / (float) total
                    > MAX_REJECTED_VERTEX_RATIO;
        }

        private boolean reachesMaximumHeightGuard() {
            if (transformedPoints.isEmpty()) {
                return false;
            }

            float maximumAllowed =
                    fallbackHeight
                            + MAX_EXTRA_RENDER_HEIGHT;

            return supportedMaximumLocalY()
                    >= maximumAllowed
                    - MAX_HEIGHT_GUARD_EPSILON;
        }

        private float supportedMaximumLocalY() {
            if (transformedPoints.isEmpty()) {
                return fallbackHeight;
            }

            float[] localYs =
                    new float[transformedPoints.size()];

            for (int index = 0;
                 index < transformedPoints.size();
                 index++) {
                localYs[index] =
                        transformedPoints.get(index).y();
            }

            Arrays.sort(localYs);

            int supportedIndex =
                    localYs.length >= MIN_TOP_SUPPORT_POINTS
                            ? localYs.length
                            - MIN_TOP_SUPPORT_POINTS
                            : localYs.length - 1;

            return localYs[supportedIndex];
        }

        private List<MeshPoint> createLocalMeshSnapshot() {
            return transformedPoints.isEmpty()
                    ? List.of()
                    : List.copyOf(transformedPoints);
        }

        private ItemStack headStack() {
            return headStack;
        }

        private void setRenderYaw(float renderYaw) {
            this.renderYaw = renderYaw;
        }

        private float renderYaw() {
            return renderYaw;
        }
    }

    private static final class PendingSnapshot {
        private final ItemStack headStack;

        private PendingSnapshot(
                ItemStack headStack
        ) {
            this.headStack = headStack.copy();
        }

        @SuppressWarnings("unused")
        private ItemStack headStack() {
            return headStack;
        }
    }

    private record MeshPoint(
            float x,
            float y,
            float z
    ) {
    }

    private record QuantizedPoint(
            int x,
            int y,
            int z
    ) {
    }
}
