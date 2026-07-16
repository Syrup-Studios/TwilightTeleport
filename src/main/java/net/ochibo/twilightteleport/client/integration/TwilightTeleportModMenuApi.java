package net.ochibo.twilightteleport.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.ochibo.twilightteleport.client.config.TwilightTeleportConfigScreen;

public final class TwilightTeleportModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TwilightTeleportConfigScreen::new;
    }
}
