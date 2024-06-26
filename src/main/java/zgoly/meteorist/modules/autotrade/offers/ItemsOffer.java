package zgoly.meteorist.modules.autotrade.offers;

import meteordevelopment.meteorclient.settings.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class ItemsOffer extends BaseOffer {
    public static final String type = "Items";
    SettingGroup sgInput = settings.createGroup("Input");
    SettingGroup sgOutput = settings.createGroup("Output");

    public final Setting<Boolean> checkFirstInputItem = sgInput.add(new BoolSetting.Builder()
            .name("check-first-input-item")
            .description("Checks first item to input.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<Item> firstInputItem = sgInput.add(new ItemSetting.Builder()
            .name("first-input-item")
            .description("First item to input.")
            .defaultValue(Items.BREAD)
            .onChanged(value -> reloadParent())
            .visible(checkFirstInputItem::get)
            .build()
    );

    public final Setting<Boolean> checkFirstInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-first-input-item-count")
            .description("Checks count of first item to input.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .visible(checkFirstInputItem::get)
            .build()
    );

    public final Setting<Boolean> useFinalCount = sgInput.add(new BoolSetting.Builder()
            .name("use-final-count")
            .description("Use the final item count after discounts and demands, or the initial count.")
            .defaultValue(true)
            .visible(() -> checkFirstInputItem.get() && checkFirstInputItemCount.get())
            .build()
    );

    public final Setting<Integer> minFirstInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("min-first-input-item-count")
            .description("Minimum count of first item to input.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && checkFirstInputItemCount.get())
            .build()
    );

    public final Setting<Integer> maxFirstInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("max-first-input-item-count")
            .description("Maximum count of first item to input.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkFirstInputItem.get() && checkFirstInputItemCount.get())
            .build()
    );

    public final Setting<Boolean> checkSecondInputItem = sgInput.add(new BoolSetting.Builder()
            .name("check-second-input-item")
            .description("Checks second item to input.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<Item> secondInputItem = sgInput.add(new ItemSetting.Builder()
            .name("second-input-item")
            .description("Second item to input.")
            .defaultValue(Items.AIR)
            .onChanged(value -> reloadParent())
            .visible(checkSecondInputItem::get)
            .build()
    );

    public final Setting<Boolean> checkSecondInputItemCount = sgInput.add(new BoolSetting.Builder()
            .name("check-second-input-item-count")
            .description("Checks count of second item to input.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(checkSecondInputItem::get)
            .build()
    );

    public final Setting<Integer> minSecondInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("min-second-input-item-count")
            .description("Minimum count of second item to input.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && checkSecondInputItemCount.get())
            .build()
    );

    public final Setting<Integer> maxSecondInputItemCount = sgInput.add(new IntSetting.Builder()
            .name("max-second-input-item-count")
            .description("Maximum count of second item to input.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkSecondInputItem.get() && checkSecondInputItemCount.get())
            .build()
    );

    public final Setting<Boolean> checkOutputItem = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item")
            .description("Checks output item.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<Item> outputItem = sgOutput.add(new ItemSetting.Builder()
            .name("output-item")
            .description("Output item.")
            .defaultValue(Items.EMERALD)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public final Setting<Boolean> checkOutputItemCount = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item-count")
            .description("Checks count of output item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .visible(checkOutputItem::get)
            .build()
    );

    public final Setting<Integer> minOutputItemCount = sgOutput.add(new IntSetting.Builder()
            .name("min-output-item-count")
            .description("Minimum count of output item.")
            .defaultValue(1)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && checkOutputItemCount.get())
            .build()
    );

    public final Setting<Integer> maxOutputItemCount = sgOutput.add(new IntSetting.Builder()
            .name("max-output-item-count")
            .description("Maximum count of output item.")
            .defaultValue(16)
            .sliderRange(1, 64)
            .min(1)
            .onChanged(value -> reloadParent())
            .visible(() -> checkOutputItem.get() && checkOutputItemCount.get())
            .build()
    );

    public ItemsOffer() {
    }

    public String getTypeName() {
        return type;
    }

    public ItemsOffer copy() {
        ItemsOffer copy = new ItemsOffer();

        copy.enabled.set(enabled.get());

        copy.checkFirstInputItem.set(checkFirstInputItem.get());
        copy.firstInputItem.set(firstInputItem.get());
        copy.checkFirstInputItemCount.set(checkFirstInputItemCount.get());
        copy.useFinalCount.set(useFinalCount.get());
        copy.minFirstInputItemCount.set(minFirstInputItemCount.get());
        copy.maxFirstInputItemCount.set(maxFirstInputItemCount.get());

        copy.checkSecondInputItem.set(checkSecondInputItem.get());
        copy.secondInputItem.set(secondInputItem.get());
        copy.checkSecondInputItemCount.set(checkSecondInputItemCount.get());
        copy.minSecondInputItemCount.set(minSecondInputItemCount.get());
        copy.maxSecondInputItemCount.set(maxSecondInputItemCount.get());

        copy.checkOutputItem.set(checkOutputItem.get());
        copy.outputItem.set(outputItem.get());
        copy.checkOutputItemCount.set(checkOutputItemCount.get());
        copy.minOutputItemCount.set(minOutputItemCount.get());
        copy.maxOutputItemCount.set(maxOutputItemCount.get());

        return copy;
    }
}
