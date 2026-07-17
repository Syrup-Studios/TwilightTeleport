package net.ochibo.twilightteleport.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityTeleportMixin {

    
    @Inject(
            method =
                    "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$interceptWaystonesDimensionTeleport(
            ServerLevel destinationWorld,
            double destinationX,
            double destinationY,
            double destinationZ,
            float yaw,
            float pitch,
            CallbackInfo ci
    ) {
        ServerPlayer player =
                (ServerPlayer) (Object) this;

        boolean intercepted =
                PendingTeleportManager
                        .interceptExternalTeleport(
                                player,
                                destinationWorld,
                                destinationX,
                                destinationY,
                                destinationZ,
                                () -> player.teleportTo(
                                        destinationWorld,
                                        destinationX,
                                        destinationY,
                                        destinationZ,
                                        yaw,
                                        pitch
                                )
                        );

        if (intercepted) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$interceptExternalTeleport(
            ServerLevel destinationWorld,
            double destinationX,
            double destinationY,
            double destinationZ,
            Set<RelativeMovement> flags,
            float yaw,
            float pitch,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player =
                (ServerPlayer) (Object) this;

        
        boolean intercepted =
                PendingTeleportManager
                        .interceptExternalTeleport(
                                player,
                                destinationWorld,
                                destinationX,
                                destinationY,
                                destinationZ,
                                () -> player.teleportTo(
                                        destinationWorld,
                                        destinationX,
                                        destinationY,
                                        destinationZ,
                                        Set.of(),
                                        yaw,
                                        pitch
                                )
                        );

        if (intercepted) {
            cir.setReturnValue(true);
        }
    }
}
