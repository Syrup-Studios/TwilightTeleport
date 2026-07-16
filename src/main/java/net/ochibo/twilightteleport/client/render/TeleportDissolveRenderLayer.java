package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class TeleportDissolveRenderLayer
        extends RenderLayer {

    private static final Map<SkinLayerKey, RenderLayer> SKIN_CACHE =
            new ConcurrentHashMap<>();

    private static final Map<WrappedLayerKey, RenderLayer> WRAPPED_CACHE =
            new ConcurrentHashMap<>();

    private TeleportDissolveRenderLayer(
            String name,
            VertexFormat vertexFormat,
            VertexFormat.DrawMode drawMode,
            int expectedBufferSize,
            boolean hasCrumbling,
            boolean translucent,
            LayerActions actions
    ) {
        super(
                name,
                vertexFormat,
                drawMode,
                expectedBufferSize,
                hasCrumbling,
                translucent,
                actions.startAction(),
                actions.endAction()
        );
    }

    
    public static RenderLayer get(
            UUID playerUuid,
            Identifier texture
    ) {
        SkinLayerKey key =
                new SkinLayerKey(playerUuid, texture);

        return SKIN_CACHE.computeIfAbsent(
                key,
                TeleportDissolveRenderLayer::createSkinLayer
        );
    }

    
    public static RenderLayer wrap(
            UUID playerUuid,
            RenderLayer originalLayer
    ) {
        if (originalLayer instanceof TeleportDissolveRenderLayer) {
            return originalLayer;
        }

        WrappedLayerKey key =
                new WrappedLayerKey(
                        playerUuid,
                        originalLayer
                );

        return WRAPPED_CACHE.computeIfAbsent(
                key,
                TeleportDissolveRenderLayer::createWrappedLayer
        );
    }

    public static void clear(UUID playerUuid) {
        SKIN_CACHE.keySet().removeIf(
                key -> key.playerUuid().equals(playerUuid)
        );

        WRAPPED_CACHE.keySet().removeIf(
                key -> key.playerUuid().equals(playerUuid)
        );
    }

    private static RenderLayer createSkinLayer(
            SkinLayerKey key
    ) {
        LayerActions actions =
                createSkinActions(key);

        return new TeleportDissolveRenderLayer(
                "twilightteleport_dissolve_skin_"
                        + key.playerUuid()
                        + "_"
                        + key.texture(),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                DEFAULT_BUFFER_SIZE,
                false,
                false,
                actions
        );
    }

    private static RenderLayer createWrappedLayer(
            WrappedLayerKey key
    ) {
        RenderLayer original = key.originalLayer();

        LayerActions actions =
                createWrappedActions(key);

        return new TeleportDissolveRenderLayer(
                "twilightteleport_dissolve_feature_"
                        + key.playerUuid()
                        + "_"
                        + Integer.toHexString(
                        System.identityHashCode(original)
                ),
                original.getVertexFormat(),
                original.getDrawMode(),
                original.getExpectedBufferSize(),
                original.hasCrumbling(),
                original.isTranslucent(),
                actions
        );
    }

    private static LayerActions createSkinActions(
            SkinLayerKey key
    ) {
        Texture texturePhase =
                new Texture(key.texture(), false, false);

        ShaderProgram shaderPhase =
                new ShaderProgram(
                        TeleportDissolveShaders::getProgram
                );

        Texturing uniformPhase =
                createUniformPhase(key.playerUuid());

        RenderPhase[] phases = {
                texturePhase,
                shaderPhase,
                NO_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                DISABLE_CULLING,
                ENABLE_LIGHTMAP,
                ENABLE_OVERLAY_COLOR,
                NO_LAYERING,
                MAIN_TARGET,
                uniformPhase,
                ALL_MASK,
                NO_COLOR_LOGIC
        };

        Runnable startAction = () -> {
            for (RenderPhase phase : phases) {
                phase.startDrawing();
            }
        };

        Runnable endAction = () -> {
            for (int i = phases.length - 1; i >= 0; i--) {
                phases[i].endDrawing();
            }
        };

        return new LayerActions(
                startAction,
                endAction
        );
    }

    private static LayerActions createWrappedActions(
            WrappedLayerKey key
    ) {
        RenderLayer original = key.originalLayer();

        ShaderProgram shaderPhase =
                new ShaderProgram(
                        TeleportDissolveShaders::getProgram
                );

        Texturing uniformPhase =
                createUniformPhase(key.playerUuid());

        Runnable startAction = () -> {
            
            original.startDrawing();

            
            shaderPhase.startDrawing();
            uniformPhase.startDrawing();
        };

        Runnable endAction = () -> {
            uniformPhase.endDrawing();
            shaderPhase.endDrawing();
            original.endDrawing();
        };

        return new LayerActions(
                startAction,
                endAction
        );
    }

    private static Texturing createUniformPhase(
            UUID playerUuid
    ) {
        return new Texturing(
                "twilightteleport_dissolve_uniforms_"
                        + playerUuid,
                () -> TeleportDissolveRenderState
                        .uploadUniforms(playerUuid),
                () -> {
                }
        );
    }

    private record SkinLayerKey(
            UUID playerUuid,
            Identifier texture
    ) {
    }

    
    private record WrappedLayerKey(
            UUID playerUuid,
            RenderLayer originalLayer
    ) {
    }

    private record LayerActions(
            Runnable startAction,
            Runnable endAction
    ) {
    }
}
