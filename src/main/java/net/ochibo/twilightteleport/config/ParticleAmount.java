package net.ochibo.twilightteleport.config;

import net.minecraft.network.chat.Component;

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

    public Component displayText() {
        return Component.translatable(
                "config.twilightteleport.particle_amount." +
                        name().toLowerCase(java.util.Locale.ROOT)
        );
    }
}
