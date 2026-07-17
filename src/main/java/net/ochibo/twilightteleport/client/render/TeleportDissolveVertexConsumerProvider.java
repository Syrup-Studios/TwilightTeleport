package net.ochibo.twilightteleport.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;


public final class TeleportDissolveVertexConsumerProvider
        implements MultiBufferSource {

    private static final VertexConsumer DISCARDING_CONSUMER =
            TeleportDiscardingVertexConsumer.INSTANCE;

    private final MultiBufferSource delegate;
    private final UUID playerUuid;

    public TeleportDissolveVertexConsumerProvider(
            MultiBufferSource delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        if (!layer.format().equals(
                DefaultVertexFormat.NEW_ENTITY
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

}
