package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public final class TeleportShaderPackFallbackVertexConsumer
        implements VertexConsumer {

    private final VertexConsumer delegate;
    private final UUID playerUuid;
    private final int quadSubdivisions;

    private final List<VertexData> quadVertices =
            new ArrayList<>(4);

    private MutableVertex pendingVertex;

    public TeleportShaderPackFallbackVertexConsumer(
            VertexConsumer delegate,
            UUID playerUuid
    ) {
        this.delegate = delegate;
        this.playerUuid = playerUuid;
        this.quadSubdivisions = Math.max(
                1,
                TwilightTeleportConfigManager
                        .get()
                        .getShaderPackEffectQuality()
                        .subdivisions()
        );
    }

    @Override
    public VertexConsumer vertex(
            float x,
            float y,
            float z
    ) {
        finishPendingVertex();

        pendingVertex =
                new MutableVertex(
                        x,
                        y,
                        z
                );

        return this;
    }

    @Override
    public VertexConsumer color(
            int red,
            int green,
            int blue,
            int alpha
    ) {
        ensurePendingVertex();

        pendingVertex.color =
                alpha << 24
                        | red << 16
                        | green << 8
                        | blue;

        return this;
    }

    @Override
    public VertexConsumer texture(
            float u,
            float v
    ) {
        ensurePendingVertex();

        pendingVertex.u = u;
        pendingVertex.v = v;

        return this;
    }

    @Override
    public VertexConsumer overlay(
            int u,
            int v
    ) {
        ensurePendingVertex();

        pendingVertex.overlay =
                u | v << 16;

        return this;
    }

    @Override
    public VertexConsumer light(
            int u,
            int v
    ) {
        ensurePendingVertex();

        pendingVertex.light =
                u | v << 16;

        return this;
    }

    @Override
    public VertexConsumer normal(
            float x,
            float y,
            float z
    ) {
        ensurePendingVertex();

        pendingVertex.normalX = x;
        pendingVertex.normalY = y;
        pendingVertex.normalZ = z;

        finishPendingVertex();
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
        finishPendingVertex();

        acceptVertex(
                new VertexData(
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
                )
        );
    }

    private void ensurePendingVertex() {
        if (pendingVertex == null) {
            pendingVertex =
                    new MutableVertex(
                            0.0F,
                            0.0F,
                            0.0F
                    );
        }
    }

    private void finishPendingVertex() {
        if (pendingVertex == null) {
            return;
        }

        acceptVertex(
                pendingVertex.toImmutable()
        );

        pendingVertex = null;
    }

    private void acceptVertex(VertexData vertex) {
        quadVertices.add(vertex);

        if (quadVertices.size() < 4) {
            return;
        }

        emitSubdividedQuad(
                quadVertices.get(0),
                quadVertices.get(1),
                quadVertices.get(2),
                quadVertices.get(3)
        );

        quadVertices.clear();
    }

    private void emitSubdividedQuad(
            VertexData v00,
            VertexData v10,
            VertexData v11,
            VertexData v01
    ) {
        for (int y = 0;
             y < quadSubdivisions;
             y++) {

            float y0 =
                    y / (float) quadSubdivisions;

            float y1 =
                    (y + 1)
                            / (float) quadSubdivisions;

            for (int x = 0;
                 x < quadSubdivisions;
                 x++) {

                float x0 =
                        x / (float) quadSubdivisions;

                float x1 =
                        (x + 1)
                                / (float) quadSubdivisions;

                VertexData cell00 =
                        bilerp(v00, v10, v11, v01, x0, y0);

                VertexData cell10 =
                        bilerp(v00, v10, v11, v01, x1, y0);

                VertexData cell11 =
                        bilerp(v00, v10, v11, v01, x1, y1);

                VertexData cell01 =
                        bilerp(v00, v10, v11, v01, x0, y1);

                VertexData center =
                        bilerp(
                                v00,
                                v10,
                                v11,
                                v01,
                                (x0 + x1) * 0.5F,
                                (y0 + y1) * 0.5F
                        );

                TeleportDissolveRenderState.FallbackSample sample =
                        TeleportDissolveRenderState
                                .sampleShaderPackFallback(
                                        playerUuid,
                                        center.x(),
                                        center.y(),
                                        center.z()
                                );

                
                if (sample.alpha() <= 0.001F) {
                    continue;
                }

                emit(cell00, sample);
                emit(cell10, sample);
                emit(cell11, sample);
                emit(cell01, sample);
            }
        }
    }

    private void emit(
            VertexData vertex,
            TeleportDissolveRenderState.FallbackSample sample
    ) {
        int sourceColor = vertex.color();

        int alpha =
                sourceColor >>> 24;

        int red =
                sourceColor >>> 16
                        & 0xFF;

        int green =
                sourceColor >>> 8
                        & 0xFF;

        int blue =
                sourceColor
                        & 0xFF;

        int modifiedColor =
                
                alpha << 24
                        | multiplyColor(
                        red,
                        sample.colorMultiplier()
                ) << 16
                        | multiplyColor(
                        green,
                        sample.colorMultiplier()
                ) << 8
                        | multiplyColor(
                        blue,
                        sample.colorMultiplier()
                );

        delegate.vertex(
                vertex.x(),
                vertex.y(),
                vertex.z(),
                modifiedColor,
                vertex.u(),
                vertex.v(),
                vertex.overlay(),
                vertex.light(),
                vertex.normalX(),
                vertex.normalY(),
                vertex.normalZ()
        );
    }

    private static VertexData bilerp(
            VertexData v00,
            VertexData v10,
            VertexData v11,
            VertexData v01,
            float x,
            float y
    ) {
        VertexData top =
                lerp(v00, v10, x);

        VertexData bottom =
                lerp(v01, v11, x);

        return lerp(top, bottom, y);
    }

    private static VertexData lerp(
            VertexData start,
            VertexData end,
            float progress
    ) {
        return new VertexData(
                MathHelper.lerp(progress, start.x(), end.x()),
                MathHelper.lerp(progress, start.y(), end.y()),
                MathHelper.lerp(progress, start.z(), end.z()),
                lerpColor(start.color(), end.color(), progress),
                MathHelper.lerp(progress, start.u(), end.u()),
                MathHelper.lerp(progress, start.v(), end.v()),
                progress < 0.5F
                        ? start.overlay()
                        : end.overlay(),
                progress < 0.5F
                        ? start.light()
                        : end.light(),
                MathHelper.lerp(
                        progress,
                        start.normalX(),
                        end.normalX()
                ),
                MathHelper.lerp(
                        progress,
                        start.normalY(),
                        end.normalY()
                ),
                MathHelper.lerp(
                        progress,
                        start.normalZ(),
                        end.normalZ()
                )
        );
    }

    private static int lerpColor(
            int start,
            int end,
            float progress
    ) {
        int alpha =
                Math.round(
                        MathHelper.lerp(
                                progress,
                                start >>> 24,
                                end >>> 24
                        )
                );

        int red =
                Math.round(
                        MathHelper.lerp(
                                progress,
                                start >>> 16 & 0xFF,
                                end >>> 16 & 0xFF
                        )
                );

        int green =
                Math.round(
                        MathHelper.lerp(
                                progress,
                                start >>> 8 & 0xFF,
                                end >>> 8 & 0xFF
                        )
                );

        int blue =
                Math.round(
                        MathHelper.lerp(
                                progress,
                                start & 0xFF,
                                end & 0xFF
                        )
                );

        return alpha << 24
                | red << 16
                | green << 8
                | blue;
    }

    private static int multiplyColor(
            int component,
            float multiplier
    ) {
        return MathHelper.clamp(
                Math.round(
                        component
                                * MathHelper.clamp(
                                multiplier,
                                0.0F,
                                1.0F
                        )
                ),
                0,
                255
        );
    }

    private record VertexData(
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
    }

    private static final class MutableVertex {
        private final float x;
        private final float y;
        private final float z;

        private int color = 0xFFFFFFFF;
        private float u;
        private float v;
        private int overlay;
        private int light;
        private float normalX;
        private float normalY = 1.0F;
        private float normalZ;

        private MutableVertex(
                float x,
                float y,
                float z
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private VertexData toImmutable() {
            return new VertexData(
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
    }
}
