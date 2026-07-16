package net.ochibo.twilightteleport.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class TeleportDissolveShaders {
    private static final Identifier PROGRAM_ID =
            Identifier.of("twilightteleport", "teleport_dissolve");

    @Nullable
    private static ShaderProgram program;

    private TeleportDissolveShaders() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(
                        PROGRAM_ID,
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        loadedProgram -> program = loadedProgram
                )
        );
    }

    @Nullable
    public static ShaderProgram getProgram() {
        return program;
    }
}
