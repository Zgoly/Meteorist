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

    private final Setting<String> logMsg = sgGeneral.add(new StringSetting.Builder()
            .name("login-message:")
            .description("Login message.")
            .defaultValue("/login")
            .build()
    );

    private final Setting<Boolean> showPass = sgGeneral.add(new BoolSetting.Builder()
            .name("show-password")
            .description("Show password.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
            .name("your-password:")
            .description("Your password.")
            .defaultValue("1234")
            .visible(showPass::get)
            .build()
    );

    private final Setting<Boolean> serverOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("server-only")
            .description("Use Auto Login only on server.")
            .defaultValue(true)
            .build()
    );

    boolean work = true;

    public AutoLogin() {
        super(Meteorist.CATEGORY, "auto-login", "Automatically logs in your account.");
    }

    //Shitty code anyway work
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (serverOnly.get() && mc.getServer() != null && mc.getServer().isSingleplayer()) return;
        if (!(logMsg.get().isEmpty() || password.get().isEmpty()) && work) {
            mc.player.sendChatMessage(logMsg.get() + " " + password.get());
            work = false;
        }
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        work = true;
    }
}