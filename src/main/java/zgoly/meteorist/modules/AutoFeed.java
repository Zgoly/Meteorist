//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class AutoFeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> feedCmd = sgGeneral.add(new StringSetting.Builder()
            .name("feed-command:")
            .description("Feed command.")
            .defaultValue("/feed")
            .build()
    );

    private final Setting<Integer> hungerLevel = sgGeneral.add(new IntSetting.Builder()
            .name("hunger-level:")
            .description("The hunger level at which to send the command.")
            .defaultValue(12)
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

    public AutoFeed() {
        super(Meteorist.CATEGORY, "auto-feed", "Writes command in chat when hunger level is low.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (value < wait.get()) value ++;
        if (value >= wait.get() && mc.player.getHungerManager().getFoodLevel() <= hungerLevel.get()) {
            value = 0;
            mc.player.sendChatMessage(feedCmd.get());
        } else value++;
    }
}