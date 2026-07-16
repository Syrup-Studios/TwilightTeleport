package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import net.ochibo.twilightteleport.TeleportCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(
            method = "update",
            at = @At("TAIL")
    )
    private void twilightTeleport$applyTeleportCamera(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        TeleportCameraController.CameraFrame frame =
                TeleportCameraController.getCameraFrame(tickDelta);

        if (frame == null) {
            return;
        }

        CameraAccessor accessor = (CameraAccessor) this;

        accessor.twilightTeleport$setPos(frame.position());
        accessor.twilightTeleport$setRotation(
                frame.yaw(),
                frame.pitch()
        );
    }
}