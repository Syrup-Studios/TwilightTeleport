#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float DissolveProgress;
uniform float EntityHeight;
uniform float EffectTime;
uniform float NoiseScale;
uniform float NoiseStrength;
uniform float EdgeWidth;

/*
 * 境界の表示側で、必ず完全な黒になる幅。
 */
uniform float BlackCoreWidth;

/*
 * 1.0なら再構築中、0.0なら消滅中。
 * ブロックノイズの向きを切り替えるために使用する。
 */
uniform float Rebuilding;

/*
 * 消滅・再構築の両方で使用する
 * 境界ブロックノイズ設定。
 */
uniform float RebuildBlockNoiseScale;
uniform float RebuildBlockBandWidth;
uniform float RebuildBlockStrength;
uniform float RebuildBlockSpeed;
uniform float RebuildMeshStarted;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 playerRelativePosition;

out vec4 fragColor;

float hash31(vec3 position) {
    position = fract(
        position * 0.1031
    );

    position += dot(
        position,
        position.yzx + 33.33
    );

    return fract(
        (position.x + position.y)
        * position.z
    );
}

float valueNoise(vec3 position) {
    vec3 cell = floor(position);
    vec3 local = fract(position);

    local =
        local * local
        * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, local.x);
    float nx10 = mix(n010, n110, local.x);
    float nx01 = mix(n001, n101, local.x);
    float nx11 = mix(n011, n111, local.x);

    float nxy0 = mix(nx00, nx10, local.y);
    float nxy1 = mix(nx01, nx11, local.y);

    return mix(nxy0, nxy1, local.z);
}

float getRebuildBlockNoise() {
    /*
     * 補間前のローカル座標を立方体セルへ量子化する。
     * 同じセル内のフラグメントは同じ値になるため、
     * 滑らかな砂嵐ではなくブロック状の欠け方になる。
     */
    vec3 blockCell = floor(
        playerRelativePosition
        * max(RebuildBlockNoiseScale, 0.01)
    );

    /*
     * 時間も整数段階へ量子化し、
     * セル単位でパターンを切り替える。
     */
    float timeStep = floor(
        EffectTime
        * max(RebuildBlockSpeed, 0.0)
    );

    return hash31(
        blockCell
        + vec3(
            timeStep * 0.73,
            timeStep * 1.17,
            timeStep * 0.41
        )
    );
}

void main() {
    vec4 color =
        texture(Sampler0, texCoord0);

    if (color.a < 0.1) {
        discard;
    }

    /*
     * パーティクルは先に開始するが、
     * REBUILD_MESH_DELAY_TICKSが終了するまでは
     * メッシュを完全に非表示にする。
     */
    if (Rebuilding > 0.5
            && RebuildMeshStarted < 0.5) {
        discard;
    }

    float safeHeight =
        max(EntityHeight, 0.01);

    float height01 = clamp(
        playerRelativePosition.y
        / safeHeight,
        0.0,
        1.0
    );

    /*
     * 消滅側で使用する滑らかなノイズ。
     */
    vec3 noisePosition =
        playerRelativePosition * NoiseScale
        + vec3(
            0.0,
            EffectTime * 0.18,
            EffectTime * 0.07
        );

    float noiseValue =
        valueNoise(noisePosition);

    float dissolveField =
        height01
        + (noiseValue - 0.5)
        * NoiseStrength;

    /*
     * Progress 0では頭頂部より上。
     * Progress 1では足元より下。
     *
     * 再構築ではProgressが1から0へ戻るため、
     * 境界が足元から頭頂部へ上昇する。
     */
    float threshold = mix(
        1.15,
        -0.15,
        clamp(DissolveProgress, 0.0, 1.0)
    );

    float finalField = dissolveField;

    float signedBoundaryDistance =
        dissolveField - threshold;

    /*
     * 境界面の前後だけを対象にする。
     * 消滅側・再構築側のどちらでも使用する。
     */
    float blockBandMask =
        1.0
        - smoothstep(
            RebuildBlockBandWidth * 0.72,
            RebuildBlockBandWidth,
            abs(signedBoundaryDistance)
        );

    float blockNoise =
        getRebuildBlockNoise();

    /*
     * セルごとに境界を前後へずらす。
     *
     * 再構築:
     *   正のセルは先に出現し、
     *   負のセルは少し遅れて出現する。
     *
     * 消滅:
     *   向きを反転させることで、
     *   正のセルは先に欠け、
     *   負のセルは少し残留する。
     */
    float direction =
        Rebuilding > 0.5
        ? 1.0
        : -1.0;

    float signedBlockOffset =
        (blockNoise * 2.0 - 1.0)
        * RebuildBlockStrength
        * blockBandMask
        * direction;

    finalField += signedBlockOffset;

    /*
     * 境界より上側を破棄する。
     */
    if (finalField > threshold) {
        discard;
    }

    color *=
        vertexColor
        * ColorModulator;

    color.rgb = mix(
        overlayColor.rgb,
        color.rgb,
        overlayColor.a
    );

    color *= lightMapColor;

    /*
     * 表示されている側の境界付近を暗くする。
     */
    float insideDistance =
        threshold - finalField;

    float resolvedBlackCoreWidth =
        max(BlackCoreWidth, 0.0);

    float resolvedEdgeEnd =
        max(
            EdgeWidth,
            resolvedBlackCoreWidth + 0.0001
        );

    /*
     * 完全黒帯の外側から通常色へ滑らかに戻す。
     */
    float blackEdge =
        1.0
        - smoothstep(
            resolvedBlackCoreWidth,
            resolvedEdgeEnd,
            insideDistance
        );

    float blackCore =
        1.0
        - step(
            resolvedBlackCoreWidth,
            insideDistance
        );

    /*
     * 境界ブロックに暗い色を付ける。
     * 再構築側は紫黒、消滅側は完全な黒寄り。
     */
    float blockAccent =
        blockBandMask
        * (
            0.38
            + blockNoise * 0.62
        );

    vec3 blockColor =  vec3(
            0.0,
            0.0,
            0.0
        );

    color.rgb = mix(
        color.rgb,
        blockColor,
        clamp(
            max(
                blackEdge,
                blockAccent * 0.78
            ),
            0.0,
            1.0
        )
    );

    /*
     * ブロックノイズやライティングより後に適用し、
     * 指定幅を確実に完全な黒へ固定する。
     */
    color.rgb = mix(
        color.rgb,
        vec3(0.0),
        blackCore
    );

    fragColor = linear_fog(
        color,
        vertexDistance,
        FogStart,
        FogEnd,
        FogColor
    );
}
