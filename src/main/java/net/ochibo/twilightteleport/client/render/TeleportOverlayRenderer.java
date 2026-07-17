package net.ochibo.twilightteleport.client.render;

//? if >=1.20.5
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TeleportOverlayRenderer {

    private static final float MAX_LETTERBOX_RATIO = 0.115F;

    private TeleportOverlayRenderer() {
    }

    public static void render(
            GuiGraphics context,
            //? if >=1.20.5 {
            DeltaTracker tickCounter
            //?} else {
            /*float tickDelta
            *///?}
    ) {
        //? if >=1.20.5
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);

        renderLetterbox(context, tickDelta);
        renderFade(context, tickDelta);
    }

    private static void renderLetterbox(
            GuiGraphics context,
            float tickDelta
    ) {
        if (!TwilightTeleportConfigManager.get().isLetterboxEnabled()) {
            return;
        }

        float progress =
                TeleportCameraController.getLetterboxProgress(tickDelta);

        if (progress <= 0.0F) {
            return;
        }

        int width = context.guiWidth();
        int height = context.guiHeight();

        int barHeight = Math.round(
                height * MAX_LETTERBOX_RATIO * progress
        );

        if (barHeight <= 0) {
            return;
        }

        context.fill(
                0,
                0,
                width,
                barHeight,
                0xFF000000
        );

        context.fill(
                0,
                height - barHeight,
                width,
                height,
                0xFF000000
        );
    }

    private static void renderFade(
            GuiGraphics context,
            float tickDelta
    ) {
        float fade =
                TeleportCameraController.getFadeAlpha(tickDelta);

        if (fade <= 0.0F) {
            return;
        }

        int alpha = Mth.clamp(
                Math.round(fade * 255.0F),
                0,
                255
        );

        int color = alpha << 24;

        context.fill(
                0,
                0,
                context.guiWidth(),
                context.guiHeight(),
                color
        );
    }
}
