package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.screen.slot.SlotActionType;

public class SlotRangeSelection extends DefaultSlotSelection {
    public static final String type = "Range";
    public final SettingGroup sgMultipleSlotSelection = settings.createGroup("Slot Range Selection");
    public final Setting<Integer> fromSlot = sgMultipleSlotSelection.add(new IntSetting.Builder()
            .name("from-slot")
            .description("The first slot to select.")
            .defaultValue(0)
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Integer> toSlot = sgMultipleSlotSelection.add(new IntSetting.Builder()
            .name("to-slot")
            .description("The last slot to select.")
            .defaultValue(8)
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<SlotActionType> action = sgMultipleSlotSelection.add(new EnumSetting.Builder<SlotActionType>()
            .name("action")
            .description("Action to perform.")
            .defaultValue(SlotActionType.PICKUP)
            .build()
    );
    public final Setting<Integer> button = sgMultipleSlotSelection.add(new IntSetting.Builder()
            .name("button")
            .description("Button to press.")
            .build()
    );
    public final Setting<Integer> delay = sgMultipleSlotSelection.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between each slot selection.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .min(0)
            .build()
    );
    public int calculatedSlot = 0;

    public SlotRangeSelection() {
    }

    public String getTypeName() {
        return type;
    }

    public SlotRangeSelection copy() {
        return (SlotRangeSelection) new SlotRangeSelection().fromTag(toTag());
    }
}
