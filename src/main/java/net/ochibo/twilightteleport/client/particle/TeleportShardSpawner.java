package net.ochibo.twilightteleport.client.particle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.ochibo.twilightteleport.ModParticles;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.render.TeleportRenderedHeightManager;

public final class TeleportShardSpawner {

    private static final double REBUILD_MIN_SPAWN_HEIGHT = 15.0D;
    private static final double REBUILD_MAX_SPAWN_HEIGHT = 20.0D;
    private static final float PARTICLE_REBUILD_TIME = 0.9f;

    
    private static final float CROSS_SECTION_HEIGHT_TOLERANCE = 0.1F;

    
    private static final double BOUNDARY_Y_JITTER = 0.05D;

    private TeleportShardSpawner() {
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;

        if (world == null) {
            return;
        }

        int particlesPerTick =
                TwilightTeleportConfigManager
                        .get()
                        .getParticleAmount()
                        .particlesPerTick();

        if (particlesPerTick <= 0) {
            return;
        }

        for (AbstractClientPlayerEntity player
                : world.getPlayers()) {

            boolean dissolving =
                    TeleportEntityEffectManager
                            .isDissolving(
                                    player.getUuid()
                            );

            boolean rebuilding =
                    TeleportEntityEffectManager
                            .isRebuilding(
                                    player.getUuid()
                            );

            if (!dissolving && !rebuilding) {
                continue;
            }

            
            if (!TeleportRenderedHeightManager
                    .ensureEffectSnapshotReady(player)) {
                continue;
            }

            if (dissolving) {
                spawnOutgoing(world, player, particlesPerTick);
            }

            if (rebuilding
                    && TeleportEntityEffectManager
                    .getRebuildingProgress(
                            player.getUuid(),
                            0.0F
                    ) < PARTICLE_REBUILD_TIME) {
                spawnRebuilding(world, player, particlesPerTick);
            }
        }
    }

    private static void spawnOutgoing(
            ClientWorld world,
            AbstractClientPlayerEntity player,
            int particlesPerTick
    ) {
        float dissolve =
                TeleportEntityEffectManager
                        .getDissolveProgress(
                                player.getUuid(),
                                0.0F
                        );

        float effectHeight =
                TeleportRenderedHeightManager
                        .getEffectHeight(player);

        float localBoundaryY =
                effectHeight * (1.0F - dissolve);

        Random random = world.random;

        for (int i = 0;
             i < particlesPerTick;
             i++) {

            Vec3d spawnPoint =
                    sampleBoundaryCrossSection(
                            player,
                            localBoundaryY,
                            random
                    );

            double velocityY =
                    0.075D
                            + random.nextDouble()
                            * 0.105D;

            world.addParticle(
                    ModParticles.TELEPORT_SHARD,
                    spawnPoint.x,
                    spawnPoint.y - 0.1f,
                    spawnPoint.z,
                    0.0D,
                    velocityY,
                    0.0D
            );
        }
    }

    private static void spawnRebuilding(
            ClientWorld world,
            AbstractClientPlayerEntity player,
            int particlesPerTick
    ) {
        
        float rebuildProgress =
                TeleportEntityEffectManager
                        .getRebuildingProgress(
                                player.getUuid(),
                                0.0F
                        );

        float particleBoundaryProgress =
                Math.clamp(
                        rebuildProgress
                                / PARTICLE_REBUILD_TIME,
                        0.0F,
                        1.0F
                );

        float effectHeight =
                TeleportRenderedHeightManager
                        .getEffectHeight(player);

        float localBoundaryY =
                effectHeight * particleBoundaryProgress;

        Random random = world.random;

        for (int i = 0;
             i < particlesPerTick;
             i++) {

            Vec3d targetPoint =
                    sampleBoundaryCrossSection(
                            player,
                            localBoundaryY,
                            random
                    );

            double spawnHeight =
                    lerp(
                            random.nextDouble(),
                            REBUILD_MIN_SPAWN_HEIGHT,
                            REBUILD_MAX_SPAWN_HEIGHT
                    );

            world.addParticle(
                    ModParticles.TELEPORT_SHARD,
                    targetPoint.x,
                    targetPoint.y + spawnHeight,
                    targetPoint.z,
                    player.getId(),
                    -1.0D,
                    targetPoint.y - player.getY()
            );
        }
    }

    
    private static Vec3d sampleBoundaryCrossSection(
            AbstractClientPlayerEntity player,
            float localBoundaryY,
            Random random
    ) {
        Vec3d point =
                TeleportRenderedHeightManager
                        .sampleCrossSectionPoint(
                                player,
                                localBoundaryY,
                                CROSS_SECTION_HEIGHT_TOLERANCE,
                                random
                        );

        return point.add(
                0.0D,
                centered(random, BOUNDARY_Y_JITTER),
                0.0D
        );
    }

    private static double centered(
            Random random,
            double range
    ) {
        return (
                random.nextDouble()
                        - 0.5D
        ) * 2.0D * range;
    }

    private static double lerp(
            double progress,
            double start,
            double end
    ) {
        return start
                + (end - start)
                * progress;
    }
}
