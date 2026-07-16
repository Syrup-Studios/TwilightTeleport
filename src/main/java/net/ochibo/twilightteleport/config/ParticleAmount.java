package net.ochibo.twilightteleport.config;

import net.minecraft.text.Text;

public enum ParticleAmount {
    NONE(0),
    FEW(3),
    DEFAULT(8);

    private final int particlesPerTick;

    ParticleAmount(int particlesPerTick) {
        this.particlesPerTick = particlesPerTick;
    }

    public int particlesPerTick() {
        return particlesPerTick;
    }

    public ParticleAmount next() {
        return switch (this) {
            case NONE -> FEW;
            case FEW -> DEFAULT;
            case DEFAULT -> NONE;
        };
    }

    public Text displayText() {
        return Text.translatable(
                "config.twilightteleport.particle_amount." +
                        name().toLowerCase(java.util.Locale.ROOT)
        );
    }
}
