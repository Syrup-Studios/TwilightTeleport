package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setPos")
    void twilightTeleport$setPos(Vec3d pos);

    @Invoker("setRotation")
    void twilightTeleport$setRotation(float yaw, float pitch);
}