package zgoly.meteorist.modules.autologin;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;

public class BaseAutoLogin implements ISerializable<BaseAutoLogin> {
    Settings settings = new Settings();

    SettingGroup sgAutoLogin = settings.createGroup("Auto Login");

    public final Setting<String> loginCommand = sgAutoLogin.add(new StringSetting.Builder()
            .name("login-command")
            .description("Command to login.")
            .placeholder("/login 12345678")
            .build()
    );
    public final Setting<ExecutionMode> executionMode = sgAutoLogin.add(new EnumSetting.Builder<ExecutionMode>()
            .name("execution-mode")
            .description("Execution mode.")
            .defaultValue(ExecutionMode.Multiplayer)
            .build()
    );
    public final Setting<Integer> delay = sgAutoLogin.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks before logging in.")
            .defaultValue(20)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );
    public final Setting<String> usernameFilter = sgAutoLogin.add(new StringSetting.Builder()
            .name("username-filter")
            .description("Username to check when logging in. Leave it empty if you don't need it.")
            .placeholder("Keep empty to skip")
            .build()
    );
    public final Setting<String> serverIpFilter = sgAutoLogin.add(new StringSetting.Builder()
            .name("server-ip-filter")
            .description("Server IP to check when logging in. Leave it empty if you don't need it.")
            .placeholder("Keep empty to skip")
            .build()
    );
    public final Setting<Boolean> lastLogin = sgAutoLogin.add(new BoolSetting.Builder()
            .name("last-login")
            .description("Enable if this password is the last Auto Login to use.")
            .build()
    );

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BaseAutoLogin fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public BaseAutoLogin copy() {
        return new BaseAutoLogin().fromTag(toTag());
    }

    public enum ExecutionMode {
        Multiplayer,
        Singleplayer,
        Both
    }
}
