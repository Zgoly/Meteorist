//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class AutoHeal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> healCmd = sgGeneral.add(new StringSetting.Builder()
            .name("heal-command:")
            .description("Heal command.")
            .defaultValue("/heal")
            .build()
    );

    private final Setting<Integer> healthLevel = sgGeneral.add(new IntSetting.Builder()
            .name("health-level:")
            .description("The health level at which to send the command.")
            .defaultValue(10)
            .range(1, 1024)
            .sliderRange(1, 20)
            .build()
    );

    private final Setting<Integer> wait = sgGeneral.add(new IntSetting.Builder()
            .name("wait:")
            .description("Waiting after sending a command in ticks (1 sec = 20 ticks).")
            .defaultValue(20)
            .range(1, 1200)
            .sliderRange(1, 40)
            .build()
    );

    private int value = 0;

    public AutoHeal() {
        super(Meteorist.CATEGORY, "auto-heal", "Writes command in chat when health level is low.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (value < wait.get()) value ++;
        if (value >= wait.get() && mc.player.getHealth() <= healthLevel.get()) {
            value = 0;
            mc.player.sendChatMessage(healCmd.get());
        } else value++;
    }
}