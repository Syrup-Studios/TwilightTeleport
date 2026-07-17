package net.ochibo.twilightteleport.client.render;

//? if <1.20.5 {
/*import com.mojang.blaze3d.systems.RenderSystem;
*///?}
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//? if <1.20.5 {
/*import org.joml.Matrix3f;
import org.joml.Vector3f;
*///?}


public final class TeleportHeightMeasuringVertexConsumerProvider
        implements MultiBufferSource {

    private final MultiBufferSource delegate;
    private final UUID playerUuid;

    public TeleportHeightMeasuringVertexConsumerProvider(
            MultiBufferSource delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
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

        //? if <1.20.5 {
        /*private final Matrix3f positionTransform;
        *///?}

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

            //? if <1.20.5 {
            /*this.positionTransform = new Matrix3f(
                    RenderSystem.getInverseViewRotationMatrix()
            );
            *///?}
        }

        //? if >=1.20.5 {
        @Override
        public VertexConsumer addVertex(
                float x,
                float y,
                float z
        ) {
            recordPosition(x, y, z);
            delegate.addVertex(x, y, z);
            return this;
        }
        //?} else {
        /*@Override
        public VertexConsumer vertex(
                double x,
                double y,
                double z
        ) {
            recordPosition((float) x, (float) y, (float) z);
            delegate.vertex(x, y, z);
            return this;
        }
        *///?}

        //? if >=1.20.5 {
        @Override
        public void addVertex(
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

            delegate.addVertex(
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
        //?}

        private void recordPosition(
                float x,
                float y,
                float z
        ) {
            //? if <1.20.5 {
            /*Vector3f correctedPosition =
                    positionTransform.transform(
                            new Vector3f(x, y, z)
                    );

            x = correctedPosition.x();
            y = correctedPosition.y();
            z = correctedPosition.z();
            *///?}

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

        //? if >=1.20.5 {
        @Override
        public VertexConsumer setColor(
                int red,
                int green,
                int blue,
                int alpha
        ) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(
                float u,
                float v
        ) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(
                int u,
                int v
        ) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(
                int u,
                int v
        ) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(
                float x,
                float y,
                float z
        ) {
            delegate.setNormal(x, y, z);
            return this;
        }
        //?} else {
        /*@Override
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
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
        *///?}
    }
}
