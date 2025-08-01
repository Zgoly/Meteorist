package zgoly.meteorist.utils.misc;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;

public class DebugLogger {
    private final Module module;
    private final Setting<Boolean> printDebugInfo;

    public DebugLogger(Module module, Settings settings) {
        this.module = module;

        SettingGroup sgDebug = settings.createGroup("Debug");
        this.printDebugInfo = sgDebug.add(new BoolSetting.Builder()
                .name("print-debug-info")
                .description("Prints debug information in the chat.")
                .defaultValue(false)
                .build()
        );
    }

    public void info(Text message) {
        if (printDebugInfo.get()) module.info(message);
    }

    public void info(String message, Object... args) {
        if (printDebugInfo.get()) module.info(message, args);
    }

    public void warning(String message, Object... args) {
        if (printDebugInfo.get()) module.warning(message, args);
    }

    public void error(String message, Object... args) {
        if (printDebugInfo.get()) module.error(message, args);
    }

    public boolean isDebugEnabled() {
        return printDebugInfo.get();
    }

    public void toggleDebug() {
        printDebugInfo.set(!printDebugInfo.get());
    }
}
