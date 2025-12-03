package zgoly.meteorist.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import zgoly.meteorist.commands.arguments.InstructionArgumentType;
import zgoly.meteorist.modules.instructions.InstructionFactory;
import zgoly.meteorist.modules.instructions.Instructions;
import zgoly.meteorist.modules.instructions.instructions.BaseInstruction;
import zgoly.meteorist.utils.InstructionUtils;
import zgoly.meteorist.utils.MeteoristUtils;
import zgoly.meteorist.utils.misc.DebugLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructionsCommand extends Command {
    public Instructions instructionsModule = Modules.get().get(Instructions.class);
    public Map<Integer, List<String>> map = new HashMap<>();

    private final DebugLogger debugLogger;

    public InstructionsCommand() {
        super("instructions", "Runs saved instructions from the \"Instructions\" module.");
        MeteorClient.EVENT_BUS.subscribe(this);

        debugLogger = new DebugLogger(instructionsModule, instructionsModule.settings);
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("run")
                .then(argument("instruction", InstructionArgumentType.instruction())
                        .then(argument("count", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    String instructionName = context.getArgument("instruction", String.class);
                                    File instructionFile = InstructionArgumentType.instruction().getInstructionFile(instructionName);
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    return handleInstruction(instructionFile, count);
                                })
                        )
                        .executes(context -> {
                            String instructionName = context.getArgument("instruction", String.class);
                            File instructionFile = InstructionArgumentType.instruction().getInstructionFile(instructionName);
                            return handleInstruction(instructionFile, 1);
                        })
                )
        );

        builder.then(literal("debug").executes(context -> {
            boolean value = debugLogger.isDebugEnabled();

            debugLogger.toggleDebug();
            info("Debug Info was (highlight)%s", value ? "disabled" : "enabled");

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stop").executes(context -> {
            if (!map.isEmpty()) {
                info("Stopping (highlight)%d(default) instructions", map.size());
                map.clear();
            } else {
                error("Nothing to stop");
            }
            return SINGLE_SUCCESS;
        }));
    }

    public int handleInstruction(File file, int runs) {
        List<BaseInstruction> instructions;
        InstructionFactory factory = new InstructionFactory();

        try {
            try (InputStream inputStream = new FileInputStream(file)) {
                CompoundTag tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                instructions = InstructionUtils.readInstructionsFromTag(tag, factory);
            }

            int worldTime = (int) mc.level.getGameTime() + 1; // +1 for the first instruction
            int tick = worldTime;

            for (int r = 0; r < runs; r++) {
                int lastTick = InstructionUtils.processInstructions(instructions, map, tick);
                debugLogger.info("Iteration (highlight)%d(default) will run from (highlight)%d(default) to (highlight)%d(default) ticks", r + 1, tick, lastTick);
                tick = lastTick;
            }

            String runsMessage = (runs > 1) ? " for (highlight)" + runs + "(default) runs" : "";
            info("Loaded (highlight)%d(default) instructions from (highlight)%s(default)%s", instructions.size(), file.getName(), runsMessage);
            int totalTicks = tick - worldTime;
            info("Total ticks to run: (highlight)%d(default) (approximately %s)", totalTicks, MeteoristUtils.ticksToTime(totalTicks, false, true));
        } catch (Exception e) {
            error("Error loading instructions: (highlight)%d(default)", e.getMessage());
        }
        return SINGLE_SUCCESS;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.level == null) return;
        int worldTime = (int) mc.level.getGameTime();

        if (!map.isEmpty()) {
            debugLogger.info("Running (highlight)%d(default) instructions", map.size());
        }

        // Remove old instructions
        map.entrySet().removeIf(entry -> entry.getKey() < worldTime);

        InstructionUtils.executeCommands(map, worldTime);
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        map.clear();
    }
}
