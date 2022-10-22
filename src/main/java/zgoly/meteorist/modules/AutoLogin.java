//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {
        Percentage,
        Commands
    }

    private final Setting<String> loginCommand = sgGeneral.add(new StringSetting.Builder()
            .name("login-command")
            .description("Command to login.")
            .defaultValue("login 1234")
            .build()
    );

    private final Setting<Boolean> serverOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("server-only")
            .description("Use Auto Login only on server.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay before send command in ticks (20 ticks = 1 sec).")
            .defaultValue(20)
            .range(1, 120)
            .sliderRange(1, 40)
            .build()
    );

    boolean work;
    private int timer;

    public AutoLogin() {
        super(Meteorist.CATEGORY, "auto-login", "Automatically logs in your account.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (serverOnly.get() && mc.getServer() != null && mc.getServer().isSingleplayer()) return;
        if (timer >= delay.get() && !loginCommand.get().isEmpty() && work) {
            work = false;
            mc.player.sendCommand(loginCommand.get().replace("/", ""));
            timer = 0;
        } else timer ++;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        work = true;
    }
}