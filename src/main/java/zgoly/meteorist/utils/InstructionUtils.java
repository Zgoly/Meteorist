package zgoly.meteorist.utils;

import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.starscript.Script;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import zgoly.meteorist.modules.instructions.InstructionFactory;
import zgoly.meteorist.modules.instructions.instructions.BaseInstruction;
import zgoly.meteorist.modules.instructions.instructions.CommandInstruction;
import zgoly.meteorist.modules.instructions.instructions.DelayInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstructionUtils {
    /**
     * Reads a list of instructions from the given NbtCompound tag and creates
     * corresponding BaseInstruction objects using the provided InstructionFactory.
     *
     * @param tag The NbtCompound containing the instructions to be read.
     * @param factory The InstructionFactory used to create BaseInstruction objects.
     * @return A List of BaseInstruction objects created from the instructions
     *         found in the NbtCompound tag.
     */
    public static List<BaseInstruction> readInstructionsFromTag(NbtCompound tag, InstructionFactory factory) {
        List<BaseInstruction> instructions = new ArrayList<>();
        NbtList list = tag.getList("instructions", NbtElement.COMPOUND_TYPE);

        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;
            String type = tagI.getString("type");
            BaseInstruction instruction = factory.createInstruction(type);

            if (instruction != null) {
                NbtCompound instructionTag = tagI.getCompound("instruction");
                if (instructionTag != null) instruction.fromTag(instructionTag);
                instructions.add(instruction);
            }
        }

        return instructions;
    }

    /**
     * Processes all instructions in the given list and adds the commands to be
     * executed to the given map. The start tick is set to 0.
     *
     * @param instructions The list of instructions to be processed.
     * @param map The map to which the commands to be executed are added.
     * @return The total number of ticks required to execute all instructions.
     */
    public static int processInstructions(List<BaseInstruction> instructions, Map<Integer, List<String>> map) {
        return processInstructions(instructions, map, 0);
    }

    /**
     * Processes all instructions in the given list and adds the commands to be
     * executed to the given map. The start tick is set to the given tick.
     *
     * @param instructions The list of instructions to be processed.
     * @param map The map to which the commands to be executed are added.
     * @param tick The start tick.
     * @return The total number of ticks required to execute all instructions.
     */
    public static int processInstructions(List<BaseInstruction> instructions, Map<Integer, List<String>> map, int tick) {
        for (BaseInstruction instruction : instructions) {
            if (instruction instanceof DelayInstruction delayInstruction) {
                tick += delayInstruction.delay.get();
            } else if (instruction instanceof CommandInstruction commandInstruction) {
                for (int i = 0; i < commandInstruction.runCount.get(); i++) {
                    map.computeIfAbsent(tick, ArrayList::new).add(commandInstruction.command.get());
                    if (i < commandInstruction.runCount.get() - 1) {
                        tick += commandInstruction.delayBetweenRuns.get();
                    }
                }
            }
        }
        return tick;
    }

    /**
     * Executes all commands in the given map that should be executed at the given tick.
     *
     * @param map The map containing the commands to be executed.
     * @param currentTick The current tick.
     */
    public static void executeCommands(Map<Integer, List<String>> map, int currentTick) {
        executeCommands(map, currentTick, 0);
    }

    /**
     * Executes all commands in the given map that should be executed at the given tick.
     * The start tick is the tick at which the first command should be executed.
     *
     * @param map The map containing the commands to be executed.
     * @param currentTick The current tick.
     * @param startTick The start tick.
     */
    public static void executeCommands(Map<Integer, List<String>> map, int currentTick, int startTick) {
        for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
            if (startTick + entry.getKey() == currentTick) {
                for (String command : entry.getValue()) {
                    Script script = MeteorStarscript.compile(command);
                    if (script != null) ChatUtils.sendPlayerMsg(MeteorStarscript.run(script));
                }
            }
        }
    }
}