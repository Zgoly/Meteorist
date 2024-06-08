package zgoly.meteorist.hud;

import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import zgoly.meteorist.Meteorist;

public class TextPresets {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Meteorist.HUD_GROUP, "meteorist", "Additional presets for Meteor.", TextPresets::create);

    static {
        addPreset("Fall Distance", "Fall distance: #1{round(meteorist.fall_distance, 1)}");
        addPreset("Max Fall Distance", "Max fall distance: #1{round(meteorist.max_fall_distance, 1)}");
        addPreset("Fall Damage", "Fall damage: #1{round(meteorist.fall_damage) / 2} ❤");
        addPreset("Max Fall Damage", "Max fall damage: #1{round(meteorist.max_fall_damage) / 2} ❤");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static void addPreset(String title, String text) {
        INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
            textHud.updateDelay.set(0);
        });
    }
}