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

public class JumpJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between jumps in ticks (20 ticks = 1 sec).")
            .defaultValue(3)
            .range(1, 1200)
            .sliderRange(1, 10)
            .build()
    );
    private final Setting<Integer> multiplier = sgGeneral.add(new IntSetting.Builder()
            .name("jumps-number")
            .description("Number of jumps to be made.")
            .defaultValue(2)
            .range(1, 1200)
            .sliderRange(1, 10)
            .onChanged(a -> onActivate())
            .build()
    );

    int mult;
    int timer;

    public JumpJump() {
        super(Meteorist.CATEGORY, "jump-jump", "Makes you jump higher than normal using multiple jumps.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        mult = multiplier.get();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) mult = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mult < multiplier.get()) {
            if (timer >= delay.get()) {
                mult++;
                mc.player.fallDistance = 0;
                mc.player.jump();
                timer = 0;
            } else timer++;
        }
    }
}