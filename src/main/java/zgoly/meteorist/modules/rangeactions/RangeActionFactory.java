package zgoly.meteorist.modules.rangeactions;

import zgoly.meteorist.modules.rangeactions.rangeactions.BaseRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.CommandsRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.DespawnerRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.InteractionRangeAction;

import java.util.HashMap;
import java.util.Map;

public class RangeActionFactory {
    private final Map<String, Factory> factories;

    public RangeActionFactory() {
        factories = new HashMap<>();
        factories.put(InteractionRangeAction.type, InteractionRangeAction::new);
        factories.put(DespawnerRangeAction.type, DespawnerRangeAction::new);
        factories.put(CommandsRangeAction.type, CommandsRangeAction::new);
    }

    public BaseRangeAction createRangeAction(String name) {
        if (factories.containsKey(name)) return factories.get(name).create();
        return null;
    }

    private interface Factory {
        BaseRangeAction create();
    }
}