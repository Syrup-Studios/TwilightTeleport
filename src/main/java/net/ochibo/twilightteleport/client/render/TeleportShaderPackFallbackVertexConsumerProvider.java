package net.ochibo.twilightteleport.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;


public final class TeleportShaderPackFallbackVertexConsumerProvider
        implements MultiBufferSource {

    private static final VertexConsumer DISCARDING_CONSUMER =
            new DiscardingVertexConsumer();

    private final MultiBufferSource delegate;
    private final UUID playerUuid;

    public TeleportShaderPackFallbackVertexConsumerProvider(
            MultiBufferSource delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        
        if (!layer.format().equals(
                DefaultVertexFormat
                        .NEW_ENTITY
        )) {
            return DISCARDING_CONSUMER;
        }

        
        return new TeleportShaderPackFallbackVertexConsumer(
                delegate.getBuffer(layer),
                playerUuid
        );
    }

    private static final class DiscardingVertexConsumer
            implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(
                float x,
                float y,
                float z
        ) {
            return this;
        }

        @Override
        public VertexConsumer setColor(
                int red,
                int green,
                int blue,
                int alpha
        ) {
            return this;
        }

        @Override
        public VertexConsumer setUv(
                float u,
                float v
        ) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(
                int u,
                int v
        ) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(
                int u,
                int v
        ) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(
                float x,
                float y,
                float z
        ) {
            return this;
        }
    }
}
