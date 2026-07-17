package net.ochibo.twilightteleport.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setPosition")
    void twilightTeleport$setPos(Vec3 pos);

    @Invoker("setRotation")
    void twilightTeleport$setRotation(float yaw, float pitch);
}