package net.ochibo.twilightteleport.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.ochibo.twilightteleport.client.effect.TeleportEntityEffectManager;
import net.ochibo.twilightteleport.client.render.TeleportDissolveRenderLayer;
import net.ochibo.twilightteleport.client.render.TeleportDissolveRenderState;
import net.ochibo.twilightteleport.client.render.TeleportDissolveVertexConsumerProvider;
import net.ochibo.twilightteleport.client.render.TeleportHeightMeasuringVertexConsumerProvider;
import net.ochibo.twilightteleport.client.render.TeleportRenderedHeightManager;
import net.ochibo.twilightteleport.client.render.TeleportShaderPackCompat;
import net.ochibo.twilightteleport.client.render.TeleportShaderPackFallbackVertexConsumer;
import net.ochibo.twilightteleport.client.render.TeleportShaderPackFallbackVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void twilightTeleport$beginRenderedHeightMeasurement(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayer player) {
            TeleportRenderedHeightManager.beginPlayerRender(
                    player,
                    tickDelta,
                    player.getVisualRotationYInDegrees()
            );
        }
    }

    @Inject(
            //? if >=1.20.5 {
            method =
                    "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            //?} else {
            /*method =
                    "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            *///?}
            at = @At("HEAD")
    )
    private void twilightTeleport$captureActualBodyYaw(
            LivingEntity entity,
            PoseStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            //? if >=1.20.5
            float scale,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayer player
                && TeleportRenderedHeightManager
                .isMeasurementActive(player.getUUID())) {
            TeleportRenderedHeightManager.updateMeasurementRenderYaw(
                    player.getUUID(),
                    bodyYaw
            );
        }
    }

    @Inject(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void twilightTeleport$finishRenderedHeightMeasurement(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayer player) {
            TeleportRenderedHeightManager.endPlayerRender(player);
        }
    }

    @Inject(
            method =
                    "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$useDissolveLayer(
            LivingEntity entity,
            boolean showBody,
            boolean translucent,
            boolean showOutline,
            CallbackInfoReturnable<RenderType> cir
    ) {
        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return;
        }

        if (TeleportShaderPackCompat.isShadowPass()) {
            return;
        }

        if (!TeleportEntityEffectManager
                .shouldRenderDissolve(player.getUUID())) {
            return;
        }

        if (!TeleportRenderedHeightManager
                .ensureEffectSnapshotReady(player)) {
            return;
        }

        if (!((Object) this
                instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        //? if >=1.20.5 {
        float tickDelta =
                client.getTimer()
                        .getGameTimeDeltaPartialTick(false);
        //?} else {
        /*float tickDelta = client.getFrameTime();
        *///?}

        TeleportDissolveRenderState.prepare(
                player,
                tickDelta
        );

        if (TeleportShaderPackCompat
                .isShaderPackInUse()) {
            
            return;
        }

        cir.setReturnValue(
                TeleportDissolveRenderLayer.get(
                        player.getUUID(),
                        playerRenderer.getTextureLocation(player)
                )
        );
    }

    
    @Redirect(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer twilightTeleport$applyShaderPackFallbackToBody(
            MultiBufferSource provider,
            RenderType layer,
            LivingEntity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light
    ) {
        VertexConsumer originalConsumer =
                provider.getBuffer(layer);

        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return originalConsumer;
        }

        if (TeleportShaderPackCompat.isShadowPass()) {
            return originalConsumer;
        }

        
        VertexConsumer measuringConsumer =
                TeleportRenderedHeightManager
                        .isMeasurementActive(
                                player.getUUID()
                        )
                        ? TeleportHeightMeasuringVertexConsumerProvider.wrap(
                        originalConsumer,
                        player.getUUID()
                )
                        : originalConsumer;

        if (!TeleportShaderPackCompat
                .isShaderPackInUse()) {
            return measuringConsumer;
        }

        if (!TeleportEntityEffectManager
                .shouldRenderDissolve(
                        player.getUUID()
                )) {
            return measuringConsumer;
        }

        if (!TeleportRenderedHeightManager
                .ensureEffectSnapshotReady(player)) {
            
            return measuringConsumer;
        }

        TeleportDissolveRenderState.prepare(
                player,
                tickDelta
        );

        
        VertexConsumer shaderPackFallback =
                new TeleportShaderPackFallbackVertexConsumer(
                        originalConsumer,
                        player.getUUID()
                );

        return TeleportRenderedHeightManager
                .isMeasurementActive(player.getUUID())
                ? TeleportHeightMeasuringVertexConsumerProvider.wrap(
                shaderPackFallback,
                player.getUUID()
        )
                : shaderPackFallback;
    }

    
    @Redirect(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"
            )
    )
    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private void twilightTeleport$renderDissolvingFeatures(
            RenderLayer featureRenderer,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            Entity entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        MultiBufferSource selectedProvider =
                vertexConsumers;

        if (entity instanceof AbstractClientPlayer player
                && !TeleportShaderPackCompat.isShadowPass()) {
            boolean wantsDissolve =
                    TeleportEntityEffectManager
                            .shouldRenderDissolve(
                                    player.getUUID()
                            );

            boolean snapshotReady =
                    !wantsDissolve
                            || TeleportRenderedHeightManager
                            .ensureEffectSnapshotReady(player);

            if (wantsDissolve && snapshotReady) {
                TeleportDissolveRenderState.prepare(
                        player,
                        tickDelta
                );

                if (TeleportShaderPackCompat
                        .isShaderPackInUse()) {
                    selectedProvider =
                            new TeleportShaderPackFallbackVertexConsumerProvider(
                                    selectedProvider,
                                    player.getUUID()
                            );
                } else {
                    selectedProvider =
                            new TeleportDissolveVertexConsumerProvider(
                                    selectedProvider,
                                    player.getUUID()
                            );
                }
            }

            boolean measuresHeadSlot =
                    TeleportRenderedHeightManager
                            .isMeasurementActive(
                                    player.getUUID()
                            )
                            && !player.getItemBySlot(
                            EquipmentSlot.HEAD
                    ).isEmpty()
                            && (
                            featureRenderer
                                    instanceof HumanoidArmorLayer
                                    || featureRenderer
                                    instanceof CustomHeadLayer
                    );

            if (measuresHeadSlot) {
                selectedProvider =
                        new TeleportHeightMeasuringVertexConsumerProvider(
                                selectedProvider,
                                player.getUUID()
                        );
            }
        }

        featureRenderer.render(
                matrices,
                selectedProvider,
                light,
                entity,
                limbAngle,
                limbDistance,
                tickDelta,
                animationProgress,
                headYaw,
                headPitch
        );
    }

    
    //? if >=1.20.5 {
    @Inject(
            method =
                    "getShadowRadius(Lnet/minecraft/world/entity/LivingEntity;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$hideShadow(
            LivingEntity entity,
            CallbackInfoReturnable<Float> cir
    ) {
        if (entity instanceof AbstractClientPlayer player
                && TeleportEntityEffectManager
                .shouldRenderDissolve(player.getUUID())) {
            cir.setReturnValue(0.0F);
        }
    }
    //?}
}
