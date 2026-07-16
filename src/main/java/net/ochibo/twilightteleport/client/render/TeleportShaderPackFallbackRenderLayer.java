package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class TeleportShaderPackFallbackRenderLayer
        extends RenderLayer {

    private static final Map<RenderLayer, RenderLayer> CACHE =
            new ConcurrentHashMap<>();

    private TeleportShaderPackFallbackRenderLayer(
            String name,
            RenderLayer original,
            LayerActions actions
    ) {
        super(
                name,
                original.getVertexFormat(),
                original.getDrawMode(),
                original.getExpectedBufferSize(),
                original.hasCrumbling(),
                true,
                actions.startAction(),
                actions.endAction()
        );
    }

    public static RenderLayer wrap(RenderLayer original) {
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

    private static RenderLayer create(RenderLayer original) {
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
            RenderLayer original
    ) {
        Runnable startAction = () -> {
            
            original.startDrawing();

            
            RenderPhase.TRANSLUCENT_TRANSPARENCY
                    .startDrawing();

            
            RenderPhase.COLOR_MASK.startDrawing();
        };

        Runnable endAction = () -> {
            RenderPhase.COLOR_MASK.endDrawing();

            RenderPhase.TRANSLUCENT_TRANSPARENCY
                    .endDrawing();

            original.endDrawing();
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
