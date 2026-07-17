package net.ochibo.twilightteleport.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ochibo.twilightteleport.config.TwilightTeleportConfig;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TwilightTeleportConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_HEIGHT = 20;

    private final Screen parent;
    private TwilightTeleportConfig working;

    public TwilightTeleportConfigScreen(Screen parent) {
        super(Component.translatable("config.twilightteleport.title"));
        this.parent = parent;
        this.working = TwilightTeleportConfigManager.copy();
    }

    @Override
    protected void init() {
        int gap = 8;
        int widgetWidth = Math.min(320, Math.max(180, width - 32));
        int x = width / 2 - widgetWidth / 2;
        int startY = 44;

        addRenderableWidget(createParticleButton(
                x,
                startY,
                widgetWidth
        ));

        addRenderableWidget(createShaderPackQualityButton(
                x,
                startY + ROW_HEIGHT,
                widgetWidth
        ));

        addRenderableWidget(createToggleButton(
                x,
                startY + ROW_HEIGHT * 2,
                widgetWidth,
                "config.twilightteleport.sound",
                working.isSoundEnabled(),
                working::setSoundEnabled
        ));

        addRenderableWidget(createToggleButton(
                x,
                startY + ROW_HEIGHT * 3,
                widgetWidth,
                "config.twilightteleport.letterbox",
                working.isLetterboxEnabled(),
                working::setLetterboxEnabled
        ));

        addRenderableWidget(createToggleButton(
                x,
                startY + ROW_HEIGHT * 4,
                widgetWidth,
                "config.twilightteleport.loading_status",
                working.isLoadingStatusEnabled(),
                working::setLoadingStatusEnabled
        ));

        int bottomY = Math.min(
                height - 28,
                startY + ROW_HEIGHT * 6
        );
        int buttonWidth = Math.min(100, (width - 4 * gap) / 3);
        int totalWidth = buttonWidth * 3 + gap * 2;
        int firstX = width / 2 - totalWidth / 2;

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "config.twilightteleport.reset"
                                ),
                                button -> {
                                    working = TwilightTeleportConfig.defaults();
                                    rebuildWidgets();
                                }
                        )
                        .bounds(
                                firstX,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.cancel"),
                                button -> onClose()
                        )
                        .bounds(
                                firstX + buttonWidth + gap,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.done"),
                                button -> saveAndClose()
                        )
                        .bounds(
                                firstX + (buttonWidth + gap) * 2,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );
    }

    private Button createParticleButton(
            int x,
            int y,
            int width
    ) {
        return Button.builder(
                        particleButtonText(),
                        button -> {
                            working.setParticleAmount(
                                    working.getParticleAmount().next()
                            );
                            button.setMessage(particleButtonText());
                        }
                )
                .bounds(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private Component particleButtonText() {
        return Component.translatable(
                "config.twilightteleport.particle_amount"
        ).append(": ").append(
                working.getParticleAmount().displayText()
        );
    }

    private Button createShaderPackQualityButton(
            int x,
            int y,
            int width
    ) {
        return Button.builder(
                        shaderPackQualityButtonText(),
                        button -> {
                            working.setShaderPackEffectQuality(
                                    working
                                            .getShaderPackEffectQuality()
                                            .next()
                            );
                            button.setMessage(
                                    shaderPackQualityButtonText()
                            );
                        }
                )
                .bounds(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private Component shaderPackQualityButtonText() {
        return Component.translatable(
                "config.twilightteleport.shader_pack_quality"
        ).append(": ").append(
                working
                        .getShaderPackEffectQuality()
                        .displayText()
        );
    }

    private Button createToggleButton(
            int x,
            int y,
            int width,
            String translationKey,
            boolean initialValue,
            java.util.function.Consumer<Boolean> setter
    ) {
        final boolean[] value = {initialValue};

        return Button.builder(
                        toggleText(translationKey, value[0]),
                        button -> {
                            value[0] = !value[0];
                            setter.accept(value[0]);
                            button.setMessage(
                                    toggleText(
                                            translationKey,
                                            value[0]
                                    )
                            );
                        }
                )
                .bounds(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private static Component toggleText(
            String translationKey,
            boolean enabled
    ) {
        return Component.translatable(translationKey)
                .append(": ")
                .append(Component.translatable(
                        enabled
                                ? "options.on"
                                : "options.off"
                ));
    }

    private void saveAndClose() {
        TwilightTeleportConfigManager.set(working);
        TwilightTeleportConfigManager.save();
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(
            GuiGraphics context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        //? if >=1.20.5 {
        renderBackground(context, mouseX, mouseY, delta);
        //?} else {
        /*renderBackground(context);
        *///?}
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(
                font,
                title,
                width / 2,
                16,
                0xFFFFFF
        );

        context.drawString(
                font,
                Component.translatable(
                        "config.twilightteleport.visual_section"
                ),
                Math.max(8, width / 2 - 160),
                30,
                0xA0A0A0
        );
    }
}
