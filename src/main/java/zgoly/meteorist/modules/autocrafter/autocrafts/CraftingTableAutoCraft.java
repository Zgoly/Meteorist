package zgoly.meteorist.modules.autocrafter.autocrafts;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class CraftingTableAutoCraft extends BaseAutoCraft {
    public static final String type = "CraftingTable";

    public final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("actions-per-tick")
            .description("How many actions are allowed to be performed per tick.")
            .defaultValue(1)
            .sliderRange(1, 9)
            .range(1, 9)
            .build()
    );

    public final Setting<Item> outputItem = sgOutput.add(new ItemSetting.Builder()
            .name("output-item")
            .description("The output item to check.")
            .defaultValue(Items.GOLD_INGOT)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public CraftingTableAutoCraft() {
        for (int i = 1; i <= 9; i++) {
            int row = (i - 1) / 3 + 1;
            int column = (i - 1) % 3 + 1;
            ingredients.add(sgIngredients.add(new ItemSetting.Builder()
                    .name("ingredient-" + i)
                    .description("Ingredient #" + i + " (row #" + row + ", column #" + column + "). Use Structure Void to keep slot empty.")
                    .defaultValue(Items.GOLD_NUGGET)
                    .onChanged(value -> reloadParent())
                    .build()
            ));
        }
    }

    public String getTypeName() {
        return type;
    }

    public CraftingTableAutoCraft copy() {
        return (CraftingTableAutoCraft) new CraftingTableAutoCraft().fromTag(toTag());
    }

    public Setting<Item> getOutputItem() {
        return outputItem;
    }

    public int getActionsPerTick() {
        return actionsPerTick.get();
    }
}
