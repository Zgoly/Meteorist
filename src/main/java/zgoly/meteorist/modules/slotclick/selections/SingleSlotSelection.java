package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.screen.slot.SlotActionType;

public class SingleSlotSelection extends DefaultSlotSelection {
    public static final String type = "Single";

    public final SettingGroup sgSingleSlotSelection = settings.createGroup("Single Slot Selection");

    public final Setting<Integer> slot = sgSingleSlotSelection.add(new IntSetting.Builder()
            .name("slot")
            .description("Slot to select.")
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<SlotActionType> action = sgSingleSlotSelection.add(new EnumSetting.Builder<SlotActionType>()
            .name("action")
            .description("Action to perform.")
            .defaultValue(SlotActionType.PICKUP)
            .build()
    );
    public final Setting<Integer> button = sgSingleSlotSelection.add(new IntSetting.Builder()
            .name("button")
            .description("Button to press.")
            .build()
    );

    public SingleSlotSelection() {
    }

    public String getTypeName() {
        return type;
    }

    public SingleSlotSelection copy() {
        return (SingleSlotSelection) new SingleSlotSelection().fromTag(toTag());
    }
}
