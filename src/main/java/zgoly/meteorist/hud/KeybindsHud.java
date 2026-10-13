package zgoly.meteorist.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.KeybindUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class KeybindsHud extends HudElement {
    public static final HudElementInfo<KeybindsHud> INFO = new HudElementInfo<>(Meteorist.HUD_GROUP, "keybinds", "Displays all occupied module keybinds.", KeybindsHud::new);

    public enum Sort {
        Alphabetical,
        Category,
        Key
    }

    public enum ColorMode {
        Module,
        Flat,
        Rainbow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgColor = settings.createGroup("Color");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Boolean> showTitle = sgGeneral.add(new BoolSetting.Builder()
            .name("show-title")
            .description("Shows the \"Bound (N)\" title line.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showMeteorBinds = sgGeneral.add(new BoolSetting.Builder()
            .name("show-meteor-binds")
            .description("Shows keybinds of Meteor modules.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showMcBinds = sgGeneral.add(new BoolSetting.Builder()
            .name("show-minecraft-binds")
            .description("Shows keybinds of vanilla Minecraft controls.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> onlyActive = sgGeneral.add(new BoolSetting.Builder()
            .name("only-active")
            .description("Only shows keybinds of toggled on modules.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Sort> sort = sgGeneral.add(new EnumSetting.Builder<Sort>()
            .name("sort")
            .description("How to sort the list.")
            .defaultValue(Sort.Alphabetical)
            .build()
    );
    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
            .name("alignment")
            .description("Horizontal alignment.")
            .defaultValue(Alignment.Auto)
            .build()
    );
    private final Setting<List<Module>> hiddenModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("hidden-modules")
            .description("Which modules not to show in the list.")
            .build()
    );

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(customScale::get)
            .defaultValue(1)
            .min(0.5)
            .sliderRange(0.5, 3)
            .build()
    );

    private final Setting<Boolean> shadow = sgColor.add(new BoolSetting.Builder()
            .name("shadow")
            .description("Renders shadow behind text.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> titleColor = sgColor.add(new ColorSetting.Builder()
            .name("title-color")
            .description("Color of the title line.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(showTitle::get)
            .build()
    );
    private final Setting<ColorMode> colorMode = sgColor.add(new EnumSetting.Builder<ColorMode>()
            .name("color-mode")
            .description("What color to use for bind names.")
            .defaultValue(ColorMode.Module)
            .build()
    );
    private final Setting<SettingColor> flatColor = sgColor.add(new ColorSetting.Builder()
            .name("flat-color")
            .description("Color of bind names in flat color mode, and of Minecraft binds.")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> colorMode.get() != ColorMode.Rainbow)
            .build()
    );
    private final Setting<SettingColor> keyColor = sgColor.add(new ColorSetting.Builder()
            .name("key-color")
            .description("Color of the \"- Key\" part of each line.")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );
    private final Setting<Double> rainbowSpeed = sgColor.add(new DoubleSetting.Builder()
            .name("rainbow-speed")
            .description("Rainbow speed of rainbow color mode.")
            .defaultValue(0.05)
            .sliderMin(0.01)
            .sliderMax(0.2)
            .decimalPlaces(4)
            .visible(() -> colorMode.get() == ColorMode.Rainbow)
            .build()
    );
    private final Setting<Double> rainbowSpread = sgColor.add(new DoubleSetting.Builder()
            .name("rainbow-spread")
            .description("Rainbow spread of rainbow color mode.")
            .defaultValue(0.01)
            .sliderMin(0.001)
            .sliderMax(0.05)
            .decimalPlaces(4)
            .visible(() -> colorMode.get() == ColorMode.Rainbow)
            .build()
    );
    private final Setting<Double> rainbowSaturation = sgColor.add(new DoubleSetting.Builder()
            .name("rainbow-saturation")
            .defaultValue(1.0d)
            .sliderRange(0.0d, 1.0d)
            .visible(() -> colorMode.get() == ColorMode.Rainbow)
            .build()
    );
    private final Setting<Double> rainbowBrightness = sgColor.add(new DoubleSetting.Builder()
            .name("rainbow-brightness")
            .defaultValue(1.0d)
            .sliderRange(0.0d, 1.0d)
            .visible(() -> colorMode.get() == ColorMode.Rainbow)
            .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
            .name("background")
            .description("Displays background.")
            .defaultValue(false)
            .build()
    );
    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
    );

    private final List<KeybindUtils.Bind> binds = new ArrayList<>();

    private final Color rainbow = new Color(255, 255, 255);
    private double rainbowHue1;
    private double rainbowHue2;

    public KeybindsHud() {
        super(INFO);
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }

    private String categoryOf(KeybindUtils.Bind bind) {
        return bind.meteor() ? bind.module().category.name : "Minecraft";
    }

    @Override
    public void tick(HudRenderer renderer) {
        binds.clear();

        for (KeybindUtils.Bind bind : KeybindUtils.getBinds(showMeteorBinds.get(), showMcBinds.get())) {
            if (onlyActive.get() && bind.meteor() && !bind.module().isActive()) continue;
            if (bind.meteor() && hiddenModules.get().contains(bind.module())) continue;
            binds.add(bind);
        }

        switch (sort.get()) {
            case Alphabetical -> binds.sort(Comparator.comparing(bind -> bind.title().toLowerCase(Locale.ROOT)));
            case Category -> binds.sort(Comparator.comparing(bind -> categoryOf(bind) + bind.title().toLowerCase(Locale.ROOT)));
            case Key -> binds.sort(Comparator.comparing(KeybindUtils.Bind::keyName, String.CASE_INSENSITIVE_ORDER));
        }

        double scale = getScale();
        double lineHeight = renderer.textHeight(shadow.get(), scale);
        double width = 0;
        double height = 0;

        if (showTitle.get()) {
            width = Math.max(width, renderer.textWidth(title(), shadow.get(), scale));
            height += lineHeight;
        }

        for (KeybindUtils.Bind bind : binds) {
            width = Math.max(width, renderer.textWidth(bind.title(), shadow.get(), scale) + renderer.textWidth(" - " + bind.keyName(), shadow.get(), scale));
            height += lineHeight;
        }

        if (binds.isEmpty() && isInEditor()) {
            width = Math.max(width, renderer.textWidth("Keybinds", shadow.get(), scale));
            height += lineHeight;
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double scale = getScale();
        double lineHeight = renderer.textHeight(shadow.get(), scale);
        double x = this.x;
        double y = this.y;

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        }

        if (showTitle.get()) {
            double titleWidth = renderer.textWidth(title(), shadow.get(), scale);
            renderer.text(title(), x + alignX(titleWidth, alignment.get()), y, titleColor.get(), shadow.get(), scale);
            y += lineHeight;
        }

        if (binds.isEmpty()) {
            if (isInEditor()) {
                double placeholderWidth = renderer.textWidth("Keybinds", shadow.get(), scale);
                renderer.text("Keybinds", x + alignX(placeholderWidth, alignment.get()), y, titleColor.get(), shadow.get(), scale);
            }
            return;
        }

        if (colorMode.get() == ColorMode.Rainbow) {
            rainbowHue1 += rainbowSpeed.get() * renderer.delta;
            if (rainbowHue1 > 1) rainbowHue1 -= 1;
            else if (rainbowHue1 < -1) rainbowHue1 += 1;

            rainbowHue2 = rainbowHue1;
        }

        for (KeybindUtils.Bind bind : binds) {
            if (colorMode.get() == ColorMode.Rainbow) rainbowHue2 += rainbowSpread.get();

            double nameWidth = renderer.textWidth(bind.title(), shadow.get(), scale);
            double keyWidth = renderer.textWidth(" - " + bind.keyName(), shadow.get(), scale);
            double lineX = x + alignX(nameWidth + keyWidth, alignment.get());

            renderer.text(bind.title(), lineX, y, nameColor(bind), shadow.get(), scale);
            renderer.text(" - " + bind.keyName(), lineX + nameWidth, y, keyColor.get(), shadow.get(), scale);

            y += lineHeight;
        }
    }

    private Color nameColor(KeybindUtils.Bind bind) {
        return switch (colorMode.get()) {
            case Module -> bind.meteor() && bind.color() != null ? bind.color() : flatColor.get();
            case Flat -> flatColor.get();
            case Rainbow -> rainbowAt(rainbowHue2);
        };
    }

    private Color rainbowAt(double hue) {
        int c = java.awt.Color.HSBtoRGB((float) hue, rainbowSaturation.get().floatValue(), rainbowBrightness.get().floatValue());
        rainbow.r = Color.toRGBAR(c);
        rainbow.g = Color.toRGBAG(c);
        rainbow.b = Color.toRGBAB(c);
        return rainbow;
    }

    private String title() {
        return "Bound (" + binds.size() + ")";
    }
}