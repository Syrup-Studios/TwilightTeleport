package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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
                    "render"
                            + "(Lnet/minecraft/entity/LivingEntity;FF"
                            + "Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void twilightTeleport$beginRenderedHeightMeasurement(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            TeleportRenderedHeightManager.beginPlayerRender(
                    player,
                    tickDelta,
                    player.getBodyYaw()
            );
        }
    }

    @Inject(
            method =
                    "setupTransforms"
                            + "(Lnet/minecraft/entity/LivingEntity;"
                            + "Lnet/minecraft/client/util/math/MatrixStack;"
                            + "FFFF)V",
            at = @At("HEAD")
    )
    private void twilightTeleport$captureActualBodyYaw(
            LivingEntity entity,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            float scale,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayerEntity player
                && TeleportRenderedHeightManager
                .isMeasurementActive(player.getUuid())) {
            TeleportRenderedHeightManager.updateMeasurementRenderYaw(
                    player.getUuid(),
                    bodyYaw
            );
        }
    }

    @Inject(
            method =
                    "render"
                            + "(Lnet/minecraft/entity/LivingEntity;FF"
                            + "Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void twilightTeleport$finishRenderedHeightMeasurement(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            TeleportRenderedHeightManager.endPlayerRender(player);
        }
    }

    
    @Inject(
            method =
                    "getRenderLayer"
                            + "(Lnet/minecraft/entity/LivingEntity;ZZZ)"
                            + "Lnet/minecraft/client/render/RenderLayer;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$useDissolveLayer(
            LivingEntity entity,
            boolean showBody,
            boolean translucent,
            boolean showOutline,
            CallbackInfoReturnable<RenderLayer> cir
    ) {
        if (!(entity
                instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        if (TeleportShaderPackCompat.isShadowPass()) {
            return;
        }

        if (!TeleportEntityEffectManager
                .shouldRenderDissolve(player.getUuid())) {
            return;
        }

        if (!TeleportRenderedHeightManager
                .ensureEffectSnapshotReady(player)) {
            return;
        }

        if (!((Object) this
                instanceof PlayerEntityRenderer playerRenderer)) {
            return;
        }

        MinecraftClient client =
                MinecraftClient.getInstance();

        float tickDelta =
                client.getRenderTickCounter()
                        .getTickDelta(false);

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
                        player.getUuid(),
                        playerRenderer.getTexture(player)
                )
        );
    }

    
    @Redirect(
            method =
                    "render"
                            + "(Lnet/minecraft/entity/LivingEntity;FF"
                            + "Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/"
                                    + "VertexConsumerProvider;getBuffer"
                                    + "(Lnet/minecraft/client/render/RenderLayer;)"
                                    + "Lnet/minecraft/client/render/VertexConsumer;"
            )
    )
    private VertexConsumer twilightTeleport$applyShaderPackFallbackToBody(
            VertexConsumerProvider provider,
            RenderLayer layer,
            LivingEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        VertexConsumer originalConsumer =
                provider.getBuffer(layer);

        if (!(entity
                instanceof AbstractClientPlayerEntity player)) {
            return originalConsumer;
        }

        if (TeleportShaderPackCompat.isShadowPass()) {
            return originalConsumer;
        }

        
        VertexConsumer measuringConsumer =
                TeleportRenderedHeightManager
                        .isMeasurementActive(
                                player.getUuid()
                        )
                        ? TeleportHeightMeasuringVertexConsumerProvider.wrap(
                        originalConsumer,
                        player.getUuid()
                )
                        : originalConsumer;

        if (!TeleportShaderPackCompat
                .isShaderPackInUse()) {
            return measuringConsumer;
        }

        if (!TeleportEntityEffectManager
                .shouldRenderDissolve(
                        player.getUuid()
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
                        player.getUuid()
                );

        return TeleportRenderedHeightManager
                .isMeasurementActive(player.getUuid())
                ? TeleportHeightMeasuringVertexConsumerProvider.wrap(
                shaderPackFallback,
                player.getUuid()
        )
                : shaderPackFallback;
    }

    
    @Redirect(
            method =
                    "render"
                            + "(Lnet/minecraft/entity/LivingEntity;FF"
                            + "Lnet/minecraft/client/util/math/MatrixStack;"
                            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/entity/feature/"
                                    + "FeatureRenderer;render"
                                    + "(Lnet/minecraft/client/util/math/MatrixStack;"
                                    + "Lnet/minecraft/client/render/VertexConsumerProvider;"
                                    + "ILnet/minecraft/entity/Entity;FFFFFF)V"
            )
    )
    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private void twilightTeleport$renderDissolvingFeatures(
            FeatureRenderer featureRenderer,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Entity entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        VertexConsumerProvider selectedProvider =
                vertexConsumers;

        if (entity instanceof AbstractClientPlayerEntity player
                && !TeleportShaderPackCompat.isShadowPass()) {
            boolean wantsDissolve =
                    TeleportEntityEffectManager
                            .shouldRenderDissolve(
                                    player.getUuid()
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
                                    player.getUuid()
                            );
                } else {
                    selectedProvider =
                            new TeleportDissolveVertexConsumerProvider(
                                    selectedProvider,
                                    player.getUuid()
                            );
                }
            }

            boolean measuresHeadSlot =
                    TeleportRenderedHeightManager
                            .isMeasurementActive(
                                    player.getUuid()
                            )
                            && !player.getEquippedStack(
                            EquipmentSlot.HEAD
                    ).isEmpty()
                            && (
                            featureRenderer
                                    instanceof ArmorFeatureRenderer
                                    || featureRenderer
                                    instanceof HeadFeatureRenderer
                    );

            if (measuresHeadSlot) {
                selectedProvider =
                        new TeleportHeightMeasuringVertexConsumerProvider(
                                selectedProvider,
                                player.getUuid()
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

    
    @Inject(
            method =
                    "getShadowRadius"
                            + "(Lnet/minecraft/entity/LivingEntity;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void twilightTeleport$hideShadow(
            LivingEntity entity,
            CallbackInfoReturnable<Float> cir
    ) {
        if (entity instanceof AbstractClientPlayerEntity player
                && TeleportEntityEffectManager
                .shouldRenderDissolve(player.getUuid())) {
            cir.setReturnValue(0.0F);
        }
    }
}
