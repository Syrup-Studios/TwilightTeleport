package net.ochibo.twilightteleport.config;

public final class TwilightTeleportConfig {

    private ParticleAmount particleAmount = ParticleAmount.DEFAULT;
    private ShaderPackEffectQuality shaderPackEffectQuality =
            ShaderPackEffectQuality.HIGH;

    private boolean soundEnabled = true;
    private boolean letterboxEnabled = true;
    private boolean loadingStatusEnabled = true;

    public TwilightTeleportConfig() {
    }

    public TwilightTeleportConfig(TwilightTeleportConfig source) {
        this.particleAmount = source.particleAmount;
        this.shaderPackEffectQuality = source.shaderPackEffectQuality;
        this.soundEnabled = source.soundEnabled;
        this.letterboxEnabled = source.letterboxEnabled;
        this.loadingStatusEnabled = source.loadingStatusEnabled;
    }

    public static TwilightTeleportConfig defaults() {
        return new TwilightTeleportConfig();
    }

    public TwilightTeleportConfig copy() {
        return new TwilightTeleportConfig(this);
    }

    public void sanitize() {
        if (particleAmount == null) {
            particleAmount = ParticleAmount.DEFAULT;
        }

        if (shaderPackEffectQuality == null) {
            shaderPackEffectQuality = ShaderPackEffectQuality.HIGH;
        }
    }

    public ParticleAmount getParticleAmount() {
        return particleAmount;
    }

    public void setParticleAmount(ParticleAmount particleAmount) {
        this.particleAmount = particleAmount == null
                ? ParticleAmount.DEFAULT
                : particleAmount;
    }

    public ShaderPackEffectQuality getShaderPackEffectQuality() {
        return shaderPackEffectQuality;
    }

    public void setShaderPackEffectQuality(
            ShaderPackEffectQuality shaderPackEffectQuality
    ) {
        this.shaderPackEffectQuality = shaderPackEffectQuality == null
                ? ShaderPackEffectQuality.HIGH
                : shaderPackEffectQuality;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isLetterboxEnabled() {
        return letterboxEnabled;
    }

    public void setLetterboxEnabled(boolean letterboxEnabled) {
        this.letterboxEnabled = letterboxEnabled;
    }

    public boolean isLoadingStatusEnabled() {
        return loadingStatusEnabled;
    }

    public void setLoadingStatusEnabled(boolean loadingStatusEnabled) {
        this.loadingStatusEnabled = loadingStatusEnabled;
    }
}
