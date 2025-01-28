package zgoly.meteorist.modules.autocrafter;

import zgoly.meteorist.modules.autocrafter.autocrafts.BaseAutoCraft;
import zgoly.meteorist.modules.autocrafter.autocrafts.CraftingTableAutoCraft;
import zgoly.meteorist.modules.autocrafter.autocrafts.InventoryAutoCraft;

import java.util.HashMap;
import java.util.Map;

public class AutoCraftFactory {
    private final Map<String, Factory> factories;

    public AutoCraftFactory() {
        factories = new HashMap<>();
        factories.put(InventoryAutoCraft.type, InventoryAutoCraft::new);
        factories.put(CraftingTableAutoCraft.type, CraftingTableAutoCraft::new);
    }

    public BaseAutoCraft createAutoCraft(String name) {
        if (factories.containsKey(name)) return factories.get(name).create();
        return null;
    }

    private interface Factory {
        BaseAutoCraft create();
    }
}
