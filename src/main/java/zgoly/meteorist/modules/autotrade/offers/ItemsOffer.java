package zgoly.meteorist.modules.autotrade.offers;

import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ItemsOffer extends BaseOffer {
    public static final String type = "Items";

    SettingGroup sgInput = settings.createGroup("Input");
    public final Setting<Boolean> checkFirstInputItem = sgInput.add(new BoolSetting.Builder()
            .name("check-first-input-item")
            .description("Enable filtering for the first input item.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<FilterMode> firstInputFilterMode = sgInput.add(new EnumSetting.Builder<FilterMode>()
            .name("first-input-filter-mode")
            .description("How to filter the first input item.")
            .defaultValue(FilterMode.Simple)
            .onChanged(value -> reloadParent())
            .visible(checkFirstInputItem::get)
            .build()
    );
    public final Setting<Item> firstInputItem = sgInput.add(new ItemSetting.Builder()
            .name("first-input-item")
            .description("First item to input.")
            .defaultValue(Items.BREAD)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Simple)
            .build()
    );
    public final Setting<Boolean> checkFirstInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-first-input-item-count")
            .description("Check count of first input item.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Simple)
            .build()
    );
    public final Setting<Boolean> useFinalCount = sgInput.add(new BoolSetting.Builder()
            .name("use-final-count")
            .description("Use final count after discounts, or base count.")
            .defaultValue(true)
            .visible(() -> checkFirstInputItem.get() &&
                    firstInputFilterMode.get() == FilterMode.Simple &&
                    checkFirstInputItemCount.get())
            .build()
    );
    public final Setting<Integer> minFirstInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("min-first-input-item-count")
            .description("Minimum count of first input item.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() &&
                    firstInputFilterMode.get() == FilterMode.Simple &&
                    checkFirstInputItemCount.get())
            .build()
    );
    public final Setting<Integer> maxFirstInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("max-first-input-item-count")
            .description("Maximum count of first input item.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() &&
                    firstInputFilterMode.get() == FilterMode.Simple &&
                    checkFirstInputItemCount.get())
            .build()
    );
    public final Setting<String> firstInputExpression = sgInput.add(new StringSetting.Builder()
            .name("first-input-expression")
            .description("Starscript expression for the first input item; wrap in {braces} and produce true/false.")
            .defaultValue("{true}")
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Advanced)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Boolean> checkSecondInputItem = sgInput.add(new BoolSetting.Builder()
            .name("check-second-input-item")
            .description("Enable filtering for the second input item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<FilterMode> secondInputFilterMode = sgInput.add(new EnumSetting.Builder<FilterMode>()
            .name("second-input-filter-mode")
            .description("How to filter the second input item.")
            .defaultValue(FilterMode.Simple)
            .onChanged(value -> reloadParent())
            .visible(checkSecondInputItem::get)
            .build()
    );
    public final Setting<Item> secondInputItem = sgInput.add(new ItemSetting.Builder()
            .name("second-input-item")
            .description("Second item to input.")
            .defaultValue(Items.AIR)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Simple)
            .build()
    );
    public final Setting<Boolean> checkSecondInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-second-input-item-count")
            .description("Check count of second input item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Simple)
            .build()
    );
    public final Setting<Integer> minSecondInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("min-second-input-item-count")
            .description("Minimum count of second input item.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() &&
                    secondInputFilterMode.get() == FilterMode.Simple &&
                    checkSecondInputItemCount.get())
            .build()
    );
    public final Setting<Integer> maxSecondInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("max-second-input-item-count")
            .description("Maximum count of second input item.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() &&
                    secondInputFilterMode.get() == FilterMode.Simple &&
                    checkSecondInputItemCount.get())
            .build()
    );
    public final Setting<String> secondInputExpression = sgInput.add(new StringSetting.Builder()
            .name("second-input-expression")
            .description("Starscript expression for the second input item; wrap in {braces} and produce true/false.")
            .defaultValue("{true}")
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Advanced)
            .onChanged(value -> reloadParent())
            .build()
    );
    SettingGroup sgOutput = settings.createGroup("Output");
    public final Setting<Boolean> checkOutputItem = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item")
            .description("Enable filtering for the output item.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<FilterMode> outputFilterMode = sgOutput.add(new EnumSetting.Builder<FilterMode>()
            .name("output-filter-mode")
            .description("How to filter the output item.")
            .defaultValue(FilterMode.Simple)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public final Setting<Item> outputItem = sgOutput.add(new ItemSetting.Builder()
            .name("output-item")
            .description("Output item.")
            .defaultValue(Items.EMERALD)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Simple)
            .build()
    );

    public final Setting<Boolean> checkOutputItemCount = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item-count")
            .description("Check count of output item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Simple)
            .build()
    );

    public final Setting<Integer> minOutputItemCount = sgOutput.add(new IntSetting.Builder()
            .name("min-output-item-count")
            .description("Minimum count of output item.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() &&
                    outputFilterMode.get() == FilterMode.Simple &&
                    checkOutputItemCount.get())
            .build()
    );

    public final Setting<Integer> maxOutputItemCount = sgOutput.add(new IntSetting.Builder()
            .name("max-output-item-count")
            .description("Maximum count of output item.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() &&
                    outputFilterMode.get() == FilterMode.Simple &&
                    checkOutputItemCount.get())
            .build()
    );

    public final Setting<String> outputExpression = sgOutput.add(new StringSetting.Builder()
            .name("output-expression")
            .description("Starscript expression for the output item; wrap in {braces} and produce true/false.")
            .defaultValue("{contains(item.id, \"_book\") and item.nbt_verbose.path(\"components.\\\"minecraft:stored_enchantments\\\"\") != null}")
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Advanced)
            .onChanged(value -> reloadParent())
            .build()
    );

    public ItemsOffer() {
    }

    public String getTypeName() {
        return type;
    }

    public ItemsOffer copy() {
        return (ItemsOffer) new ItemsOffer().fromTag(toTag());
    }

    public enum FilterMode {
        Simple,
        Advanced
    }
}
