package zgoly.meteorist.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minescript.common.JobControl;
import net.minescript.common.JobState;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.modules.minescript.RealMinescriptService;
import zgoly.meteorist.utils.MeteoristUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinescriptHud extends HudElement {
    public static final HudElementInfo<MinescriptHud> INFO = new HudElementInfo<>(Meteorist.HUD_GROUP, "minescript-scripts", "Displays the currently running Minescript scripts.", MinescriptHud::new);

    private static final boolean MINESCRIPT_PRESENT = detectMinescript();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Color");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Boolean> showTitle = sgGeneral.add(new BoolSetting.Builder()
            .name("show-title")
            .description("Shows the \"Scripts (N)\" title line.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showTime = sgGeneral.add(new BoolSetting.Builder()
            .name("show-time")
            .description("Shows the elapsed time of each running script.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showState = sgGeneral.add(new BoolSetting.Builder()
            .name("show-state")
            .description("Shows a marker next to suspended scripts.")
            .defaultValue(true)
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
    private final Setting<SettingColor> textColor = sgColor.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color of script names.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );
    private final Setting<SettingColor> infoColor = sgColor.add(new ColorSetting.Builder()
            .name("info-color")
            .description("Color of the elapsed time.")
            .defaultValue(new SettingColor(175, 175, 175))
            .visible(showTime::get)
            .build()
    );
    private final Setting<SettingColor> suspendedColor = sgColor.add(new ColorSetting.Builder()
            .name("suspended-color")
            .description("Color of the suspended marker.")
            .defaultValue(new SettingColor(255, 210, 25))
            .visible(showState::get)
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

    private final Map<Integer, Integer> tickCounts = new HashMap<>();
    private final List<JobData> scripts = new ArrayList<>();

    public MinescriptHud() {
        super(INFO);
    }

    private static boolean detectMinescript() {
        try {
            Class.forName("net.minescript.common.Minescript");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }

    private String title() {
        return "Scripts (" + scripts.size() + ")";
    }

    private String editorPlaceholder() {
        return MINESCRIPT_PRESENT ? "Scripts" : "Minescript not installed";
    }

    private String timeOf(JobData script) {
        return MeteoristUtils.ticksToTime(tickCounts.getOrDefault(script.id(), 0), true, false);
    }

    private double lineWidth(HudRenderer renderer, JobData script, double scale) {
        double width = renderer.textWidth(script.name(), shadow.get(), scale);

        if (showTime.get()) {
            width += renderer.textWidth(" " + timeOf(script), shadow.get(), scale);
        }

        if (showState.get() && script.suspended()) {
            width += renderer.textWidth(" [Suspended]", shadow.get(), scale);
        }

        return width;
    }

    @Override
    public void tick(HudRenderer renderer) {
        scripts.clear();

        if (MINESCRIPT_PRESENT) {
            Set<Integer> ids = new HashSet<>();

            for (Map.Entry<Integer, JobControl> entry : RealMinescriptService.getJobMap().entrySet()) {
                JobControl job = entry.getValue();
                JobState state = job.state();
                int id = entry.getKey();
                ids.add(id);

                if (state == JobState.KILLED) {
                    tickCounts.remove(id);
                    continue;
                }

                if (state != JobState.SUSPENDED && state != JobState.DONE) {
                    tickCounts.merge(id, 1, Integer::sum);
                }

                scripts.add(new JobData(id, job.jobSummary(), state == JobState.SUSPENDED));
            }

            tickCounts.keySet().removeIf(id -> !ids.contains(id));
        }

        double scale = getScale();
        double lineHeight = renderer.textHeight(shadow.get(), scale);
        double width = 0;
        double height = 0;

        if (showTitle.get()) {
            width = Math.max(width, renderer.textWidth(title(), shadow.get(), scale));
            height += lineHeight;
        }

        for (JobData script : scripts) {
            width = Math.max(width, lineWidth(renderer, script, scale));
            height += lineHeight;
        }

        if (scripts.isEmpty() && isInEditor()) {
            width = Math.max(width, renderer.textWidth(editorPlaceholder(), shadow.get(), scale));
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
            renderer.text(title(), x, y, titleColor.get(), shadow.get(), scale);
            y += lineHeight;
        }

        if (scripts.isEmpty()) {
            if (isInEditor()) renderer.text(editorPlaceholder(), x, y, titleColor.get(), shadow.get(), scale);
            return;
        }

        for (JobData script : scripts) {
            double xOffset = x;

            renderer.text(script.name(), xOffset, y, textColor.get(), shadow.get(), scale);
            xOffset += renderer.textWidth(script.name(), shadow.get(), scale);

            if (showTime.get()) {
                String time = " " + timeOf(script);
                renderer.text(time, xOffset, y, infoColor.get(), shadow.get(), scale);
                xOffset += renderer.textWidth(time, shadow.get(), scale);
            }

            if (showState.get() && script.suspended()) {
                renderer.text(" [Suspended]", xOffset, y, suspendedColor.get(), shadow.get(), scale);
            }

            y += lineHeight;
        }
    }

    private record JobData(int id, String name, boolean suspended) {
    }
}