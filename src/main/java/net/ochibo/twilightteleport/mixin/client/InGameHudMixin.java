package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.client.hud.TeleportLoadingStatusHud;
import net.ochibo.twilightteleport.client.render.TeleportOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$renderTeleportOverlay(
            DrawContext context,
            RenderTickCounter tickCounter,
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