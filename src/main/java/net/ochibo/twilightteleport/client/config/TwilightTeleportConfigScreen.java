package net.ochibo.twilightteleport.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.ochibo.twilightteleport.config.TwilightTeleportConfig;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TwilightTeleportConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_HEIGHT = 20;

    private final Screen parent;
    private TwilightTeleportConfig working;

    public TwilightTeleportConfigScreen(Screen parent) {
        super(Text.translatable("config.twilightteleport.title"));
        this.parent = parent;
        this.working = TwilightTeleportConfigManager.copy();
    }

    @Override
    protected void init() {
        int gap = 8;
        int widgetWidth = Math.min(320, Math.max(180, width - 32));
        int x = width / 2 - widgetWidth / 2;
        int startY = 44;

        addDrawableChild(createParticleButton(
                x,
                startY,
                widgetWidth
        ));

        addDrawableChild(createShaderPackQualityButton(
                x,
                startY + ROW_HEIGHT,
                widgetWidth
        ));

        addDrawableChild(createToggleButton(
                x,
                startY + ROW_HEIGHT * 2,
                widgetWidth,
                "config.twilightteleport.sound",
                working.isSoundEnabled(),
                working::setSoundEnabled
        ));

        addDrawableChild(createToggleButton(
                x,
                startY + ROW_HEIGHT * 3,
                widgetWidth,
                "config.twilightteleport.letterbox",
                working.isLetterboxEnabled(),
                working::setLetterboxEnabled
        ));

        addDrawableChild(createToggleButton(
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

        addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable(
                                        "config.twilightteleport.reset"
                                ),
                                button -> {
                                    working = TwilightTeleportConfig.defaults();
                                    clearAndInit();
                                }
                        )
                        .dimensions(
                                firstX,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("gui.cancel"),
                                button -> close()
                        )
                        .dimensions(
                                firstX + buttonWidth + gap,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("gui.done"),
                                button -> saveAndClose()
                        )
                        .dimensions(
                                firstX + (buttonWidth + gap) * 2,
                                bottomY,
                                buttonWidth,
                                WIDGET_HEIGHT
                        )
                        .build()
        );
    }

    private ButtonWidget createParticleButton(
            int x,
            int y,
            int width
    ) {
        return ButtonWidget.builder(
                        particleButtonText(),
                        button -> {
                            working.setParticleAmount(
                                    working.getParticleAmount().next()
                            );
                            button.setMessage(particleButtonText());
                        }
                )
                .dimensions(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private Text particleButtonText() {
        return Text.translatable(
                "config.twilightteleport.particle_amount"
        ).append(": ").append(
                working.getParticleAmount().displayText()
        );
    }

    private ButtonWidget createShaderPackQualityButton(
            int x,
            int y,
            int width
    ) {
        return ButtonWidget.builder(
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
                .dimensions(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private Text shaderPackQualityButtonText() {
        return Text.translatable(
                "config.twilightteleport.shader_pack_quality"
        ).append(": ").append(
                working
                        .getShaderPackEffectQuality()
                        .displayText()
        );
    }

    private ButtonWidget createToggleButton(
            int x,
            int y,
            int width,
            String translationKey,
            boolean initialValue,
            java.util.function.Consumer<Boolean> setter
    ) {
        final boolean[] value = {initialValue};

        return ButtonWidget.builder(
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
                .dimensions(x, y, width, WIDGET_HEIGHT)
                .build();
    }

    private static Text toggleText(
            String translationKey,
            boolean enabled
    ) {
        return Text.translatable(translationKey)
                .append(": ")
                .append(Text.translatable(
                        enabled
                                ? "options.on"
                                : "options.off"
                ));
    }

    private void saveAndClose() {
        TwilightTeleportConfigManager.set(working);
        TwilightTeleportConfigManager.save();
        close();
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                width / 2,
                16,
                0xFFFFFF
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.translatable(
                        "config.twilightteleport.visual_section"
                ),
                Math.max(8, width / 2 - 160),
                30,
                0xA0A0A0
        );
    }
}
