package net.ochibo.twilightteleport.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.ochibo.twilightteleport.config.TwilightTeleportConfigManager;
import net.ochibo.twilightteleport.network.TeleportEffectAction;
import net.ochibo.twilightteleport.network.TeleportEffectPayload;

public final class TeleportEffectSoundPlayer {

    
    private static final SoundEvent DISSOLVE_SOUND =
            SoundEvents.BLOCK_BEACON_ACTIVATE;

    private static final SoundEvent REBUILD_SOUND =
            SoundEvents.BLOCK_BEACON_DEACTIVATE;

    private static final float DISSOLVE_VOLUME = 0.85F;
    private static final float DISSOLVE_PITCH = 0.62F;

    private static final float REBUILD_VOLUME = 0.90F;
    private static final float REBUILD_PITCH = 0.6F;

    private TeleportEffectSoundPlayer() {
    }

    public static void play(
            MinecraftClient client,
            TeleportEffectPayload payload
    ) {
        if (!TwilightTeleportConfigManager.get().isSoundEnabled()
                || client.world == null
                || payload.elapsedTicks() != 0) {
            return;
        }

        SoundEvent sound;
        float volume;
        float pitch;

        if (payload.action()
                == TeleportEffectAction.START_DISSOLVE) {
            sound = DISSOLVE_SOUND;
            volume = DISSOLVE_VOLUME;
            pitch = DISSOLVE_PITCH;
        } else if (payload.action()
                == TeleportEffectAction.START_REBUILD) {
            sound = REBUILD_SOUND;
            volume = REBUILD_VOLUME;
            pitch = REBUILD_PITCH;
        } else {
            return;
        }

        PlayerEntity target =
                client.world.getPlayerByUuid(
                        payload.playerUuid()
                );

        if (target == null) {
            return;
        }

        client.world.playSound(
                target.getX(),
                target.getY()
                        + target.getHeight() * 0.5D,
                target.getZ(),
                sound,
                SoundCategory.PLAYERS,
                volume,
                pitch,
                false
        );
    }
}
