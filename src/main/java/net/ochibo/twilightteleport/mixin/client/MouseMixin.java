package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.Mouse;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(
            method = "onMouseScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disableMouseScroll(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo ci
    ) {
        if (TeleportCameraController.shouldBlockInput()) {
            ci.cancel();
        }
    }
}