package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.MeteoristConfig;

import java.util.ArrayList;
import java.util.List;

import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class AutoLogin extends Module {
    public static List<Password> passwords = new ArrayList<>();
    private long currentWorldTime = -1;
    private boolean work = true;

    public AutoLogin() {
        super(Meteorist.CATEGORY, "auto-login", "Automatically logs in your account using /login.");
    }

    @Override
    public void onActivate() {
        currentWorldTime = -1;
    }

    public enum ExecutionMode {
        Multiplayer,
        Singleplayer,
        Both
    }

    public NbtCompound toTag() {
        NbtCompound superTag = super.toTag();

        NbtCompound tag = new NbtCompound();
        NbtList nbtList = new NbtList();
        for (Password password : passwords) {
            nbtList.add(password.toTag());
        }
        tag.put("passwords", nbtList);
        MeteoristConfig.save("passwords", tag);

        return superTag;
    }

    public Module fromTag(NbtCompound superTag) {
        NbtCompound tag = MeteoristConfig.load("passwords");
        NbtList nbtList = tag.getList("passwords", NbtElement.COMPOUND_TYPE);

        passwords.clear();
        for (NbtElement nbtElement : nbtList) {
            if (nbtElement.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                continue;
            }
            passwords.add(new Password().fromTag((NbtCompound) nbtElement));
        }

        return super.fromTag(superTag);
    }

    public static class Password implements ISerializable<Password> {
        public String passwordCommand = "/login 12345678";
        public ExecutionMode executionMode = ExecutionMode.Multiplayer;
        public int delay;
        public String username = "";
        public String serverIp = "";
        public boolean lastLogin = false;

        public Password() {}

        public Password(String passwordCommand, ExecutionMode executionMode, int delay, String username, String serverIp, boolean lastLogin) {
            this.passwordCommand = passwordCommand;
            this.executionMode = executionMode;
            this.delay = delay;
            this.username = username;
            this.serverIp = serverIp;
            this.lastLogin = lastLogin;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("passwordCommand", passwordCommand);
            tag.putString("executionMode", executionMode.name());
            tag.putInt("delay", delay);
            tag.putString("username", username);
            tag.putString("serverIP", serverIp);
            tag.putBoolean("lastLogin", lastLogin);
            return tag;
        }

        @Override
        public Password fromTag(NbtCompound tag) {
            this.passwordCommand = tag.getString("passwordCommand");
            this.executionMode = ExecutionMode.valueOf(tag.getString("executionMode"));
            this.delay = tag.getInt("delay");
            this.username = tag.getString("username");
            this.serverIp = tag.getString("serverIP");
            this.lastLogin = tag.getBoolean("lastLogin");
            return this;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection passwordsSection = list.add(theme.section("Passwords")).expandX().widget();
        WHorizontalList passwordsList = passwordsSection.add(theme.horizontalList()).expandX().widget();
        WTable table = passwordsList.add(theme.table()).expandX().widget();

        for (int i = 0; i < passwords.size(); i++) {
            Password password = passwords.get(i);

            WLabel passwordCommandLabel = table.add(theme.label("Login command")).widget();
            passwordCommandLabel.tooltip = "Command to login.";

            WTextBox passwordCommand = table.add(theme.textBox(password.passwordCommand)).expandX().widget();
            passwordCommand.tooltip = passwordCommandLabel.tooltip;
            passwordCommand.actionOnUnfocused = () -> password.passwordCommand = passwordCommand.get();

            table.row();

            WLabel executionModeLabel = table.add(theme.label("Execution mode")).widget();
            executionModeLabel.tooltip = "Execution mode.";

            WDropdown<ExecutionMode> executionMode = table.add(theme.dropdown(password.executionMode)).widget();
            executionMode.tooltip = executionModeLabel.tooltip;
            executionMode.action = () -> password.executionMode = executionMode.get();

            table.row();

            WLabel delayLabel = table.add(theme.label("Delay")).widget();
            delayLabel.tooltip = "Delay in ticks before logging in.";

            WIntEdit delay = table.add(theme.intEdit(password.delay, 0, Integer.MIN_VALUE, 0, 40)).widget();
            delay.tooltip = delayLabel.tooltip;
            delay.action = () -> password.delay = delay.get();

            table.row();

            WLabel usernameLabel = table.add(theme.label("Username filter")).widget();
            usernameLabel.tooltip = "Username to check when logging in. Leave it empty if you don't need it.";

            WTextBox username = table.add(theme.textBox(password.username)).expandX().widget();
            username.tooltip = usernameLabel.tooltip;
            username.actionOnUnfocused = () -> password.username = username.get();

            table.row();

            WLabel serverIPLabel = table.add(theme.label("Server IP filter")).widget();
            serverIPLabel.tooltip = "Server IP to check when logging in. Leave it empty if you don't need it.";

            WTextBox serverIP = table.add(theme.textBox(password.serverIp)).expandX().widget();
            serverIP.tooltip = serverIPLabel.tooltip;
            serverIP.actionOnUnfocused = () -> password.serverIp = serverIP.get();

            table.row();

            WLabel lastLoginLabel = table.add(theme.label("Last login")).widget();
            lastLoginLabel.tooltip = "Enable if this password is the last login command to use.";

            WCheckbox lastLogin = table.add(theme.checkbox(password.lastLogin)).widget();
            lastLogin.tooltip = lastLoginLabel.tooltip;
            lastLogin.action = () -> password.lastLogin = lastLogin.checked;

            table.row();

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (passwords.size() > 1) {
                int index = passwords.indexOf(password);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move PlacerData up.";
                    moveUp.action = () -> {
                        passwords.remove(index);
                        passwords.add(index - 1, password);
                        list.clear();
                        fillWidget(theme, list);
                    };
                }

                if (index < passwords.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move PlacerData down.";
                    moveDown.action = () -> {
                        passwords.remove(index);
                        passwords.add(index + 1, password);
                        list.clear();
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = table.add(theme.button("Copy")).expandX().widget();
            copy.tooltip = "Duplicate password.";
            copy.action = () -> {
                list.clear();
                passwords.add(new Password(password.passwordCommand, password.executionMode, password.delay, password.username, password.serverIp, password.lastLogin));
                fillWidget(theme, list);
            };

            WButton remove = table.add(theme.button("Remove")).expandX().widget();
            remove.tooltip = "Remove password.";
            remove.action = () -> {
                list.clear();
                passwords.remove(password);
                fillWidget(theme, list);
            };

            table.row();
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();
        WButton add = controls.add(theme.button("Add new password")).expandX().widget();
        add.action = () -> {
            Password password = new Password();
            passwords.add(password);
            list.clear();
            fillWidget(theme, list);
        };
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (work) {
            if (currentWorldTime == -1) currentWorldTime = mc.world.getTime();
            for (Password password : passwords) {
                if (mc.world.getTime() != currentWorldTime + password.delay) continue;
                if (password.executionMode == ExecutionMode.Multiplayer && mc.getServer().isSingleplayer()) continue;
                if (password.executionMode == ExecutionMode.Singleplayer && !mc.getServer().isSingleplayer()) continue;
                if (password.serverIp != null && mc.getServer().getServerIp() != null && !mc.getServer().getServerIp().contains(password.serverIp)) continue;
                if (password.username != null && !mc.getSession().getUsername().contains(password.username)) continue;

                ChatUtils.sendPlayerMsg(password.passwordCommand);
                if (password.lastLogin) work = false;
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        currentWorldTime = -1;
        work = true;
    }
}