package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static zgoly.meteorist.Meteorist.*;

public class Instructions extends Module {
    public static List<Instruction> instructions = new ArrayList<>();

    public Instructions() {
        super(Meteorist.CATEGORY, "instructions", "Runs commands one by one with different delays and number of cycles.");
    }

    public static abstract class Instruction implements ISerializable<Instruction> {
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList nbtList = new NbtList();
        for (Instruction instruction : instructions) {
            nbtList.add(instruction.toTag());
        }
        tag.put("instructions", nbtList);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        NbtList nbtList = tag.getList("instructions", NbtElement.COMPOUND_TYPE);
        instructions.clear();
        for (NbtElement nbtElement : nbtList) {
            if (nbtElement.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                continue;
            }

            if (((NbtCompound) nbtElement).contains("delay")) {
                instructions.add(new DelayInstruction().fromTag((NbtCompound) nbtElement));
            } else {
                instructions.add(new CommandInstruction().fromTag((NbtCompound) nbtElement));
            }
        }

        return super.fromTag(tag);
    }

    public static class CommandInstruction extends Instruction {
        public String command = "";
        public int runCount = 1;
        public int delayBetweenRuns = 10;

        public CommandInstruction() {}

        public CommandInstruction(String command, int runCount, int delayBetweenRuns) {
            this.command = command;
            this.runCount = runCount;
            this.delayBetweenRuns = delayBetweenRuns;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("command", command);
            tag.putInt("runCount", runCount);
            tag.putInt("delayBetweenRuns", delayBetweenRuns);
            return tag;
        }

        @Override
        public CommandInstruction fromTag(NbtCompound tag) {
            this.command = tag.getString("command");
            this.runCount = tag.getInt("runCount");
            this.delayBetweenRuns = tag.getInt("delayBetweenRuns");
            return this;
        }
    }

    public static class DelayInstruction extends Instruction {
        public int delay = 20;

        public DelayInstruction() {}

        public DelayInstruction(int delay) {
            this.delay = delay;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putInt("delay", delay);
            return tag;
        }

        @Override
        public DelayInstruction fromTag(NbtCompound tag) {
            this.delay = tag.getInt("delay");
            return this;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection instructionsSection = list.add(theme.section("Instructions")).expandX().widget();
        WHorizontalList instructionsList = instructionsSection.add(theme.horizontalList()).expandX().widget();
        WTable table = instructionsList.add(theme.table()).expandX().widget();

        for (Instruction instruction : instructions) {
            CommandInstruction defaultCommandInstruction = new CommandInstruction();
            DelayInstruction defaultDelayInstruction = new DelayInstruction();

            if (instruction instanceof CommandInstruction commandInstruction) {
                WLabel commandLabel = theme.label("Command");
                commandLabel.tooltip = "The command to run.";
                table.add(commandLabel).widget();

                WTextBox command = table.add(theme.textBox(commandInstruction.command, (text1, c) -> true, StarscriptTextBoxRenderer.class)).expandX().widget();
                command.action = () -> commandInstruction.command = command.get();

                table.add(theme.button(GuiRenderer.RESET)).widget().action = () -> {
                    command.set(defaultCommandInstruction.command);
                    commandInstruction.command = command.get();
                };

                table.row();

                WLabel runCountLabel = theme.label("Run Count");
                runCountLabel.tooltip = "The number of times to run the command.";
                table.add(runCountLabel).widget();

                WIntEdit runCount = table.add(theme.intEdit(commandInstruction.runCount, 1, Integer.MAX_VALUE, 1, 10)).expandX().widget();
                runCount.action = () -> commandInstruction.runCount = runCount.get();

                table.add(theme.button(GuiRenderer.RESET)).widget().action = () -> {
                    runCount.set(defaultCommandInstruction.runCount);
                    commandInstruction.runCount = runCount.get();
                };

                table.row();

                WLabel delayBetweenRunsLabel = theme.label("Delay Between Runs");
                delayBetweenRunsLabel.tooltip = "The number of ticks to wait between runs.";
                table.add(delayBetweenRunsLabel).widget();

                WIntEdit delayBetweenRuns = table.add(theme.intEdit(commandInstruction.delayBetweenRuns, 0, Integer.MAX_VALUE, 0, 20)).expandX().widget();
                delayBetweenRuns.action = () -> commandInstruction.delayBetweenRuns = delayBetweenRuns.get();

                table.add(theme.button(GuiRenderer.RESET)).widget().action = () -> {
                    delayBetweenRuns.set(defaultCommandInstruction.delayBetweenRuns);
                    commandInstruction.delayBetweenRuns = delayBetweenRuns.get();
                };

                table.row();
            } else if (instruction instanceof DelayInstruction delayInstruction) {

                WLabel delayLabel = theme.label("Delay");
                delayLabel.tooltip = "The number of ticks to wait.";
                table.add(delayLabel).widget();

                WIntEdit delay = table.add(theme.intEdit(delayInstruction.delay, 0, Integer.MAX_VALUE, 0, 20)).expandX().widget();
                delay.action = () -> delayInstruction.delay = delay.get();

                table.add(theme.button(GuiRenderer.RESET)).widget().action = () -> {
                    delay.set(defaultDelayInstruction.delay);
                    delayInstruction.delay = delay.get();
                };
                table.row();
            }

            WContainer container = table.add(theme.horizontalList()).expandX().widget();
            if (instructions.size() > 1) {
                int index = instructions.indexOf(instruction);
                WButton moveUp = container.add(theme.button(ARROW_UP)).widget();
                moveUp.tooltip = "Move instruction up.";
                moveUp.action = () -> {
                    if (index == 0) return;
                    instructions.remove(index);
                    instructions.add(index - 1, instruction);
                    list.clear();
                    fillWidget(theme, list);
                };

                WButton moveDown = container.add(theme.button(ARROW_DOWN)).widget();
                moveDown.tooltip = "Move instruction down.";
                moveDown.action = () -> {
                    if (index == instructions.size() - 1) return;
                    instructions.remove(index);
                    instructions.add(index + 1, instruction);
                    list.clear();
                    fillWidget(theme, list);
                };
            }

            WButton copy = container.add(theme.button(COPY)).widget();
            copy.tooltip = "Copy instruction.";
            copy.action = () -> {
                list.clear();
                int index = instructions.indexOf(instruction);

                if (instruction instanceof CommandInstruction commandInstruction) {
                    instructions.add(index, new CommandInstruction(commandInstruction.command, commandInstruction.runCount, commandInstruction.delayBetweenRuns));
                } else if (instruction instanceof DelayInstruction delayInstruction) {
                    instructions.add(index, new DelayInstruction(delayInstruction.delay));
                }

                fillWidget(theme, list);
            };

            WMinus remove = container.add(theme.minus()).widget();
            remove.tooltip = "Remove instruction.";
            remove.action = () -> {
                list.clear();
                instructions.remove(instruction);
                fillWidget(theme, list);
            };
            table.row();

            table.add(theme.horizontalSeparator()).expandX().widget();
            table.row();
        }

        WButton createCommand = table.add(theme.button("Add new command")).expandX().widget();
        createCommand.action = () -> {
            CommandInstruction instruction = new CommandInstruction();
            instructions.add(instruction);
            list.clear();
            fillWidget(theme, list);
        };

        WButton createDelay = table.add(theme.button("Add new delay")).expandX().widget();
        createDelay.action = () -> {
            DelayInstruction instruction = new DelayInstruction();
            instructions.add(instruction);
            list.clear();
            fillWidget(theme, list);
        };
    }

    private int startTick = -1;

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        startTick = -1;
    }

    public void onDeactivate() {
        startTick = -1;
    }

    // This method of looping through all instructions allows to perform actions in one tick, which can be useful in certain situations
    @EventHandler
    public void onTick(TickEvent.Post event) {
        int currentTick = (int) mc.world.getTime();
        if (startTick == -1) startTick = currentTick;

        Map<Integer, List<String>> map = new HashMap<>();

        int tick = 0;

        for (Instruction instruction : instructions) {
            if (instruction instanceof DelayInstruction delayInstruction) {
                tick += delayInstruction.delay;
            } else if (instruction instanceof CommandInstruction commandInstruction) {
                for (int i = 0; i < commandInstruction.runCount; i++) {
                    map.computeIfAbsent(tick, k -> new ArrayList<>()).add(commandInstruction.command);
                    if (i < commandInstruction.runCount - 1) {
                        tick += commandInstruction.delayBetweenRuns;
                    }
                }
            }
        }

        for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
            if (startTick + entry.getKey() == currentTick) {
                for (String command : entry.getValue()) {
                    Script script = MeteorStarscript.compile(command);
                    if (script != null) {
                        ChatUtils.sendPlayerMsg(MeteorStarscript.run(script));
                    }
                }
            }
        }

        if (startTick + tick <= currentTick) startTick = -1;
    }
}