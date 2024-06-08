package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class AutoFeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> feedCommand = sgGeneral.add(new StringSetting.Builder()
            .name("feed-command")
            .description("Command to refill hunger bar.")
            .defaultValue("/feed")
            .build()
    );

    private final Setting<Integer> hungerLevel = sgGeneral.add(new IntSetting.Builder()
            .name("hunger-level")
            .description("Hunger level at which to send the command.")
            .defaultValue(12)
            .min(1)
            .sliderRange(1, 20)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after sending a command in ticks (20 ticks = 1 sec).")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 40)
            .build()
    );

    private int timer;

    public AutoFeed() {
        super(Meteorist.CATEGORY, "auto-feed", "Writes command in chat when hunger level is low.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer >= delay.get() && mc.player.getHungerManager().getFoodLevel() <= hungerLevel.get()) {
            ChatUtils.sendPlayerMsg(feedCommand.get());
            timer = 0;
        } else timer++;
    }
}