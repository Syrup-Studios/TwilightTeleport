package net.ochibo.twilightteleport.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {

    @Unique
    private ServerPlayer twilightTeleport$teleportingPlayer;

    @Inject(
            method =
                    "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("HEAD")
    )
    private void twilightTeleport$beginTeleportInterceptionBypass(
            HitResult hitResult,
            CallbackInfo ci
    ) {
        Entity owner =
                ((ThrownEnderpearl) (Object) this).getOwner();

        if (owner instanceof ServerPlayer player) {
            twilightTeleport$teleportingPlayer = player;
            PendingTeleportManager
                    .beginTeleportInterceptionBypass(player);
        }
    }

    @Inject(
            method =
                    "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("RETURN")
    )
    private void twilightTeleport$endTeleportInterceptionBypass(
            HitResult hitResult,
            CallbackInfo ci
    ) {
        if (twilightTeleport$teleportingPlayer == null) {
            return;
        }

        PendingTeleportManager
                .endTeleportInterceptionBypass(
                        twilightTeleport$teleportingPlayer
                );
        twilightTeleport$teleportingPlayer = null;
    }
}
