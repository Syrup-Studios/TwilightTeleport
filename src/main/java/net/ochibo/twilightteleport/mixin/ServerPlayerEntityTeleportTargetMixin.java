package net.ochibo.twilightteleport.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityTeleportTargetMixin {

    @Inject(
            method =
                    "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD")
    )
    private void twilightTeleport$beginTeleportTargetTransition(
            DimensionTransition teleportTarget,
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .beginTeleportTargetTransition(
                        (ServerPlayer) (Object) this
                );
    }

    @Inject(
            method =
                    "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN")
    )
    private void twilightTeleport$endTeleportTargetTransition(
            DimensionTransition teleportTarget,
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .endTeleportTargetTransition(
                        (ServerPlayer) (Object) this
                );
    }
}
