package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class DelaySelection extends BaseSlotSelection {
    public static final String type = "Delay";

    public final SettingGroup sgDelaySelection = settings.createGroup("Delay Selection");

    public final Setting<Integer> delay = sgDelaySelection.add(new IntSetting.Builder()
            .name("delay")
            .description("Selection delay.")
            .defaultValue(10)
            .sliderRange(0, 20)
            .min(0)
            .onChanged(value -> reloadParent())
            .build()
    );

    public DelaySelection() {
    }

    public String getTypeName() {
        return type;
    }

    public DelaySelection copy() {
        return (DelaySelection) new DelaySelection().fromTag(toTag());
    }
}