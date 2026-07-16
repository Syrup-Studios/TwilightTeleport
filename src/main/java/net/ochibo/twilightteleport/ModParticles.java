package net.ochibo.twilightteleport;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {

    public static final SimpleParticleType TELEPORT_SHARD =
            FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void register() {
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(
                        TwilightTeleport.MOD_ID,
                        "teleport_shard"
                ),
                TELEPORT_SHARD
        );
    }
}
