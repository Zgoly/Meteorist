package zgoly.meteorist.modules.instructions;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.modules.instructions.instructions.BaseInstruction;
import zgoly.meteorist.modules.instructions.instructions.CommandInstruction;
import zgoly.meteorist.modules.instructions.instructions.DelayInstruction;
import zgoly.meteorist.utils.InstructionUtils;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static zgoly.meteorist.Meteorist.*;

public class Instructions extends Module {
    public final SettingGroup sgDebug = settings.createGroup("Debug");

    public final Setting<Boolean> printDebugInfo = sgDebug.add(new BoolSetting.Builder()
            .name("print-debug-info")
            .description("Logs debug information to the chat. Not intended for the module, but for the command.")
            .defaultValue(false)
            .build()
    );

    public static List<BaseInstruction> instructions = new ArrayList<>();
    private final InstructionFactory factory = new InstructionFactory();
    private int startTick = -1;

    public Instructions() {
        super(Meteorist.CATEGORY, "instructions", "Runs commands with different delays and number of cycles. Supports Starscript.");
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BaseInstruction instruction : instructions) {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", instruction.getTypeName());
            mTag.put("instruction", instruction.toTag());

            list.add(mTag);
        }
        tag.put("instructions", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        instructions.clear();
        instructions = InstructionUtils.readInstructionsFromTag(tag, factory);

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        for (BaseInstruction instruction : instructions) {
            list.add(theme.settings(instruction.settings)).expandX();

            WContainer container = list.add(theme.horizontalList()).expandX().widget();
            if (instructions.size() > 1) {
                int index = instructions.indexOf(instruction);
                if (index > 0) {
                    WButton moveUp = container.add(theme.button(ARROW_UP)).widget();
                    moveUp.tooltip = "Move instruction up.";
                    moveUp.action = () -> {
                        instructions.remove(index);
                        instructions.add(index - 1, instruction);
                        fillWidget(theme, list);
                    };
                }

                if (index < instructions.size() - 1) {
                    WButton moveDown = container.add(theme.button(ARROW_DOWN)).widget();
                    moveDown.tooltip = "Move instruction down.";
                    moveDown.action = () -> {
                        instructions.remove(index);
                        instructions.add(index + 1, instruction);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = container.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate instruction.";
            copy.action = () -> {
                instructions.add(instructions.indexOf(instruction), instruction.copy());
                fillWidget(theme, list);
            };

            WMinus remove = container.add(theme.minus()).widget();
            remove.tooltip = "Remove instruction.";
            remove.action = () -> {
                instructions.remove(instruction);
                fillWidget(theme, list);
            };
        }

        list.add(theme.horizontalSeparator()).expandX();
        WTable controls = list.add(theme.table()).expandX().widget();

        WButton createCommand = controls.add(theme.button("New Command")).expandX().widget();
        createCommand.action = () -> {
            CommandInstruction instruction = new CommandInstruction();
            instructions.add(instruction);
            fillWidget(theme, list);
        };

        WButton createDelay = controls.add(theme.button("New Delay")).expandX().widget();
        createDelay.action = () -> {
            DelayInstruction instruction = new DelayInstruction();
            instructions.add(instruction);
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Instructions")).expandX().widget();
        removeAll.action = () -> {
            instructions.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        startTick = -1;
    }

    public void onDeactivate() {
        startTick = -1;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        int currentTick = (int) mc.world.getTime();
        if (startTick == -1) startTick = currentTick;

        Map<Integer, List<String>> map = new HashMap<>();

        int lastTick = InstructionUtils.processInstructions(instructions, map);
        InstructionUtils.executeCommands(map, currentTick, startTick);

        if (startTick + lastTick <= currentTick) startTick = -1;

    }
}