package zgoly.meteorist.modules.autocrafter.autocrafts;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class InventoryAutoCraft extends BaseAutoCraft {
    public static final String type = "Inventory";

    public final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("actions-per-tick")
            .description("How many actions are allowed to be performed per tick.")
            .defaultValue(1)
            .sliderRange(1, 4)
            .range(1, 4)
            .build()
    );

    public final Setting<Item> outputItem = sgOutput.add(new ItemSetting.Builder()
            .name("output-item")
            .description("The output item to check.")
            .defaultValue(Items.CRAFTING_TABLE)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public InventoryAutoCraft() {
        for (int i = 1; i <= 4; i++) {
            int row = (i - 1) / 2 + 1;
            int column = (i - 1) % 2 + 1;
            ingredients.add(sgIngredients.add(new ItemSetting.Builder()
                    .name("ingredient-" + i)
                    .description("Ingredient #" + i + " (row #" + row + ", column #" + column + "). Use Structure Void to keep slot empty.")
                    .defaultValue(Items.OAK_PLANKS)
                    .onChanged(value -> reloadParent())
                    .build()
            ));
        }
    }

    public String getTypeName() {
        return type;
    }

    public InventoryAutoCraft copy() {
        return (InventoryAutoCraft) new InventoryAutoCraft().fromTag(toTag());
    }

    public Setting<Item> getOutputItem() {
        return outputItem;
    }

    public int getActionsPerTick() {
        return actionsPerTick.get();
    }
}
