package net.ochibo.twilightteleport;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.ochibo.twilightteleport.network.ModNetworking;
import net.ochibo.twilightteleport.server.PendingTeleportManager;

public class TwilightTeleport implements ModInitializer {
    public static final String MOD_ID = "twilightteleport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModParticles.register();
        ModNetworking.register();
        PendingTeleportManager.register();
    }
}
