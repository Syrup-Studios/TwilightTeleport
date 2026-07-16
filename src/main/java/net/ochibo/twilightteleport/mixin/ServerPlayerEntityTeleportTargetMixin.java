package net.ochibo.twilightteleport.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.TeleportTarget;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityTeleportTargetMixin {

    @Inject(
            method =
                    "teleportTo"
                            + "(Lnet/minecraft/world/TeleportTarget;)"
                            + "Lnet/minecraft/entity/Entity;",
            at = @At("HEAD")
    )
    private void twilightTeleport$beginTeleportTargetTransition(
            TeleportTarget teleportTarget,
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .beginTeleportTargetTransition(
                        (ServerPlayerEntity) (Object) this
                );
    }

    @Inject(
            method =
                    "teleportTo"
                            + "(Lnet/minecraft/world/TeleportTarget;)"
                            + "Lnet/minecraft/entity/Entity;",
            at = @At("RETURN")
    )
    private void twilightTeleport$endTeleportTargetTransition(
            TeleportTarget teleportTarget,
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .endTeleportTargetTransition(
                        (ServerPlayerEntity) (Object) this
                );
    }
}
