package net.ochibo.twilightteleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;

public final class CinematicWorldFreeze {

    
    private static volatile IntegratedServer frozenServer;
    private static volatile boolean ownsFreeze;

    private CinematicWorldFreeze() {
    }

    public static void freezeIfPossible() {
        Minecraft client = Minecraft.getInstance();
        IntegratedServer server = client.getSingleplayerServer();

        
        if (server == null) {
            return;
        }

        frozenServer = server;

        //? if >=1.20.3 {
        server.execute(() -> {
            
            if (server.tickRateManager().isFrozen()) {
                ownsFreeze = false;
                return;
            }

            server.tickRateManager().setFrozen(true);
            ownsFreeze = true;
        });
        //?} else {
        /*// Minecraft 1.20.1 has no tick-rate manager. The cinematic
        // continues without freezing the integrated server on this target.
        ownsFreeze = false;
        *///?}
    }

    public static void unfreeze() {
        IntegratedServer server = frozenServer;

        frozenServer = null;

        if (server == null) {
            ownsFreeze = false;
            return;
        }

        //? if >=1.20.3 {
        server.execute(() -> {
            if (ownsFreeze) {
                server.tickRateManager().setFrozen(false);
            }

            ownsFreeze = false;
        });
        //?} else {
        /*ownsFreeze = false;
        *///?}
    }
}
