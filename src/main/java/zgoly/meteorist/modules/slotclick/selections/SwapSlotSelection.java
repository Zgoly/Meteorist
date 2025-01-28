package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class SwapSlotSelection extends DefaultSlotSelection {
    public static final String type = "Swap";

    public final SettingGroup sgSwapSlotSelection = settings.createGroup("Swap Slot Selection");

    public final Setting<Integer> fromSlot = sgSwapSlotSelection.add(new IntSetting.Builder()
            .name("from-slot")
            .description("Slot to swap from.")
            .defaultValue(0)
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Integer> toSlot = sgSwapSlotSelection.add(new IntSetting.Builder()
            .name("to-slot")
            .description("Slot to swap to.")
            .defaultValue(1)
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );

    public SwapSlotSelection() {
    }

    public String getTypeName() {
        return type;
    }

    public SwapSlotSelection copy() {
        return (SwapSlotSelection) new SwapSlotSelection().fromTag(toTag());
    }
}
