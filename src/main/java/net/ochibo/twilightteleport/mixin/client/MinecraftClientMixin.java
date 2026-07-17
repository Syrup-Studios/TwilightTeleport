package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    
    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$hideDimensionLoadingScreen(
            Screen screen,
            CallbackInfo ci
    ) {
        if (screen instanceof ReceivingLevelScreen
                && TeleportCameraController.isWaitingForTeleport()) {
            ci.cancel();
        }
    }

    
    @Inject(
            method = "handleKeybinds",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disableInputEvents(
            CallbackInfo ci
    ) {
        if (TeleportCameraController.shouldBlockInput()) {
            ci.cancel();
        }
    }

    
    @Inject(
            method = "continueAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disableBlockBreaking(
            boolean breaking,
            CallbackInfo ci
    ) {
        if (TeleportCameraController.shouldBlockInput()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "startAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disableAttack(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (TeleportCameraController.shouldBlockInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "startUseItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disableItemUse(
            CallbackInfo ci
    ) {
        if (TeleportCameraController.shouldBlockInput()) {
            ci.cancel();
        }
    }
}