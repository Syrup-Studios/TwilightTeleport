package net.ochibo.twilightteleport.client.particle;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.SimpleParticleType;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.render.TeleportRenderedHeightManager;

public final class TeleportShardParticle
        extends SpriteBillboardParticle {

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
            ClientWorld world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteProvider spriteProvider
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

        setSprite(spriteProvider);

        rebuilding = velocityY < 0.0D;
        targetEntityId = rebuilding
                ? (int) Math.round(velocityX)
                : -1;

        collidesWithWorld = false;
        gravityStrength = 0.0F;
        velocityMultiplier = 1.0F;

        if (rebuilding) {
            maxAge = 70 + random.nextInt(20);

            AbstractClientPlayerEntity player =
                    getTargetPlayer();

            if (player != null) {
                targetOffsetX = x - player.getX();
                targetOffsetY = velocityZ;
                targetOffsetZ = z - player.getZ();
                targetReferenceYaw = player.getBodyYaw();
            } else {
                targetOffsetX = 0.0D;
                targetOffsetY = velocityZ;
                targetOffsetZ = 0.0D;
                targetReferenceYaw = 0.0F;
            }

            this.velocityX = 0.0D;
            this.velocityY = -REBUILD_MAX_DOWNWARD_SPEED;
            this.velocityZ = 0.0D;
        } else {
            maxAge = 24 + random.nextInt(16);

            targetOffsetX = 0.0D;
            targetOffsetY = 0.0D;
            targetOffsetZ = 0.0D;
            targetReferenceYaw = 0.0F;

            this.velocityX = 0.0D;
            this.velocityY = Math.max(0.025D, velocityY);
            this.velocityZ = 0.0D;
        }

        scale =
                0.035F
                        + random.nextFloat()
                        * 0.075F;

        angle = 0.0F;
        prevAngle = angle;

        setColor(
                0.018F,
                0.012F,
                0.026F
        );

        setAlpha(1.0F);
    }

    @Override
    public void tick() {
        prevAngle = angle;

        if (rebuilding) {
            tickRebuilding();
        } else {
            tickOutgoing();
        }
    }

    private void tickOutgoing() {
        velocityX = 0.0D;
        velocityZ = 0.0D;

        velocityY =
                Math.min(
                        velocityY
                                + OUTGOING_UPWARD_ACCELERATION,
                        OUTGOING_MAX_UPWARD_SPEED
                );

        super.tick();

        float lifeProgress = age / (float) maxAge;

        setAlpha(
                1.0F
                        - lifeProgress
                        * lifeProgress
        );
    }

    private void tickRebuilding() {
        AbstractClientPlayerEntity player =
                getTargetPlayer();

        if (player == null
                || !TeleportEntityEffectManager
                .isRebuilding(player.getUuid())) {
            markDead();
            return;
        }

        
        net.minecraft.util.math.Vec3d rotatedOffset =
                TeleportRenderedHeightManager.rotateHorizontalOffset(
                        targetOffsetX,
                        targetOffsetZ,
                        targetReferenceYaw,
                        player.getBodyYaw()
                );

        x = player.getX() + rotatedOffset.x;
        z = player.getZ() + rotatedOffset.z;
        prevPosX = x;
        prevPosZ = z;

        double targetY =
                player.getY() + targetOffsetY;

        double remainingDistance = y - targetY;

        if (remainingDistance
                <= REBUILD_ARRIVAL_DISTANCE) {
            markDead();
            return;
        }

        velocityX = 0.0D;
        velocityZ = 0.0D;

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

        velocityY = -downwardSpeed;

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
                        / (float) maxAge;

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

    private AbstractClientPlayerEntity getTargetPlayer() {
        if (!rebuilding) {
            return null;
        }

        Entity entity = world.getEntityById(targetEntityId);

        if (entity
                instanceof AbstractClientPlayerEntity player) {
            return player;
        }

        return null;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory
            implements ParticleFactory<SimpleParticleType> {

        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType parameters,
                ClientWorld world,
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
