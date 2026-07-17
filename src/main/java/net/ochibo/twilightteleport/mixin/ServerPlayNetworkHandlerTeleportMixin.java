package net.ochibo.twilightteleport.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerTeleportMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method =
                    "teleport(DDDFFLjava/util/Set;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$interceptNetworkTeleport(
            double destinationX,
            double destinationY,
            double destinationZ,
            float yaw,
            float pitch,
            Set<RelativeMovement> flags,
            CallbackInfo ci
    ) {
        ServerGamePacketListenerImpl handler =
                (ServerGamePacketListenerImpl) (Object) this;

        
        boolean intercepted =
                PendingTeleportManager
                        .interceptExternalTeleport(
                                player,
                                player.serverLevel(),
                                destinationX,
                                destinationY,
                                destinationZ,
                                () -> handler.teleport(
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
