package net.ochibo.twilightteleport.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.ochibo.twilightteleport.mixin.client.RenderTypeAccessor;


public final class TeleportDissolveRenderLayer
        extends RenderType {

    private static final Map<SkinLayerKey, RenderType> SKIN_CACHE =
            new ConcurrentHashMap<>();

    private static final Map<WrappedLayerKey, RenderType> WRAPPED_CACHE =
            new ConcurrentHashMap<>();

    private TeleportDissolveRenderLayer(
            String name,
            VertexFormat vertexFormat,
            VertexFormat.Mode drawMode,
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

    
    public static RenderType get(
            UUID playerUuid,
            ResourceLocation texture
    ) {
        SkinLayerKey key =
                new SkinLayerKey(playerUuid, texture);

        return SKIN_CACHE.computeIfAbsent(
                key,
                TeleportDissolveRenderLayer::createSkinLayer
        );
    }

    
    public static RenderType wrap(
            UUID playerUuid,
            RenderType originalLayer
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

    private static RenderType createSkinLayer(
            SkinLayerKey key
    ) {
        LayerActions actions =
                createSkinActions(key);

        return new TeleportDissolveRenderLayer(
                "twilightteleport_dissolve_skin_"
                        + key.playerUuid()
                        + "_"
                        + key.texture(),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                TRANSIENT_BUFFER_SIZE,
                false,
                false,
                actions
        );
    }

    private static RenderType createWrappedLayer(
            WrappedLayerKey key
    ) {
        RenderType original = key.originalLayer();

        LayerActions actions =
                createWrappedActions(key);

        return new TeleportDissolveRenderLayer(
                "twilightteleport_dissolve_feature_"
                        + key.playerUuid()
                        + "_"
                        + Integer.toHexString(
                        System.identityHashCode(original)
                ),
                original.format(),
                original.mode(),
                original.bufferSize(),
                original.affectsCrumbling(),
                //? if >=1.20.5 {
                original.sortOnUpload(),
                //?} else {
                /*((RenderTypeAccessor) (Object) original)
                        .twilightTeleport$sortOnUpload(),
                *///?}
                actions
        );
    }

    private static LayerActions createSkinActions(
            SkinLayerKey key
    ) {
        TextureStateShard texturePhase =
                new TextureStateShard(key.texture(), false, false);

        ShaderStateShard shaderPhase =
                new ShaderStateShard(
                        TeleportDissolveShaders::getProgram
                );

        TexturingStateShard uniformPhase =
                createUniformPhase(key.playerUuid());

        RenderStateShard[] phases = {
                texturePhase,
                shaderPhase,
                NO_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                NO_CULL,
                LIGHTMAP,
                OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                uniformPhase,
                COLOR_DEPTH_WRITE,
                NO_COLOR_LOGIC
        };

        Runnable startAction = () -> {
            for (RenderStateShard phase : phases) {
                phase.setupRenderState();
            }
        };

        Runnable endAction = () -> {
            for (int i = phases.length - 1; i >= 0; i--) {
                phases[i].clearRenderState();
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
        RenderType original = key.originalLayer();

        ShaderStateShard shaderPhase =
                new ShaderStateShard(
                        TeleportDissolveShaders::getProgram
                );

        TexturingStateShard uniformPhase =
                createUniformPhase(key.playerUuid());

        Runnable startAction = () -> {
            
            original.setupRenderState();

            
            shaderPhase.setupRenderState();
            uniformPhase.setupRenderState();
        };

        Runnable endAction = () -> {
            uniformPhase.clearRenderState();
            shaderPhase.clearRenderState();
            original.clearRenderState();
        };

        return new LayerActions(
                startAction,
                endAction
        );
    }

    private static TexturingStateShard createUniformPhase(
            UUID playerUuid
    ) {
        return new TexturingStateShard(
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
            ResourceLocation texture
    ) {
    }

    
    private record WrappedLayerKey(
            UUID playerUuid,
            RenderType originalLayer
    ) {
    }

    private record LayerActions(
            Runnable startAction,
            Runnable endAction
    ) {
    }
}
