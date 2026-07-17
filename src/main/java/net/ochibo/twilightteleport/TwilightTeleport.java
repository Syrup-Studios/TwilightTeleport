package net.ochibo.twilightteleport;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.ochibo.twilightteleport.network.ModNetworking;
import net.ochibo.twilightteleport.server.PendingTeleportManager;

public class TwilightTeleport implements ModInitializer {
    public static final String MOD_ID = "twilightteleport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        //? if >=1.20.5 {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?} else {
        /*return new ResourceLocation(MOD_ID, path);
        *///?}
    }

    @Override
    public void onInitialize() {
        ModParticles.register();
        ModNetworking.register();
        PendingTeleportManager.register();
    }
}
