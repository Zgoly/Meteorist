package zgoly.meteorist.modules.walkerist;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WBlockPosEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.pathfinder.Path;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.config.MeteoristConfigManager;
import zgoly.meteorist.utils.movement.WalkeristUtils;

import java.util.ArrayList;
import java.util.List;

import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class Walkerist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPathfinding = settings.createGroup("Pathfinding");
    private final SettingGroup sgCleanup = settings.createGroup("Cleanup");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");
    private final SettingGroup sgLogging = settings.createGroup("Logging");

    public final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
            .name("sprint")
            .description("Sprint while walking.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> water = sgGeneral.add(new BoolSetting.Builder()
            .name("water")
            .description("Walk through water.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> stealControl = sgGeneral.add(new BoolSetting.Builder()
            .name("steal-control")
            .description("Steal control from the player while walking and jump/interact for you.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> autoPlay = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-play")
            .description("Automatically advance through the queue after arriving at a waypoint.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> doors = sgGeneral.add(new BoolSetting.Builder()
            .name("open-doors")
            .description("Open doors that are in the way.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> rails = sgGeneral.add(new BoolSetting.Builder()
            .name("walk-on-rails")
            .description("Walk over rails instead of treating them as an obstacle.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> range = sgPathfinding.add(new IntSetting.Builder()
            .name("range")
            .description("Maximum distance in blocks the pathfinder is allowed to search from the player.")
            .defaultValue(64)
            .min(1)
            .sliderRange(1, 512)
            .build()
    );
    public final Setting<Integer> maxNodes = sgPathfinding.add(new IntSetting.Builder()
            .name("max-nodes")
            .description("Maximum number of nodes the pathfinder is allowed to visit.")
            .defaultValue(8192)
            .min(1)
            .sliderRange(100, 50000)
            .build()
    );
    public final Setting<Integer> maxStuckTicks = sgPathfinding.add(new IntSetting.Builder()
            .name("max-stuck-ticks")
            .description("How many ticks without progress until the walker gives up on a path.")
            .defaultValue(80)
            .min(1)
            .sliderRange(1, 200)
            .build()
    );

    public final Setting<Integer> maxRepaths = sgPathfinding.add(new IntSetting.Builder()
            .name("max-repaths")
            .description("How many times the walker may re-plan a path without getting closer before giving up. Set to -1 for unlimited attempts.")
            .defaultValue(4)
            .min(-1)
            .sliderRange(-1, 20)
            .build()
    );

    public final Setting<Integer> jumpLead = sgPathfinding.add(new IntSetting.Builder()
            .name("jump-lead")
            .description("How many blocks ahead of the player the walker starts jumping for elevated nodes.")
            .defaultValue(6)
            .min(1)
            .sliderRange(1, 20)
            .build()
    );
    public final Setting<Integer> jumpCooldown = sgPathfinding.add(new IntSetting.Builder()
            .name("jump-cooldown")
            .description("Minimum ticks between jumps.")
            .defaultValue(6)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );
    public final Setting<Double> jumpDy = sgPathfinding.add(new DoubleSetting.Builder()
            .name("jump-dy")
            .description("How much higher than the player's feet a node must be to trigger or require a jump.")
            .defaultValue(0.4)
            .min(0.0)
            .sliderRange(0.0, 2.0)
            .build()
    );
    public final Setting<Double> progressMin = sgPathfinding.add(new DoubleSetting.Builder()
            .name("progress-min")
            .description("Minimum distance gained per tick toward a node to count as progress.")
            .defaultValue(0.03)
            .min(0.0)
            .sliderRange(0.0, 1.0)
            .build()
    );
    public final Setting<Double> nodeBox = sgPathfinding.add(new DoubleSetting.Builder()
            .name("node-box")
            .description("How close to a node the player needs to get horizontally to consider it reached.")
            .defaultValue(0.45)
            .min(0.0)
            .sliderRange(0.0, 2.0)
            .build()
    );
    public final Setting<Double> nodeDy = sgPathfinding.add(new DoubleSetting.Builder()
            .name("node-dy")
            .description("Vertical tolerance for reaching a node.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderRange(0.0, 3.0)
            .build()
    );
    public final Setting<Double> arrivedDy = sgPathfinding.add(new DoubleSetting.Builder()
            .name("arrived-dy")
            .description("Vertical tolerance for counting as arrived at the target.")
            .defaultValue(1.5)
            .min(0.0)
            .sliderRange(0.0, 3.0)
            .build()
    );

    public final Setting<Boolean> cleanup = sgCleanup.add(new BoolSetting.Builder()
            .name("cleanup")
            .description("Remove the last waypoint when the queue gets too long.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> cleanupLimit = sgCleanup.add(new IntSetting.Builder()
            .name("max-waypoints")
            .description("Maximum number of waypoints in the queue. When exceeded and cleanup is on, the last waypoint is removed.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 100)
            .build()
    );

    private final Setting<Boolean> renderPath = sgRendering.add(new BoolSetting.Builder()
            .name("render-path")
            .description("Render the path lines.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> pathColor = sgRendering.add(new ColorSetting.Builder()
            .name("path-color")
            .description("Color of the path lines.")
            .defaultValue(new SettingColor(57, 198, 255))
            .build()
    );

    private final Setting<Boolean> renderTarget = sgRendering.add(new BoolSetting.Builder()
            .name("render-target")
            .description("Render the target box.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRendering.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the side of the target box.")
            .defaultValue(new SettingColor(57, 255, 100, 50))
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRendering.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the line of the target box.")
            .defaultValue(new SettingColor(57, 255, 100))
            .build()
    );

    public final Setting<Boolean> logEnabled = sgLogging.add(new BoolSetting.Builder()
            .name("log-messages")
            .description("Master toggle for all walkerist messages in chat.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logWalkTo = sgLogging.add(new BoolSetting.Builder()
            .name("log-walk-to")
            .description("Log when a search starts.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logPath = sgLogging.add(new BoolSetting.Builder()
            .name("log-path")
            .description("Log when a path is found.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logRepath = sgLogging.add(new BoolSetting.Builder()
            .name("log-repath")
            .description("Log when the walker searches for a new path.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logArrived = sgLogging.add(new BoolSetting.Builder()
            .name("log-arrived")
            .description("Log when the walker arrives at the target.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logStuck = sgLogging.add(new BoolSetting.Builder()
            .name("log-stuck")
            .description("Log when the walker gets stuck and gives up.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> logDoor = sgLogging.add(new BoolSetting.Builder()
            .name("log-door")
            .description("Log when a door is opened by the walker.")
            .defaultValue(true)
            .build()
    );

    private GuiTheme widgetTheme;
    private WVerticalList widgetList;
    private WBlockPosEdit posEdit;
    private WButton pauseButton;
    private WCheckbox autoPlayCheckbox;
    private BlockPos posEditValue;
    private final List<WLabel> statusLabels = new ArrayList<>();
    private final List<WCheckbox> activeCheckboxes = new ArrayList<>();

    public Walkerist() {
        super(Meteorist.CATEGORY, "walkerist", "Walks through a queue of positions using the vanilla A* PathFinder.");
        WalkeristUtils.setModule(this);
        WalkeristUtils.setQueueChangeListener(this::refreshWidget);
    }

    private void refreshWidget() {
        if (widgetTheme != null && widgetList != null) fillWidget(widgetTheme, widgetList);
    }

    public boolean isLogEnabled(WalkeristUtils.LogType type) {
        if (!logEnabled.get()) return false;

        return switch (type) {
            case WalkTo -> logWalkTo.get();
            case Path -> logPath.get();
            case Repath -> logRepath.get();
            case Arrived -> logArrived.get();
            case Stuck -> logStuck.get();
            case Door -> logDoor.get();
        };
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        Path path = WalkeristUtils.getPath();
        if (path != null && renderPath.get()) {
            Color color = pathColor.get();
            for (int i = 1; i < path.getNodeCount(); i++) {
                BlockPos a = path.getNodePos(i - 1), b = path.getNodePos(i);
                event.renderer.line(a.getX() + 0.5, a.getY() + 0.1, a.getZ() + 0.5,
                        b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, color);
            }
        }

        BlockPos target = WalkeristUtils.getTarget();
        if (target != null && renderTarget.get()) {
            event.renderer.box(target, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }

    @Override
    public String getInfoString() {
        return WalkeristUtils.getState().name();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        widgetTheme = theme;
        WVerticalList list = theme.verticalList();
        widgetList = list;
        fillWidget(theme, list);
        return list;
    }

    public void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        WHorizontalList addRow = list.add(theme.horizontalList()).expandX().widget();
        addRow.spacing = 10;

        posEdit = addRow.add(theme.blockPosEdit(posEditValue != null ? posEditValue : mc.player.blockPosition())).expandX().widget();
        posEdit.action = () -> posEditValue = posEdit.get();

        WButton add = addRow.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            posEditValue = posEdit.get();
            WalkeristUtils.queueAdd(posEditValue);
        };

        list.add(theme.horizontalSeparator()).expandX();

        WTable table = list.add(theme.table()).expandX().widget();

        statusLabels.clear();
        activeCheckboxes.clear();
        List<Waypoint> waypoints = WalkeristUtils.getWaypoints();
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint waypoint = waypoints.get(i);
            int index = i;

            WCheckbox active = table.add(theme.checkbox(index == WalkeristUtils.getCurrent())).widget();
            activeCheckboxes.add(active);
            active.tooltip = "Select which waypoint to walk to.";
            active.action = () -> {
                if (active.checked) WalkeristUtils.queueSelect(index);
                else WalkeristUtils.queueDeselect();
            };

            WLabel status = table.add(theme.label(WalkeristUtils.statusText(index))).widget();
            statusLabels.add(status);

            WBlockPosEdit coordinates = table.add(theme.blockPosEdit(waypoint.pos.get())).expandX().widget();
            coordinates.action = () -> WalkeristUtils.queueSet(index, coordinates.get());

            if (waypoints.size() > 1) {
                WContainer moveContainer = table.add(theme.horizontalList()).expandWidgetX().widget();

                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move waypoint up.";
                    moveUp.action = () -> WalkeristUtils.queueMove(index, index - 1);
                }

                if (index < waypoints.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move waypoint down.";
                    moveDown.action = () -> WalkeristUtils.queueMove(index, index + 1);
                }
            }

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove waypoint.";
            remove.action = () -> WalkeristUtils.queueRemove(index);

            table.row();
        }

        if (!WalkeristUtils.getWaypoints().isEmpty()) list.add(theme.horizontalSeparator()).expandX();

        WTable controls = list.add(theme.table()).expandX().widget();

        WButton play = controls.add(theme.button("Play")).expandX().widget();
        play.action = WalkeristUtils::queuePlay;

        pauseButton = controls.add(theme.button("Pause")).expandX().widget();
        pauseButton.action = WalkeristUtils::togglePaused;

        WButton clear = controls.add(theme.button("Clear")).expandX().widget();
        clear.action = WalkeristUtils::queueClear;

        autoPlayCheckbox = controls.add(theme.checkbox(autoPlay.get())).widget();
        autoPlayCheckbox.tooltip = autoPlay.description;
        autoPlayCheckbox.action = () -> {
            autoPlay.set(autoPlayCheckbox.checked);
            if (autoPlayCheckbox.checked && WalkeristUtils.getCurrent() == -1) WalkeristUtils.queuePlay();
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    public void updateStatus() {
        if (widgetTheme == null) return;
        for (int i = 0; i < statusLabels.size(); i++) statusLabels.get(i).set(WalkeristUtils.statusText(i));
        for (int i = 0; i < activeCheckboxes.size(); i++) activeCheckboxes.get(i).checked = i == WalkeristUtils.getCurrent();

        if (pauseButton != null) pauseButton.set(WalkeristUtils.isPaused() ? "Resume" : "Pause");
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.put("waypoints", WalkeristUtils.saveWaypoints());

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        super.fromTag(tag);
        WalkeristUtils.loadWaypoints(tag.getListOrEmpty("waypoints"));

        return this;
    }
}
