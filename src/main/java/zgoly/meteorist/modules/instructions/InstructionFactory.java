package zgoly.meteorist.modules.instructions;

import zgoly.meteorist.modules.instructions.instructions.BaseInstruction;
import zgoly.meteorist.modules.instructions.instructions.CommandInstruction;
import zgoly.meteorist.modules.instructions.instructions.DelayInstruction;

import java.util.HashMap;
import java.util.Map;

public class InstructionFactory {
    private final Map<String, Factory> factories;

    public InstructionFactory() {
        factories = new HashMap<>();
        factories.put(CommandInstruction.type, CommandInstruction::new);
        factories.put(DelayInstruction.type, DelayInstruction::new);
    }

    public BaseInstruction createInstruction(String name) {
        if (factories.containsKey(name)) return factories.get(name).create();
        return null;
    }

    private interface Factory {
        BaseInstruction create();
    }
}
