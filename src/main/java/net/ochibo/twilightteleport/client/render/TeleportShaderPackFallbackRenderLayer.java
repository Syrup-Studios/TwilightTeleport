package net.ochibo.twilightteleport.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;


public final class TeleportShaderPackFallbackRenderLayer
        extends RenderType {

    private static final Map<RenderType, RenderType> CACHE =
            new ConcurrentHashMap<>();

    private TeleportShaderPackFallbackRenderLayer(
            String name,
            RenderType original,
            LayerActions actions
    ) {
        super(
                name,
                original.format(),
                original.mode(),
                original.bufferSize(),
                original.affectsCrumbling(),
                true,
                actions.startAction(),
                actions.endAction()
        );
    }

    public static RenderType wrap(RenderType original) {
        if (original
                instanceof TeleportShaderPackFallbackRenderLayer) {
            return original;
        }

        return CACHE.computeIfAbsent(
                original,
                TeleportShaderPackFallbackRenderLayer::create
        );
    }

    public static void clear() {
        CACHE.clear();
    }

    private static RenderType create(RenderType original) {
        LayerActions actions =
                createActions(original);

        return new TeleportShaderPackFallbackRenderLayer(
                "twilightteleport_shaderpack_fallback_"
                        + Integer.toHexString(
                        System.identityHashCode(original)
                ),
                original,
                actions
        );
    }

    private static LayerActions createActions(
            RenderType original
    ) {
        Runnable startAction = () -> {
            
            original.setupRenderState();

            
            RenderStateShard.TRANSLUCENT_TRANSPARENCY
                    .setupRenderState();

            
            RenderStateShard.COLOR_WRITE.setupRenderState();
        };

        Runnable endAction = () -> {
            RenderStateShard.COLOR_WRITE.clearRenderState();

            RenderStateShard.TRANSLUCENT_TRANSPARENCY
                    .clearRenderState();

            original.clearRenderState();
        };

        return new LayerActions(
                startAction,
                endAction
        );
    }

    private record LayerActions(
            Runnable startAction,
            Runnable endAction
    ) {
    }
}
