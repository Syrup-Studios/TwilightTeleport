package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ochibo.twilightteleport.TeleportTimings;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import com.mojang.blaze3d.shaders.Uniform;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportDissolveRenderState {
    private static final float NOISE_SCALE = 0.1F;
    private static final float NOISE_STRENGTH = 0.18F;
    private static final float EDGE_WIDTH = 0.3F;


    
    private static final float BLACK_CORE_WIDTH = 0.1F;

    
    private static final float REBUILD_BLOCK_NOISE_SCALE = 14.0F;

    
    private static final float REBUILD_BLOCK_BAND_WIDTH = 0.145F;

    
    private static final float REBUILD_BLOCK_STRENGTH = 0.1F;

    
    private static final float REBUILD_BLOCK_SPEED = 14.0F;

    
    private static final float IRIS_BLOCK_NOISE_SCALE = 24F;
    private static final float IRIS_BLOCK_BAND_WIDTH = 0.2F;
    private static final float IRIS_BLOCK_STRENGTH = 0.1F;

    private static final Map<UUID, PreparedState> STATES =
            new ConcurrentHashMap<>();

    private TeleportDissolveRenderState() {
    }

    public static void prepare(
            AbstractClientPlayer player,
            float tickDelta
    ) {
        Minecraft client =
                Minecraft.getInstance();

        Vec3 cameraPosition =
                client.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 playerPosition =
                player.getPosition(tickDelta);

        UUID playerUuid =
                player.getUUID();

        boolean rebuilding =
                TeleportEntityEffectManager
                        .isRebuilding(playerUuid);

        boolean rebuildMeshStarted =
                rebuilding
                        && TeleportEntityEffectManager
                        .hasRebuildMeshStarted(
                                playerUuid,
                                tickDelta
                        );

        STATES.put(
                playerUuid,
                new PreparedState(
                        TeleportEntityEffectManager
                                .getDissolveProgress(
                                        playerUuid,
                                        tickDelta
                                ),
                        TeleportRenderedHeightManager
                                .getEffectHeight(player)
                                + 0.05F,
                        (player.tickCount + tickDelta)
                                / 20.0F,
                        playerPosition.subtract(
                                cameraPosition
                        ),
                        rebuilding ? 1.0F : 0.0F,
                        rebuildMeshStarted
                                ? 1.0F
                                : 0.0F
                )
        );
    }

    public static void uploadUniforms(UUID playerUuid) {
        ShaderInstance program =
                TeleportDissolveShaders.getProgram();

        if (program == null) {
            return;
        }

        PreparedState state = STATES.get(playerUuid);

        if (state == null) {
            state = new PreparedState(
                    0.0F,
                    1.8F,
                    0.0F,
                    Vec3.ZERO,
                    0.0F,
                    0.0F
            );
        }

        setFloat(program, "DissolveProgress", state.progress());

        setVec3(
                program,
                "EntityOrigin",
                (float) state.origin().x,
                (float) state.origin().y,
                (float) state.origin().z
        );

        setFloat(program, "EntityHeight", state.height());
        setFloat(program, "EffectTime", state.effectTime());
        setFloat(program, "NoiseScale", NOISE_SCALE);
        setFloat(program, "NoiseStrength", NOISE_STRENGTH);
        setFloat(program, "EdgeWidth", EDGE_WIDTH);
        setFloat(
                program,
                "BlackCoreWidth",
                BLACK_CORE_WIDTH
        );

        setFloat(
                program,
                "Rebuilding",
                state.rebuilding()
        );

        setFloat(
                program,
                "RebuildBlockNoiseScale",
                REBUILD_BLOCK_NOISE_SCALE
        );

        setFloat(
                program,
                "RebuildBlockBandWidth",
                REBUILD_BLOCK_BAND_WIDTH
        );

        setFloat(
                program,
                "RebuildBlockStrength",
                REBUILD_BLOCK_STRENGTH
        );

        setFloat(
                program,
                "RebuildBlockSpeed",
                REBUILD_BLOCK_SPEED
        );

        setFloat(
                program,
                "RebuildMeshStarted",
                state.rebuildMeshStarted()
        );
    }

    
    
    public static FallbackSample sampleShaderPackFallback(
            UUID playerUuid,
            float vertexX,
            float vertexY,
            float vertexZ
    ) {
        PreparedState state =
                STATES.get(playerUuid);

        if (state == null) {
            return FallbackSample.FULLY_VISIBLE;
        }

        if (state.rebuilding() > 0.5F
                && state.rebuildMeshStarted() < 0.5F) {
            return FallbackSample.HIDDEN;
        }

        float relativeX =
                vertexX
                        - (float) state.origin().x;

        float relativeY =
                vertexY
                        - (float) state.origin().y;

        float relativeZ =
                vertexZ
                        - (float) state.origin().z;

        float safeHeight =
                Math.max(
                        state.height(),
                        0.01F
                );

        float height01 =
                Mth.clamp(
                        relativeY / safeHeight,
                        0.0F,
                        1.0F
                );

        float threshold =
                Mth.lerp(
                        Mth.clamp(
                                state.progress(),
                                0.0F,
                                1.0F
                        ),
                        1.15F,
                        -0.15F
                );

        float blockNoise =
                fallbackBlockNoise(
                        relativeX,
                        relativeY,
                        relativeZ,
                        state.effectTime()
                );

        float signedBoundaryDistance =
                height01 - threshold;

        float bandMask =
                1.0F
                        - smoothStep(
                        IRIS_BLOCK_BAND_WIDTH * 0.65F,
                        IRIS_BLOCK_BAND_WIDTH,
                        Math.abs(
                                signedBoundaryDistance
                        )
                );

        float direction =
                state.rebuilding() > 0.5F
                        ? 1.0F
                        : -1.0F;

        float finalField =
                height01
                        + (
                        blockNoise * 2.0F
                                - 1.0F
                )
                        * IRIS_BLOCK_STRENGTH
                        * bandMask
                        * direction;

        
        if (finalField > threshold) {
            return FallbackSample.HIDDEN;
        }

        float insideDistance =
                threshold - finalField;

        if (insideDistance <= BLACK_CORE_WIDTH) {
            return FallbackSample.BLACK;
        }

        
        if (bandMask > 0.05F) {
            float steppedShade =
                    blockNoise < 0.34F
                            ? 0.08F
                            : blockNoise < 0.67F
                            ? 0.22F
                            : 0.42F;

            return new FallbackSample(
                    1.0F,
                    steppedShade
            );
        }

        return FallbackSample.FULLY_VISIBLE;
    }

    private static float fallbackBlockNoise(
            float x,
            float y,
            float z,
            float effectTime
    ) {
        int cellX =
                Mth.floor(
                        x * IRIS_BLOCK_NOISE_SCALE
                );

        int cellY =
                Mth.floor(
                        y * IRIS_BLOCK_NOISE_SCALE
                );

        int cellZ =
                Mth.floor(
                        z * IRIS_BLOCK_NOISE_SCALE
                );

        int timeStep =
                Mth.floor(
                        effectTime
                                * REBUILD_BLOCK_SPEED
                );

        long hash =
                cellX * 0x632BE59BD9B4E019L
                        ^ cellY * 0x9E3779B97F4A7C15L
                        ^ cellZ * 0x94D049BB133111EBL
                        ^ timeStep * 0xD6E8FEB86659FD93L;

        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;

        return (
                hash
                        & 0x00FFFFFFL
        ) / 16777215.0F;
    }

    private static float smoothStep(
            float edge0,
            float edge1,
            float value
    ) {
        if (edge1 <= edge0) {
            return value < edge0
                    ? 0.0F
                    : 1.0F;
        }

        float x =
                Mth.clamp(
                        (value - edge0)
                                / (edge1 - edge0),
                        0.0F,
                        1.0F
                );

        return x * x
                * (3.0F - 2.0F * x);
    }

    public static void clear(UUID playerUuid) {
        STATES.remove(playerUuid);
        TeleportRenderedHeightManager.clearEffect(playerUuid);
    }

    private static void setFloat(
            ShaderInstance program,
            String name,
            float value
    ) {
        Uniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setVec3(
            ShaderInstance program,
            String name,
            float x,
            float y,
            float z
    ) {
        Uniform uniform = program.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    public record FallbackSample(
            float alpha,
            float colorMultiplier
    ) {
        public static final FallbackSample FULLY_VISIBLE =
                new FallbackSample(
                        1.0F,
                        1.0F
                );

        public static final FallbackSample HIDDEN =
                new FallbackSample(
                        0.0F,
                        1.0F
                );

        public static final FallbackSample BLACK =
                new FallbackSample(
                        1.0F,
                        0.0F
                );
    }

    private record PreparedState(
            float progress,
            float height,
            float effectTime,
            Vec3 origin,
            float rebuilding,
            float rebuildMeshStarted
    ) {
    }
}
