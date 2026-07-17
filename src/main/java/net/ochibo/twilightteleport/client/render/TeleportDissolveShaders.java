package net.ochibo.twilightteleport.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TeleportDissolveShaders {
    private static final ResourceLocation PROGRAM_ID =
            ResourceLocation.fromNamespaceAndPath("twilightteleport", "teleport_dissolve");

    @Nullable
    private static ShaderInstance program;

    private TeleportDissolveShaders() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(
                        PROGRAM_ID,
                        DefaultVertexFormat.NEW_ENTITY,
                        loadedProgram -> program = loadedProgram
                )
        );
    }

    @Nullable
    public static ShaderInstance getProgram() {
        return program;
    }
}
