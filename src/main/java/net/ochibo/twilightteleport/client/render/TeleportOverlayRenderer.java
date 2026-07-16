package net.ochibo.twilightteleport.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TeleportOverlayRenderer {

    private static final float MAX_LETTERBOX_RATIO = 0.115F;

    private TeleportOverlayRenderer() {
    }

    public static void render(
            DrawContext context,
            RenderTickCounter tickCounter
    ) {
        float tickDelta = tickCounter.getTickDelta(false);

        renderLetterbox(context, tickDelta);
        renderFade(context, tickDelta);
    }

    private static void renderLetterbox(
            DrawContext context,
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

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

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
            DrawContext context,
            float tickDelta
    ) {
        float fade =
                TeleportCameraController.getFadeAlpha(tickDelta);

        if (fade <= 0.0F) {
            return;
        }

        int alpha = MathHelper.clamp(
                Math.round(fade * 255.0F),
                0,
                255
        );

        int color = alpha << 24;

        context.fill(
                0,
                0,
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight(),
                color
        );
    }
}