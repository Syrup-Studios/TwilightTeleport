package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.MouseHandler;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Inject(
            method = "onScroll",
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