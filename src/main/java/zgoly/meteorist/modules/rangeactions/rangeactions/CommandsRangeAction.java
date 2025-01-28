package zgoly.meteorist.modules.rangeactions.rangeactions;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;

import java.util.List;

public class CommandsRangeAction extends BaseRangeAction {
    public static final String type = "Commands";

    protected final SettingGroup sgCommandsRangeAction = settings.createGroup("Commands Range Action");

    public final Setting<List<String>> commands = sgCommandsRangeAction.add(new StringListSetting.Builder()
            .name("commands")
            .description("Commands to send.")
            .defaultValue("/spawn")
            .build()
    );

    public final Setting<Integer> delay = sgCommandsRangeAction.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after sending a commands in ticks (20 ticks = 1 sec).")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 40)
            .build()
    );

    public final Setting<Integer> commandsPerTick = sgCommandsRangeAction.add(new IntSetting.Builder()
            .name("commands-per-tick")
            .description("Number of commands to send per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public CommandsRangeAction copy() {
        return (CommandsRangeAction) new CommandsRangeAction().fromTag(toTag());
    }
}
