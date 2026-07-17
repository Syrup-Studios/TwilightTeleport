package net.ochibo.twilightteleport;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModParticles {

    public static final SimpleParticleType TELEPORT_SHARD =
            FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        TwilightTeleport.MOD_ID,
                        "teleport_shard"
                ),
                TELEPORT_SHARD
        );
    }
}
