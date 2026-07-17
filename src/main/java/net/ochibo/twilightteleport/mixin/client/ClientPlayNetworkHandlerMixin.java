package net.ochibo.twilightteleport.mixin.client;

import com.mojang.brigadier.ParseResults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.TeleportCommandMatcher;
import net.ochibo.twilightteleport.client.network.TeleportClientNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    
    @Unique
    private boolean twilightTeleport$bypassTeleportCommand;

    @Inject(
            method = "sendCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$delayTeleportCommand(
            String command,
            CallbackInfo ci
    ) {
        if (twilightTeleport$bypassTeleportCommand) {
            twilightTeleport$bypassTeleportCommand = false;
            return;
        }

        if (TeleportCameraController.isRunning()
                || !TeleportCommandMatcher.isTeleportCommand(command)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return;
        }

        ClientPacketListener networkHandler =
                (ClientPacketListener) (Object) this;

        
        if (TeleportClientNetworking.isServerInstalled()) {
            return;
        }

        
        if (!twilightTeleport$canExecuteCommand(
                networkHandler,
                command
        )) {
            return;
        }

        ci.cancel();

        Runnable sendOriginalCommand = () -> {
            twilightTeleport$bypassTeleportCommand = true;
            networkHandler.sendCommand(command);
        };

        TeleportCameraController.start(sendOriginalCommand);

        
        if (!TeleportCameraController.isRunning()) {
            sendOriginalCommand.run();
        }
    }

    @Unique
    private static boolean twilightTeleport$canExecuteCommand(
            ClientPacketListener networkHandler,
            String command
    ) {
        String normalized = command == null
                ? ""
                : command.stripLeading();

        if (normalized.startsWith("/")) {
            normalized = normalized
                    .substring(1)
                    .stripLeading();
        }

        if (normalized.isEmpty()) {
            return false;
        }

        ParseResults<SharedSuggestionProvider> parseResults =
                networkHandler
                        .getCommands()
                        .parse(
                                normalized,
                                networkHandler.getSuggestionsProvider()
                        );

        return !parseResults.getReader().canRead()
                && parseResults.getContext().getCommand() != null;
    }

    @Inject(
            method = "handleMovePlayer",
            at = @At("TAIL")
    )
    private void twilightTeleport$markTeleportArrival(
            ClientboundPlayerPositionPacket packet,
            CallbackInfo ci
    ) {
        if (TeleportCameraController.isWaitingForTeleport()) {
            TeleportCameraController.markArrived();
        }
    }
}
