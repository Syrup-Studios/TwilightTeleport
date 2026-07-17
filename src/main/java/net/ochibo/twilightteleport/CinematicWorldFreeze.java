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

        
        server.execute(() -> {
            
            if (server.tickRateManager().isFrozen()) {
                ownsFreeze = false;
                return;
            }

            server.tickRateManager().setFrozen(true);
            ownsFreeze = true;
        });
    }

    public static void unfreeze() {
        IntegratedServer server = frozenServer;

        frozenServer = null;

        if (server == null) {
            ownsFreeze = false;
            return;
        }

        server.execute(() -> {
            if (ownsFreeze) {
                server.tickRateManager().setFrozen(false);
            }

            ownsFreeze = false;
        });
    }
}