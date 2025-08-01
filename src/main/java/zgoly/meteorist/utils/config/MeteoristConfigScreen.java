package zgoly.meteorist.utils.config;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

public class MeteoristConfigScreen {
    public static Setting<Boolean> showDevModules;

    public static void init() {
        SettingGroup sgMeteorist = Config.get().settings.createGroup("Meteorist");

        showDevModules = sgMeteorist.add(new BoolSetting.Builder()
                .name("show-dev-modules")
                .description("Show modules that are primarily intended for development. Not very useful for the average player.")
                .defaultValue(false)
                .build()
        );
    }
}
