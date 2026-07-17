package net.ochibo.twilightteleport.client.hud;

//? if >=1.20.5
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.ochibo.twilightteleport.TeleportCameraController;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TeleportLoadingStatusHud {

    
    private static final int TEXT_RGB = 0x00666666;

    
    private static final int MIN_RENDERABLE_ALPHA = 4;

    private static final int LEFT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;

    private static final float FADE_IN_DURATION_SECONDS = 0.50F;
    private static final float FADE_OUT_DURATION_SECONDS = 0.10F;

    private static float alpha;

    
    private static Component displayedText =
            Component.empty();

    private static long previousFrameNanos;

    private TeleportLoadingStatusHud() {
    }

    public static void render(
            GuiGraphics drawContext,
            //? if >=1.20.5 {
            DeltaTracker tickCounter
            //?} else {
            /*float tickDelta
            *///?}
    ) {
        boolean shouldShow =
                TwilightTeleportConfigManager
                        .get()
                        .isLoadingStatusEnabled()
                        && TeleportCameraController
                        .shouldShowLoadingStatus();

        Component currentText =
                TeleportCameraController
                        .getLoadingStatusText();

        boolean hasCurrentText =
                !currentText.getString().isEmpty();

        
        if (shouldShow && hasCurrentText) {
            displayedText = currentText;
        }

        boolean hasDisplayedText =
                !displayedText.getString().isEmpty();

        float targetAlpha =
                shouldShow && hasDisplayedText
                        ? 1.0F
                        : 0.0F;

        float deltaSeconds =
                calculateDeltaSeconds();

        alpha = moveTowards(
                alpha,
                targetAlpha,
                deltaSeconds
        );

        if (alpha <= 0.0F) {
            alpha = 0.0F;

            if (!shouldShow) {
                displayedText =
                        Component.empty();
            }

            return;
        }

        if (!hasDisplayedText) {
            return;
        }

        float easedAlpha =
                smootherStep(alpha);

        int alphaByte =
                Mth.clamp(
                        Math.round(
                                easedAlpha * 255.0F
                        ),
                        0,
                        255
                );

        
        if (alphaByte < MIN_RENDERABLE_ALPHA) {
            return;
        }

        int color =
                alphaByte << 24
                        | TEXT_RGB;

        Minecraft client =
                Minecraft.getInstance();

        int y =
                drawContext.guiHeight()
                        - client.font.lineHeight
                        - BOTTOM_MARGIN;

        drawContext.drawString(
                client.font,
                displayedText,
                LEFT_MARGIN,
                y,
                color
        );
    }

    private static float moveTowards(
            float current,
            float target,
            float deltaSeconds
    ) {
        float duration =
                target > current
                        ? FADE_IN_DURATION_SECONDS
                        : FADE_OUT_DURATION_SECONDS;

        if (duration <= 0.0F) {
            return target;
        }

        float maximumChange =
                deltaSeconds / duration;

        if (current < target) {
            return Math.min(
                    current + maximumChange,
                    target
            );
        }

        return Math.max(
                current - maximumChange,
                target
        );
    }

    private static float calculateDeltaSeconds() {
        long currentNanos =
                System.nanoTime();

        if (previousFrameNanos == 0L) {
            previousFrameNanos =
                    currentNanos;

            return 0.0F;
        }

        float deltaSeconds =
                (currentNanos - previousFrameNanos)
                        / 1_000_000_000.0F;

        previousFrameNanos =
                currentNanos;

        return Mth.clamp(
                deltaSeconds,
                0.0F,
                0.1F
        );
    }

    private static float smootherStep(float value) {
        float x =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return x * x * x
                * (
                x * (x * 6.0F - 15.0F)
                        + 10.0F
        );
    }

    public static void reset() {
        alpha = 0.0F;
        displayedText =
                Component.empty();

        previousFrameNanos = 0L;
    }
}
