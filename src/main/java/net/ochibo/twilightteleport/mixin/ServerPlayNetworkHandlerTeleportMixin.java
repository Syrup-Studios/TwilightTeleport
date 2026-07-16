package net.ochibo.twilightteleport.mixin;

import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerTeleportMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method =
                    "requestTeleport"
                            + "(DDDFFLjava/util/Set;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$interceptNetworkTeleport(
            double destinationX,
            double destinationY,
            double destinationZ,
            float yaw,
            float pitch,
            Set<PositionFlag> flags,
            CallbackInfo ci
    ) {
        ServerPlayNetworkHandler handler =
                (ServerPlayNetworkHandler) (Object) this;

        
        boolean intercepted =
                PendingTeleportManager
                        .interceptExternalTeleport(
                                player,
                                player.getServerWorld(),
                                destinationX,
                                destinationY,
                                destinationZ,
                                () -> handler.requestTeleport(
                                        destinationX,
                                        destinationY,
                                        destinationZ,
                                        yaw,
                                        pitch,
                                        Set.of()
                                )
                        );

        if (intercepted) {
            ci.cancel();
        }
    }
}
