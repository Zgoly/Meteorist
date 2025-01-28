package zgoly.meteorist.modules.instructions.instructions;

import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;

public class CommandInstruction extends BaseInstruction {
    public static final String type = "Command";

    protected final SettingGroup sgCommandInstruction = settings.createGroup("Command Instruction");

    public final Setting<String> command = sgCommandInstruction.add(new StringSetting.Builder()
            .name("command")
            .description("The command to run.")
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
    );
    public final Setting<Integer> runCount = sgCommandInstruction.add(new IntSetting.Builder()
            .name("run-count")
            .description("The number of times to run the command.")
            .defaultValue(1)
            .sliderRange(1, 10)
            .min(1)
            .build()
    );
    public final Setting<Integer> delayBetweenRuns = sgCommandInstruction.add(new IntSetting.Builder()
            .name("delay-between-runs")
            .description("The delay between runs in ticks.")
            .defaultValue(10)
            .min(0)
            .build()
    );

    public CommandInstruction() {
    }

    public String getTypeName() {
        return type;
    }

    public CommandInstruction copy() {
        return (CommandInstruction) new CommandInstruction().fromTag(toTag());
    }
}
