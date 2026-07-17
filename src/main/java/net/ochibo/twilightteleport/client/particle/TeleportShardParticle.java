package net.ochibo.twilightteleport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.render.TeleportRenderedHeightManager;

public final class TeleportShardParticle
        extends TextureSheetParticle {

    private static final double OUTGOING_UPWARD_ACCELERATION = 0.055D;
    private static final double OUTGOING_MAX_UPWARD_SPEED = 4.00D;

    private static final double REBUILD_MAX_DOWNWARD_SPEED = 6.40D;
    private static final double REBUILD_BRAKE_START_DISTANCE = 9.0D;
    private static final double REBUILD_DECELERATION_STRENGTH = 1.8D;
    private static final double REBUILD_MIN_DOWNWARD_SPEED = 0.12D;
    private static final double REBUILD_ARRIVAL_DISTANCE = 0.05D;
    private static final double REBUILD_FADE_DISTANCE = 0.3D;

    private final boolean rebuilding;
    private final int targetEntityId;

    
    private final double targetOffsetX;
    private final double targetOffsetY;
    private final double targetOffsetZ;
    private final float targetReferenceYaw;

    private TeleportShardParticle(
            ClientLevel world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteSet spriteProvider
    ) {
        super(
                world,
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ
        );

        pickSprite(spriteProvider);

        rebuilding = velocityY < 0.0D;
        targetEntityId = rebuilding
                ? (int) Math.round(velocityX)
                : -1;

        hasPhysics = false;
        gravity = 0.0F;
        friction = 1.0F;

        if (rebuilding) {
            lifetime = 70 + random.nextInt(20);

            AbstractClientPlayer player =
                    getTargetPlayer();

            if (player != null) {
                targetOffsetX = x - player.getX();
                targetOffsetY = velocityZ;
                targetOffsetZ = z - player.getZ();
                targetReferenceYaw = player.getVisualRotationYInDegrees();
            } else {
                targetOffsetX = 0.0D;
                targetOffsetY = velocityZ;
                targetOffsetZ = 0.0D;
                targetReferenceYaw = 0.0F;
            }

            this.xd = 0.0D;
            this.yd = -REBUILD_MAX_DOWNWARD_SPEED;
            this.zd = 0.0D;
        } else {
            lifetime = 24 + random.nextInt(16);

            targetOffsetX = 0.0D;
            targetOffsetY = 0.0D;
            targetOffsetZ = 0.0D;
            targetReferenceYaw = 0.0F;

            this.xd = 0.0D;
            this.yd = Math.max(0.025D, velocityY);
            this.zd = 0.0D;
        }

        quadSize =
                0.035F
                        + random.nextFloat()
                        * 0.075F;

        roll = 0.0F;
        oRoll = roll;

        setColor(
                0.018F,
                0.012F,
                0.026F
        );

        setAlpha(1.0F);
    }

    @Override
    public void tick() {
        oRoll = roll;

        if (rebuilding) {
            tickRebuilding();
        } else {
            tickOutgoing();
        }
    }

    private void tickOutgoing() {
        xd = 0.0D;
        zd = 0.0D;

        yd =
                Math.min(
                        yd
                                + OUTGOING_UPWARD_ACCELERATION,
                        OUTGOING_MAX_UPWARD_SPEED
                );

        super.tick();

        float lifeProgress = age / (float) lifetime;

        setAlpha(
                1.0F
                        - lifeProgress
                        * lifeProgress
        );
    }

    private void tickRebuilding() {
        AbstractClientPlayer player =
                getTargetPlayer();

        if (player == null
                || !TeleportEntityEffectManager
                .isRebuilding(player.getUUID())) {
            remove();
            return;
        }

        
        net.minecraft.world.phys.Vec3 rotatedOffset =
                TeleportRenderedHeightManager.rotateHorizontalOffset(
                        targetOffsetX,
                        targetOffsetZ,
                        targetReferenceYaw,
                        player.getVisualRotationYInDegrees()
                );

        x = player.getX() + rotatedOffset.x;
        z = player.getZ() + rotatedOffset.z;
        xo = x;
        zo = z;

        double targetY =
                player.getY() + targetOffsetY;

        double remainingDistance = y - targetY;

        if (remainingDistance
                <= REBUILD_ARRIVAL_DISTANCE) {
            remove();
            return;
        }

        xd = 0.0D;
        zd = 0.0D;

        double normalizedDistance =
                Math.min(
                        1.0D,
                        remainingDistance
                                / REBUILD_BRAKE_START_DISTANCE
                );

        double decelerationCurve =
                Math.pow(
                        normalizedDistance,
                        REBUILD_DECELERATION_STRENGTH
                );

        double downwardSpeed =
                REBUILD_MIN_DOWNWARD_SPEED
                        + (
                        REBUILD_MAX_DOWNWARD_SPEED
                                - REBUILD_MIN_DOWNWARD_SPEED
                ) * decelerationCurve;

        downwardSpeed =
                Math.min(
                        downwardSpeed,
                        remainingDistance
                );

        yd = -downwardSpeed;

        super.tick();

        float proximityAlpha =
                (float) Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                remainingDistance
                                        / REBUILD_FADE_DISTANCE
                        )
                );

        float ageAlpha =
                1.0F
                        - age
                        / (float) lifetime;

        setAlpha(
                Math.max(
                        0.0F,
                        Math.min(
                                proximityAlpha,
                                ageAlpha * 2.0F
                        )
                )
        );
    }

    private AbstractClientPlayer getTargetPlayer() {
        if (!rebuilding) {
            return null;
        }

        Entity entity = level.getEntity(targetEntityId);

        if (entity
                instanceof AbstractClientPlayer player) {
            return player;
        }

        return null;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory
            implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType parameters,
                ClientLevel world,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new TeleportShardParticle(
                    world,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    spriteProvider
            );
        }
    }
}
