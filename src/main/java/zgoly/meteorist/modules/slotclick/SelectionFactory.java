package zgoly.meteorist.modules.slotclick;

import zgoly.meteorist.modules.slotclick.selections.*;

import java.util.HashMap;
import java.util.Map;

public class SelectionFactory {
    private final Map<String, Factory> factories;

    public SelectionFactory() {
        factories = new HashMap<>();
        factories.put(SingleSlotSelection.type, SingleSlotSelection::new);
        factories.put(SlotRangeSelection.type, SlotRangeSelection::new);
        factories.put(SwapSlotSelection.type, SwapSlotSelection::new);
        factories.put(DelaySelection.type, DelaySelection::new);
    }

    public BaseSlotSelection createSelection(String name) {
        if (factories.containsKey(name)) return factories.get(name).create();
        return null;
    }

    private interface Factory {
        BaseSlotSelection create();
    }
}
