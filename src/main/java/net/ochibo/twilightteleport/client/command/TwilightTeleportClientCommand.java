package net.ochibo.twilightteleport.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.ochibo.twilightteleport.config.ParticleAmount;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;

public final class TwilightTeleportClientCommand {

    private TwilightTeleportClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) ->
                        dispatcher.register(
                                ClientCommandManager.literal("ttp")
                                        .then(createParticleCommand())
                                        .then(
                                                ClientCommandManager
                                                        .literal("reset")
                                                        .executes(context ->
                                                                resetConfig(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )
                        )
        );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource>
    createParticleCommand() {
        return ClientCommandManager.literal("particle")
                .then(particleOption("none", ParticleAmount.NONE))
                .then(particleOption("nothing", ParticleAmount.NONE))
                .then(particleOption("few", ParticleAmount.FEW))
                .then(particleOption("default", ParticleAmount.DEFAULT));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource>
    particleOption(
            String literal,
            ParticleAmount amount
    ) {
        return ClientCommandManager.literal(literal)
                .executes(context -> setParticleAmount(
                        context.getSource(),
                        amount
                ));
    }

    private static int setParticleAmount(
            FabricClientCommandSource source,
            ParticleAmount amount
    ) {
        TwilightTeleportConfigManager
                .get()
                .setParticleAmount(amount);
        TwilightTeleportConfigManager.save();

        source.sendFeedback(
                Text.translatable(
                        "command.twilightteleport.particle_set",
                        amount.displayText()
                )
        );

        return 1;
    }

    private static int resetConfig(
            FabricClientCommandSource source
    ) {
        TwilightTeleportConfigManager.reset();

        source.sendFeedback(
                Text.translatable(
                        "command.twilightteleport.reset"
                )
        );

        return 1;
    }
}
