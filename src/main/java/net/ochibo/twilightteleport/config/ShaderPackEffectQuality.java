package net.ochibo.twilightteleport.config;

import net.minecraft.text.Text;


public enum ShaderPackEffectQuality {
    LOW(3, "config.twilightteleport.shader_pack_quality.low"),
    MEDIUM(5, "config.twilightteleport.shader_pack_quality.medium"),
    HIGH(8, "config.twilightteleport.shader_pack_quality.high");

    private final int subdivisions;
    private final String translationKey;

    ShaderPackEffectQuality(
            int subdivisions,
            String translationKey
    ) {
        this.subdivisions = subdivisions;
        this.translationKey = translationKey;
    }

    public int subdivisions() {
        return subdivisions;
    }

    public Text displayText() {
        return Text.translatable(translationKey);
    }

    public ShaderPackEffectQuality next() {
        ShaderPackEffectQuality[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
