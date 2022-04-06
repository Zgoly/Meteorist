//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class HighJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> multiplier = sgGeneral.add(new IntSetting.Builder()
            .name("jump-multiplier:")
            .description("Jump height multiplier.")
            .defaultValue(1)
            .range(1, 128)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay:")
            .description("Delay between jumps in ticks (1 sec = 20 ticks).")
            .defaultValue(3)
            .range(1, 1200)
            .sliderRange(1, 10)
            .build()
    );

    int t = 0;
    int i = multiplier.get();

    public HighJump() {
        super(Meteorist.CATEGORY, "high-jump", "Makes you jump higher than normal.");
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) i=0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (i < multiplier.get()) {
            if (t >= delay.get()) {
                t=0;
                i++;
                mc.player.fallDistance = 0;
                mc.player.jump();
            } else t++;
        }
    }
}