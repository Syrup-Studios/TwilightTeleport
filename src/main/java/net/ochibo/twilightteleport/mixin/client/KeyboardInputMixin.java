package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(
            method = "tick(ZF)V",
            at = @At("TAIL")
    )
    private void twilightTeleport$disableMovementInput(
            boolean slowDown,
            float slowDownFactor,
            CallbackInfo ci
    ) {
        if (!TeleportCameraController.shouldBlockInput()) {
            return;
        }

        Input input = (Input) (Object) this;

        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;

        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;

        input.jumping = false;
        input.sneaking = false;
    }
}