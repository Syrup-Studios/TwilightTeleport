package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(
            method = "changeLookDirection(DD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$disablePlayerLook(
            double cursorDeltaX,
            double cursorDeltaY,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if ((Entity)(Object) this != client.player) {
            return;
        }

        if (TeleportCameraController.shouldBlockInput()) {
            ci.cancel();
        }
    }
}