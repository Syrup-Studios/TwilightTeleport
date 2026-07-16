package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;

import java.util.UUID;


public final class TeleportDissolveVertexConsumerProvider
        implements VertexConsumerProvider {

    private static final VertexConsumer DISCARDING_CONSUMER =
            new DiscardingVertexConsumer();

    private final VertexConsumerProvider delegate;
    private final UUID playerUuid;

    public TeleportDissolveVertexConsumerProvider(
            VertexConsumerProvider delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        if (!layer.getVertexFormat().equals(
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
        )) {
            return DISCARDING_CONSUMER;
        }

        return delegate.getBuffer(
                TeleportDissolveRenderLayer.wrap(
                        playerUuid,
                        layer
                )
        );
    }

    private static final class DiscardingVertexConsumer
            implements VertexConsumer {

        @Override
        public VertexConsumer vertex(
                float x,
                float y,
                float z
        ) {
            return this;
        }

        @Override
        public VertexConsumer color(
                int red,
                int green,
                int blue,
                int alpha
        ) {
            return this;
        }

        @Override
        public VertexConsumer texture(
                float u,
                float v
        ) {
            return this;
        }

        @Override
        public VertexConsumer overlay(
                int u,
                int v
        ) {
            return this;
        }

        @Override
        public VertexConsumer light(
                int u,
                int v
        ) {
            return this;
        }

        @Override
        public VertexConsumer normal(
                float x,
                float y,
                float z
        ) {
            return this;
        }
    }
}
