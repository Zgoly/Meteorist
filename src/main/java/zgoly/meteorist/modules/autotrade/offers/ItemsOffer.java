package zgoly.meteorist.modules.autotrade.offers;

import meteordevelopment.meteorclient.settings.*;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import zgoly.meteorist.settings.StringPairSetting;

import java.util.ArrayList;
import java.util.List;

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
            .defaultValue(FilterMode.Item)
            .onChanged(value -> reloadParent())
            .visible(checkFirstInputItem::get)
            .build()
    );
    public final Setting<Item> firstInputItem = sgInput.add(new ItemSetting.Builder()
            .name("first-input-item")
            .description("First item to input.")
            .defaultValue(Items.BREAD)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Item)
            .build()
    );
    public final Setting<Boolean> checkFirstInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-first-input-item-count")
            .description("Check count of first input item.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Item)
            .build()
    );
    public final Setting<Boolean> useFinalCount = sgInput.add(new BoolSetting.Builder()
            .name("use-final-count")
            .description("Use final count after discounts, or base count.")
            .defaultValue(true)
            .visible(() -> checkFirstInputItem.get() &&
                    firstInputFilterMode.get() == FilterMode.Item &&
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
                    firstInputFilterMode.get() == FilterMode.Item &&
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
                    firstInputFilterMode.get() == FilterMode.Item &&
                    checkFirstInputItemCount.get())
            .build()
    );
    public final Setting<MatchMode> firstInputMatchMode = sgInput.add(new EnumSetting.Builder<MatchMode>()
            .name("first-input-match-mode")
            .description("Whether all NBT filters must match, or any one is enough.")
            .defaultValue(MatchMode.All)
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Nbt)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<List<Tuple<String, String>>> firstInputNbtFilter = sgInput.add(new StringPairSetting.Builder()
            .name("first-input-nbt-filter")
            .description("NBT path and regex pattern pairs to match on first input item.")
            .defaultValue(new ArrayList<>())
            .placeholder(new Tuple<>("nbt.path", "regex"))
            .visible(() -> checkFirstInputItem.get() && firstInputFilterMode.get() == FilterMode.Nbt)
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
            .defaultValue(FilterMode.Item)
            .onChanged(value -> reloadParent())
            .visible(checkSecondInputItem::get)
            .build()
    );
    public final Setting<Item> secondInputItem = sgInput.add(new ItemSetting.Builder()
            .name("second-input-item")
            .description("Second item to input.")
            .defaultValue(Items.AIR)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Item)
            .build()
    );
    public final Setting<Boolean> checkSecondInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-second-input-item-count")
            .description("Check count of second input item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Item)
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
                    secondInputFilterMode.get() == FilterMode.Item &&
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
                    secondInputFilterMode.get() == FilterMode.Item &&
                    checkSecondInputItemCount.get())
            .build()
    );
    public final Setting<MatchMode> secondInputMatchMode = sgInput.add(new EnumSetting.Builder<MatchMode>()
            .name("second-input-match-mode")
            .description("Whether all NBT filters must match, or any one is enough.")
            .defaultValue(MatchMode.All)
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Nbt)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<List<Tuple<String, String>>> secondInputNbtFilter = sgInput.add(new StringPairSetting.Builder()
            .name("second-input-nbt-filter")
            .description("NBT path and regex pattern pairs to match on second input item.")
            .defaultValue(new ArrayList<>())
            .placeholder(new Tuple<>("nbt.path", "regex"))
            .visible(() -> checkSecondInputItem.get() && secondInputFilterMode.get() == FilterMode.Nbt)
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
            .defaultValue(FilterMode.Item)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public final Setting<Item> outputItem = sgOutput.add(new ItemSetting.Builder()
            .name("output-item")
            .description("Output item.")
            .defaultValue(Items.EMERALD)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Item)
            .build()
    );

    public final Setting<Boolean> checkOutputItemCount = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item-count")
            .description("Check count of output item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Item)
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
                    outputFilterMode.get() == FilterMode.Item &&
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
                    outputFilterMode.get() == FilterMode.Item &&
                    checkOutputItemCount.get())
            .build()
    );

    public final Setting<MatchMode> outputMatchMode = sgOutput.add(new EnumSetting.Builder<MatchMode>()
            .name("output-match-mode")
            .description("Whether all NBT filters must match, or any one is enough.")
            .defaultValue(MatchMode.All)
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Nbt)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<List<Tuple<String, String>>> outputNbtFilter = sgOutput.add(new StringPairSetting.Builder()
            .name("output-nbt-filter")
            .description("NBT path and regex pattern pairs to match on output item.")
            .defaultValue(List.of(new Tuple<>("components.\"minecraft:stored_enchantments\"", "minecraft:mending")))
            .placeholder(new Tuple<>("nbt.path", "regex"))
            .visible(() -> checkOutputItem.get() && outputFilterMode.get() == FilterMode.Nbt)
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
        Item,
        Nbt
    }

    public enum MatchMode {
        Any,
        All
    }
}