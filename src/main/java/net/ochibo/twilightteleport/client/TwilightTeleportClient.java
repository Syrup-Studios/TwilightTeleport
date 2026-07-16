package net.ochibo.twilightteleport.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.ochibo.twilightteleport.ModParticles;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.client.command.TwilightTeleportClientCommand;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.network.TeleportClientNetworking;
import net.ochibo.twilightteleport.client.particle.TeleportShardParticle;
import net.ochibo.twilightteleport.client.particle.TeleportShardSpawner;
import net.ochibo.twilightteleport.client.render.TeleportDissolveShaders;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public class TwilightTeleportClient
        implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TwilightTeleportConfigManager.load();
        TwilightTeleportClientCommand.register();
        TeleportDissolveShaders.register();
        TeleportClientNetworking.register();

        ParticleFactoryRegistry.getInstance().register(
                ModParticles.TELEPORT_SHARD,
                TeleportShardParticle.Factory::new
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                TeleportCameraController::tick
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                TeleportEntityEffectManager::tick
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                TeleportShardSpawner::tick
        );
    }
}
