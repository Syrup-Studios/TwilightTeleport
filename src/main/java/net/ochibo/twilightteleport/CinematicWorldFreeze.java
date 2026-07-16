package net.ochibo.twilightteleport;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

public final class CinematicWorldFreeze {

    
    private static volatile IntegratedServer frozenServer;
    private static volatile boolean ownsFreeze;

    private CinematicWorldFreeze() {
    }

    public static void freezeIfPossible() {
        MinecraftClient client = MinecraftClient.getInstance();
        IntegratedServer server = client.getServer();

        
        if (server == null) {
            return;
        }

        frozenServer = server;

        
        server.execute(() -> {
            
            if (server.getTickManager().isFrozen()) {
                ownsFreeze = false;
                return;
            }

            server.getTickManager().setFrozen(true);
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
                server.getTickManager().setFrozen(false);
            }

            ownsFreeze = false;
        });
    }
}