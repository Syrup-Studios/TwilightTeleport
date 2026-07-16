package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.UUID;


public final class TeleportHeightMeasuringVertexConsumerProvider
        implements VertexConsumerProvider {

    private final VertexConsumerProvider delegate;
    private final UUID playerUuid;

    public TeleportHeightMeasuringVertexConsumerProvider(
            VertexConsumerProvider delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return wrap(
                delegate.getBuffer(layer),
                playerUuid
        );
    }

    
    public static VertexConsumer wrap(
            VertexConsumer delegate,
            UUID playerUuid
    ) {
        return new MeasuringVertexConsumer(
                delegate,
                playerUuid
        );
    }

    private static final class MeasuringVertexConsumer
            implements VertexConsumer {

        private final VertexConsumer delegate;
        private final UUID playerUuid;

        private final float[] quadX = new float[4];
        private final float[] quadY = new float[4];
        private final float[] quadZ = new float[4];
        private int quadVertexCount;

        private MeasuringVertexConsumer(
                VertexConsumer delegate,
                UUID playerUuid
        ) {
            this.delegate = delegate;
            this.playerUuid = playerUuid;
        }

        @Override
        public VertexConsumer vertex(
                float x,
                float y,
                float z
        ) {
            recordPosition(x, y, z);
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public void vertex(
                float x,
                float y,
                float z,
                int color,
                float u,
                float v,
                int overlay,
                int light,
                float normalX,
                float normalY,
                float normalZ
        ) {
            recordPosition(x, y, z);

            delegate.vertex(
                    x,
                    y,
                    z,
                    color,
                    u,
                    v,
                    overlay,
                    light,
                    normalX,
                    normalY,
                    normalZ
            );
        }

        private void recordPosition(
                float x,
                float y,
                float z
        ) {
            TeleportRenderedHeightManager.recordVertex(
                    playerUuid,
                    x,
                    y,
                    z
            );

            quadX[quadVertexCount] = x;
            quadY[quadVertexCount] = y;
            quadZ[quadVertexCount] = z;
            quadVertexCount++;

            if (quadVertexCount < 4) {
                return;
            }

            
            for (int uStep = 1; uStep <= 3; uStep++) {
                float u = uStep / 4.0F;

                float topX = lerp(quadX[0], quadX[1], u);
                float topY = lerp(quadY[0], quadY[1], u);
                float topZ = lerp(quadZ[0], quadZ[1], u);

                float bottomX = lerp(quadX[3], quadX[2], u);
                float bottomY = lerp(quadY[3], quadY[2], u);
                float bottomZ = lerp(quadZ[3], quadZ[2], u);

                for (int vStep = 1; vStep <= 3; vStep++) {
                    float v = vStep / 4.0F;

                    TeleportRenderedHeightManager.recordVertex(
                            playerUuid,
                            lerp(topX, bottomX, v),
                            lerp(topY, bottomY, v),
                            lerp(topZ, bottomZ, v)
                    );
                }
            }

            quadVertexCount = 0;
        }

        private static float lerp(
                float start,
                float end,
                float progress
        ) {
            return start + (end - start) * progress;
        }

        @Override
        public VertexConsumer color(
                int red,
                int green,
                int blue,
                int alpha
        ) {
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(
                float u,
                float v
        ) {
            delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(
                int u,
                int v
        ) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(
                int u,
                int v
        ) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(
                float x,
                float y,
                float z
        ) {
            delegate.normal(x, y, z);
            return this;
        }
    }
}
