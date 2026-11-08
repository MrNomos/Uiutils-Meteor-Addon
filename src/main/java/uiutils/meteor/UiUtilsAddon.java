package uiutils.meteor;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import uiutils.meteor.modules.UiUtilsModule;

public class UiUtilsAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new UiUtilsModule());
    }

    @Override
    public String getPackage() {
        return "uiutils.meteor";
    }
}
