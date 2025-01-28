package zgoly.meteorist.modules.autologin;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.config.MeteoristConfig;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoSave = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-save")
            .description("Automatically saves passwords when you login or register.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-self")
            .description("Ignore self commands. Recommended to leave enabled, otherwise Auto Save will trigger on Auto Login commands.")
            .defaultValue(true)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<String> loginCommand = sgGeneral.add(new StringSetting.Builder()
            .name("login-command")
            .description("Command to login.")
            .defaultValue("/login")
            .visible(autoSave::get)
            .build()
    );
    private final Setting<List<String>> commandsToHandle = sgGeneral.add(new StringListSetting.Builder()
            .name("commands-to-handle")
            .description("Commands to handle.")
            .defaultValue("login", "log", "l", "register", "reg")
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> saveServerIp = sgGeneral.add(new BoolSetting.Builder()
            .name("save-server-ip")
            .description("Save server IP in filter.")
            .defaultValue(true)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> saveUsername = sgGeneral.add(new BoolSetting.Builder()
            .name("save-username")
            .description("Save username in filter.")
            .defaultValue(true)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkPasswordCommand = sgGeneral.add(new BoolSetting.Builder()
            .name("check-password-command")
            .description("Whether to check password command when adding new auto login.")
            .defaultValue(true)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkExecutionMode = sgGeneral.add(new BoolSetting.Builder()
            .name("check-execution-mode")
            .description("Whether to check execution mode when adding new auto login.")
            .defaultValue(false)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("check-delay")
            .description("Whether to check delay when adding new auto login.")
            .defaultValue(false)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkUsername = sgGeneral.add(new BoolSetting.Builder()
            .name("check-username")
            .description("Whether to check username when adding new auto login.")
            .defaultValue(false)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkServerIp = sgGeneral.add(new BoolSetting.Builder()
            .name("check-server-ip")
            .description("Whether to check server IP when adding new auto login.")
            .defaultValue(false)
            .visible(autoSave::get)
            .build()
    );
    private final Setting<Boolean> checkLastLogin = sgGeneral.add(new BoolSetting.Builder()
            .name("check-last-login")
            .description("Whether to check last login when adding new auto login.")
            .defaultValue(false)
            .visible(autoSave::get)
            .build()
    );

    public static List<BaseAutoLogin> autoLogins = new ArrayList<>();
    private long startWorldTime = -1;
    private boolean work = true;
    // I haven't come up with anything better than this, feel free to create PR and make it better!
    private boolean isSendingChatMessage = false;

    public AutoLogin() {
        super(Meteorist.CATEGORY, "auto-login", "Automatically logs in your account using /login.");
    }

    @Override
    public void onActivate() {
        startWorldTime = -1;
    }

    public NbtCompound toTag() {
        NbtCompound superTag = super.toTag();

        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();
        for (BaseAutoLogin autoLogin : autoLogins) {
            NbtCompound mTag = new NbtCompound();
            mTag.put("autoLogin", autoLogin.toTag());

            list.add(mTag);
        }
        tag.put("autoLogins", list);
        MeteoristConfig.save(this.name, "default", tag);

        return superTag;
    }

    public Module fromTag(NbtCompound superTag) {
        NbtCompound tag = MeteoristConfig.load(this.name, "default");

        autoLogins.clear();
        NbtList list = tag.getList("autoLogins", NbtElement.COMPOUND_TYPE);

        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;

            BaseAutoLogin autoLogin = new BaseAutoLogin();
            NbtCompound autoLoginTag = tagI.getCompound("autoLogin");

            if (autoLoginTag != null) autoLogin.fromTag(autoLoginTag);

            autoLogins.add(autoLogin);
        }

        return super.fromTag(superTag);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        for (BaseAutoLogin autoLogin : autoLogins) {

            list.add(theme.settings(autoLogin.settings)).expandX();

            WContainer controls = list.add(theme.horizontalList()).widget();

            if (autoLogins.size() > 1) {
                WContainer moveContainer = controls.add(theme.horizontalList()).expandX().widget();
                int index = autoLogins.indexOf(autoLogin);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move auto login up.";
                    moveUp.action = () -> {
                        autoLogins.remove(index);
                        autoLogins.add(index - 1, autoLogin);
                        fillWidget(theme, list);
                    };
                }

                if (index < autoLogins.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move auto login down.";
                    moveDown.action = () -> {
                        autoLogins.remove(index);
                        autoLogins.add(index + 1, autoLogin);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = controls.add(theme.button("Copy")).expandX().widget();
            copy.tooltip = "Duplicate auto login.";
            copy.action = () -> {
                autoLogins.add(autoLogins.indexOf(autoLogin), autoLogin.copy());
                fillWidget(theme, list);
            };

            WButton remove = controls.add(theme.button("Remove")).expandX().widget();
            remove.tooltip = "Remove auto login.";
            remove.action = () -> {
                autoLogins.remove(autoLogin);
                fillWidget(theme, list);
            };
        }

        list.add(theme.horizontalSeparator()).expandX();

        WContainer controls = list.add(theme.horizontalList()).expandX().widget();
        WButton add = controls.add(theme.button("New Auto Login")).expandX().widget();
        add.action = () -> {
            BaseAutoLogin autoLogin = new BaseAutoLogin();
            autoLogins.add(autoLogin);
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Auto Logins")).expandX().widget();
        removeAll.action = () -> {
            autoLogins.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (work) {
            if (startWorldTime == -1) startWorldTime = mc.world.getTime();
            boolean hasRemainingAutoLogins = false;
            for (BaseAutoLogin autoLogin : List.copyOf(autoLogins)) {
                // Check all conditions

                if (mc.world.getTime() < startWorldTime + autoLogin.delay.get()) {
                    hasRemainingAutoLogins = true;
                    continue;
                }
                if (autoLogin.executionMode.get() == BaseAutoLogin.ExecutionMode.Multiplayer && mc.isInSingleplayer())
                    continue;
                if (autoLogin.executionMode.get() == BaseAutoLogin.ExecutionMode.Singleplayer && !mc.isInSingleplayer())
                    continue;
                if (!autoLogin.serverIpFilter.get().isEmpty() && !Utils.getWorldName().equals(autoLogin.serverIpFilter.get()))
                    continue;
                if (!autoLogin.usernameFilter.get().isEmpty() && !mc.getSession().getUsername().equals(autoLogin.usernameFilter.get()))
                    continue;

                isSendingChatMessage = true;
                ChatUtils.sendPlayerMsg(autoLogin.passwordCommand.get());
                isSendingChatMessage = false;

                if (autoLogin.lastLogin.get()) {
                    work = false;
                    break;
                }

                if (!hasRemainingAutoLogins) work = false;
            }
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        startWorldTime = -1;
        work = true;
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Send event) {
        if (autoSave.get() && event.packet instanceof CommandExecutionC2SPacket packet) {
            if (ignoreSelf.get() && isSendingChatMessage) return;
            String command = packet.command();
            String[] args = command.split(" ");
            if (args.length >= 2 && commandsToHandle.get().contains(args[0])) {
                BaseAutoLogin autoLogin = new BaseAutoLogin();
                autoLogin.passwordCommand.set(loginCommand.get() + " " + args[1]);
                if (saveUsername.get()) autoLogin.usernameFilter.set(mc.getSession().getUsername());
                if (saveServerIp.get() && mc.getServer() != null) autoLogin.serverIpFilter.set(Utils.getWorldName());
                if (!exists(autoLogin)) autoLogins.add(autoLogin);
            }
        }
    }

    public boolean exists(BaseAutoLogin toCheck) {
        for (BaseAutoLogin autoLogin : List.copyOf(autoLogins)) {
            boolean allChecksPassed = true;

            // Check all conditions
            if (checkPasswordCommand.get())
                allChecksPassed &= autoLogin.passwordCommand.get().equals(toCheck.passwordCommand.get());
            if (checkExecutionMode.get())
                allChecksPassed &= autoLogin.executionMode.get() == toCheck.executionMode.get();
            if (checkDelay.get()) allChecksPassed &= Objects.equals(autoLogin.delay.get(), toCheck.delay.get());
            if (checkUsername.get())
                allChecksPassed &= autoLogin.usernameFilter.get().equals(toCheck.usernameFilter.get());
            if (checkServerIp.get())
                allChecksPassed &= autoLogin.serverIpFilter.get().equals(toCheck.serverIpFilter.get());
            if (checkLastLogin.get()) allChecksPassed &= autoLogin.lastLogin.get() == toCheck.lastLogin.get();

            if (allChecksPassed) return true;
        }
        return false;
    }
}