package net.ochibo.twilightteleport.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
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

    public static void tick(Minecraft client) {
        ClientLevel world = client.level;

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

        for (AbstractClientPlayer player
                : world.players()) {

            boolean dissolving =
                    TeleportEntityEffectManager
                            .isDissolving(
                                    player.getUUID()
                            );

            boolean rebuilding =
                    TeleportEntityEffectManager
                            .isRebuilding(
                                    player.getUUID()
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
                            player.getUUID(),
                            0.0F
                    ) < PARTICLE_REBUILD_TIME) {
                spawnRebuilding(world, player, particlesPerTick);
            }
        }
    }

    private static void spawnOutgoing(
            ClientLevel world,
            AbstractClientPlayer player,
            int particlesPerTick
    ) {
        float dissolve =
                TeleportEntityEffectManager
                        .getDissolveProgress(
                                player.getUUID(),
                                0.0F
                        );

        float effectHeight =
                TeleportRenderedHeightManager
                        .getEffectHeight(player);

        float localBoundaryY =
                effectHeight * (1.0F - dissolve);

        RandomSource random = world.random;

        for (int i = 0;
             i < particlesPerTick;
             i++) {

            Vec3 spawnPoint =
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
            ClientLevel world,
            AbstractClientPlayer player,
            int particlesPerTick
    ) {
        
        float rebuildProgress =
                TeleportEntityEffectManager
                        .getRebuildingProgress(
                                player.getUUID(),
                                0.0F
                        );

        float particleBoundaryProgress =
                Mth.clamp(
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

        RandomSource random = world.random;

        for (int i = 0;
             i < particlesPerTick;
             i++) {

            Vec3 targetPoint =
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

    
    private static Vec3 sampleBoundaryCrossSection(
            AbstractClientPlayer player,
            float localBoundaryY,
            RandomSource random
    ) {
        Vec3 point =
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
            RandomSource random,
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
