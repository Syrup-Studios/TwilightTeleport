package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//? if <1.20.5 {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
*///?}
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherShadowMixin {

    //? if <1.20.5 {
    /*@ModifyArgs(
            method =
                    "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"
            )
    )
    private void twilightTeleport$hideShadow(Args args) {
        Entity entity = args.get(2);

        if (entity instanceof AbstractClientPlayer player
                && TeleportEntityEffectManager
                .shouldRenderDissolve(player.getUUID())) {
            args.set(6, 0.0F);
        }
    }
    *///?}
}
