package net.ochibo.twilightteleport.mixin.client;

import com.mojang.brigadier.ParseResults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.TeleportCommandMatcher;
import net.ochibo.twilightteleport.client.network.TeleportClientNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    
    @Unique
    private boolean twilightTeleport$bypassTeleportCommand;

    @Inject(
            method = "sendChatCommand",
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

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            return;
        }

        ClientPlayNetworkHandler networkHandler =
                (ClientPlayNetworkHandler) (Object) this;

        
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
            networkHandler.sendChatCommand(command);
        };

        TeleportCameraController.start(sendOriginalCommand);

        
        if (!TeleportCameraController.isRunning()) {
            sendOriginalCommand.run();
        }
    }

    @Unique
    private static boolean twilightTeleport$canExecuteCommand(
            ClientPlayNetworkHandler networkHandler,
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

        ParseResults<CommandSource> parseResults =
                networkHandler
                        .getCommandDispatcher()
                        .parse(
                                normalized,
                                networkHandler.getCommandSource()
                        );

        return !parseResults.getReader().canRead()
                && parseResults.getContext().getCommand() != null;
    }

    @Inject(
            method = "onPlayerPositionLook",
            at = @At("TAIL")
    )
    private void twilightTeleport$markTeleportArrival(
            PlayerPositionLookS2CPacket packet,
            CallbackInfo ci
    ) {
        if (TeleportCameraController.isWaitingForTeleport()) {
            TeleportCameraController.markArrived();
        }
    }
}
