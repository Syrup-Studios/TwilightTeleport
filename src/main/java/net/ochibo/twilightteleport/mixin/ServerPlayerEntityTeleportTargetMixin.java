package net.ochibo.twilightteleport.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
//? if >=1.20.5 {
import net.minecraft.world.level.portal.DimensionTransition;
//?} else {
/*import net.minecraft.server.level.ServerLevel;
*///?}
import net.ochibo.twilightteleport.server.PendingTeleportManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityTeleportTargetMixin {

    @Inject(
            //? if >=1.20.5 {
            method =
                    "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            //?} else {
            /*method =
                    "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;",
            *///?}
            at = @At("HEAD")
    )
    private void twilightTeleport$beginTeleportTargetTransition(
            //? if >=1.20.5 {
            DimensionTransition teleportTarget,
            //?} else {
            /*ServerLevel teleportTarget,
            *///?}
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .beginTeleportTargetTransition(
                        (ServerPlayer) (Object) this
                );
    }

    @Inject(
            //? if >=1.20.5 {
            method =
                    "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            //?} else {
            /*method =
                    "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;",
            *///?}
            at = @At("RETURN")
    )
    private void twilightTeleport$endTeleportTargetTransition(
            //? if >=1.20.5 {
            DimensionTransition teleportTarget,
            //?} else {
            /*ServerLevel teleportTarget,
            *///?}
            CallbackInfoReturnable<Entity> cir
    ) {
        PendingTeleportManager
                .endTeleportTargetTransition(
                        (ServerPlayer) (Object) this
                );
    }
}
