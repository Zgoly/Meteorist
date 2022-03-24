//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("")
            .defaultValue("/login 1234")
            .build()
    );

    public AutoLogin() {
        super(Meteorist.CATEGORY, "auto-login", "Automatically logs in your account.");
    }

    @EventHandler
    private void onMessageRecieve(ReceiveMessageEvent event) {
        if (!message.get().isEmpty()) mc.player.sendChatMessage(message.get());
    }
}