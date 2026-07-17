package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
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

        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;

        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;

        input.jumping = false;
        input.shiftKeyDown = false;
    }
}