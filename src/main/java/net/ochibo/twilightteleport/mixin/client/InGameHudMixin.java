package net.ochibo.twilightteleport.mixin.client;

//? if >=1.20.5
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.client.hud.TeleportLoadingStatusHud;
import net.ochibo.twilightteleport.client.render.TeleportOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$renderTeleportOverlay(
            GuiGraphics context,
            //? if >=1.20.5 {
            DeltaTracker tickCounter,
            //?} else {
            /*float tickCounter,
            *///?}
            CallbackInfo ci
    ) {
        if (!TeleportCameraController.isRunning()) {
            return;
        }

        
        TeleportOverlayRenderer.render(
                context,
                tickCounter
        );

        
        TeleportLoadingStatusHud.render(
                context,
                tickCounter
        );

        
        ci.cancel();
    }
}
