package zgoly.meteorist.utils.config;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.IdentityHashMap;
import java.util.Map;

public class MeteoristGui {
    public static MeteorGuiTheme theme;
    public static Setting<Boolean> imNothingLikeYall;
    public static Setting<SettingColor> accent;
    public static Setting<SettingColor> background;
    public static Setting<SettingColor> moduleBackground;

    private static final Map<GuiTheme, Boolean> initialized = new IdentityHashMap<>();

    public static void init(GuiTheme guiTheme) {
        if (initialized.containsKey(guiTheme)) return;

        if (guiTheme instanceof MeteorGuiTheme mt) {
            theme = mt;
        }

        if (imNothingLikeYall == null) {
            imNothingLikeYall = new BoolSetting.Builder()
                    .name("i'm-nothing-like-yall-mode")
                    .description("Use custom colors for the Meteorist category.")
                    .defaultValue(false)
                    .build();

            accent = new ColorSetting.Builder()
                    .name("accent-color")
                    .description("Header background color.")
                    .defaultValue(new SettingColor(0, 128, 255))
                    .visible(imNothingLikeYall::get)
                    .build();

            background = new ColorSetting.Builder()
                    .name("background-color")
                    .description("Window body background color.")
                    .defaultValue(new SettingColor(0, 25, 50, 200))
                    .visible(imNothingLikeYall::get)
                    .build();

            moduleBackground = new ColorSetting.Builder()
                    .name("module-background")
                    .description("Active module background color.")
                    .defaultValue(new SettingColor(0, 40, 80))
                    .visible(imNothingLikeYall::get)
                    .build();
        }

        SettingGroup sg = guiTheme.settings.createGroup("Meteorist");
        sg.add(imNothingLikeYall);
        sg.add(accent);
        sg.add(background);
        sg.add(moduleBackground);

        initialized.put(guiTheme, true);
    }
}
