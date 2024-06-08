package zgoly.meteorist.modules.instructions.instructions;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class DelayInstruction extends BaseInstruction {
    public static final String type = "Delay";

    protected final SettingGroup sgGeneral = settings.createGroup("Delay Instruction");

    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The number of ticks to wait.")
            .defaultValue(20)
            .sliderRange(1, 20)
            .min(1)
            .build()
    );

    public DelayInstruction() {
    }

    public String getTypeName() {
        return type;
    }

    public DelayInstruction copy() {
        DelayInstruction copy = new DelayInstruction();
        copy.delay.set(this.delay.get());
        return copy;
    }
}
