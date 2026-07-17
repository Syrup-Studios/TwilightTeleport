package net.ochibo.twilightteleport.client.render;

//? if <1.20.5 {
/*import com.mojang.blaze3d.systems.RenderSystem;
*///?}
import net.minecraft.util.Mth;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
//? if <1.20.5 {
/*import org.joml.Matrix3f;
import org.joml.Vector3f;
*///?}


public final class TeleportShaderPackFallbackVertexConsumer
        implements VertexConsumer {

    private final VertexConsumer delegate;
    private final UUID playerUuid;
    private final int quadSubdivisions;

    //? if <1.20.5 {
    /*private final Matrix3f positionTransform;
    private final Vector3f correctedSamplePosition =
            new Vector3f();
    *///?}

    private final List<VertexData> quadVertices =
            new ArrayList<>(4);

    private MutableVertex pendingVertex;
    private int defaultColor = 0xFFFFFFFF;
    private boolean defaultColorSet;

    public TeleportShaderPackFallbackVertexConsumer(
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

        this.quadSubdivisions = Math.max(
                1,
                TwilightTeleportConfigManager
                        .get()
                        .getShaderPackEffectQuality()
                        .subdivisions()
        );
    }

    //? if >=1.20.5 {
    @Override
    public VertexConsumer addVertex(
            float x,
            float y,
            float z
    ) {
        finishPendingVertex();

        pendingVertex =
                createPendingVertex(x, y, z);

        return this;
    }
    //?} else {
    /*@Override
    public VertexConsumer vertex(
            double x,
            double y,
            double z
    ) {
        finishPendingVertex();
        pendingVertex = createPendingVertex(
                (float) x,
                (float) y,
                (float) z
        );
        return this;
    }
    *///?}

    //? if >=1.20.5 {
    @Override
    public VertexConsumer setColor(
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
    public VertexConsumer setUv(
            float u,
            float v
    ) {
        ensurePendingVertex();

        pendingVertex.u = u;
        pendingVertex.v = v;

        return this;
    }

    @Override
    public VertexConsumer setUv1(
            int u,
            int v
    ) {
        ensurePendingVertex();

        pendingVertex.overlay =
                u | v << 16;

        return this;
    }

    @Override
    public VertexConsumer setUv2(
            int u,
            int v
    ) {
        ensurePendingVertex();

        pendingVertex.light =
                u | v << 16;

        return this;
    }

    @Override
    public VertexConsumer setNormal(
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
    //?} else {
    /*@Override
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
    public VertexConsumer uv(float u, float v) {
        ensurePendingVertex();
        pendingVertex.u = u;
        pendingVertex.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        ensurePendingVertex();
        pendingVertex.overlay = u | v << 16;
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        ensurePendingVertex();
        pendingVertex.light = u | v << 16;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        ensurePendingVertex();
        pendingVertex.normalX = x;
        pendingVertex.normalY = y;
        pendingVertex.normalZ = z;
        return this;
    }

    @Override
    public void endVertex() {
        finishPendingVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        defaultColor =
                alpha << 24
                        | red << 16
                        | green << 8
                        | blue;
        defaultColorSet = true;
    }

    @Override
    public void unsetDefaultColor() {
        defaultColorSet = false;
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
    //?}

    private MutableVertex createPendingVertex(
            float x,
            float y,
            float z
    ) {
        MutableVertex vertex = new MutableVertex(x, y, z);

        if (defaultColorSet) {
            vertex.color = defaultColor;
        }

        return vertex;
    }

    private void ensurePendingVertex() {
        if (pendingVertex == null) {
            pendingVertex =
                    createPendingVertex(
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
                        sample(center);

                
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

    private TeleportDissolveRenderState.FallbackSample sample(
            VertexData position
    ) {
        float x = position.x();
        float y = position.y();
        float z = position.z();

        //? if <1.20.5 {
        /*correctedSamplePosition.set(x, y, z);
        positionTransform.transform(correctedSamplePosition);

        x = correctedSamplePosition.x();
        y = correctedSamplePosition.y();
        z = correctedSamplePosition.z();
        *///?}

        return TeleportDissolveRenderState
                .sampleShaderPackFallback(
                        playerUuid,
                        x,
                        y,
                        z
                );
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

        //? if >=1.20.5 {
        delegate.addVertex(
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
        //?} else {
        /*delegate.vertex(
                        vertex.x(),
                        vertex.y(),
                        vertex.z()
                )
                .color(modifiedColor)
                .uv(vertex.u(), vertex.v())
                .overlayCoords(vertex.overlay())
                .uv2(vertex.light())
                .normal(
                        vertex.normalX(),
                        vertex.normalY(),
                        vertex.normalZ()
                )
                .endVertex();
        *///?}
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
                Mth.lerp(progress, start.x(), end.x()),
                Mth.lerp(progress, start.y(), end.y()),
                Mth.lerp(progress, start.z(), end.z()),
                lerpColor(start.color(), end.color(), progress),
                Mth.lerp(progress, start.u(), end.u()),
                Mth.lerp(progress, start.v(), end.v()),
                progress < 0.5F
                        ? start.overlay()
                        : end.overlay(),
                progress < 0.5F
                        ? start.light()
                        : end.light(),
                Mth.lerp(
                        progress,
                        start.normalX(),
                        end.normalX()
                ),
                Mth.lerp(
                        progress,
                        start.normalY(),
                        end.normalY()
                ),
                Mth.lerp(
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
                        Mth.lerpInt(
                                progress,
                                start >>> 24,
                                end >>> 24
                        )
                );

        int red =
                Math.round(
                        Mth.lerpInt(
                                progress,
                                start >>> 16 & 0xFF,
                                end >>> 16 & 0xFF
                        )
                );

        int green =
                Math.round(
                        Mth.lerpInt(
                                progress,
                                start >>> 8 & 0xFF,
                                end >>> 8 & 0xFF
                        )
                );

        int blue =
                Math.round(
                        Mth.lerpInt(
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
        return Mth.clamp(
                Math.round(
                        component
                                * Mth.clamp(
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
