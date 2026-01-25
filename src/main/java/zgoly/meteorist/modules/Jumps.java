package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class Jumps extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between extra jumps in ticks (20 ticks = 1 second).")
            .defaultValue(3)
            .range(1, 20)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Integer> extraJumps = sgGeneral.add(new IntSetting.Builder()
            .name("extra-jumps")
            .description("Number of additional jumps to perform after the initial jump.")
            .defaultValue(2)
            .range(1, 10)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only activate the jump sequence if the player is standing on the ground.")
            .defaultValue(false)
            .build()
    );

    private int jumpsRemaining, tickTimer = 0;

    public Jumps() {
        super(Meteorist.CATEGORY, "jumps", "Performs multiple jumps to gain extra height. Works best with NoFall.");
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press || !mc.options.keyJump.matches(event.input)) return;

        if (mc.player == null) return;
        if (onlyOnGround.get() && !mc.player.onGround()) return;

        mc.player.jumpFromGround();
        jumpsRemaining = extraJumps.get();
        tickTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (jumpsRemaining > 0) {
            tickTimer++;
            if (tickTimer >= delay.get()) {
                mc.player.fallDistance = 0;
                mc.player.jumpFromGround();
                jumpsRemaining--;
                tickTimer = 0;
            }
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(jumpsRemaining);
    }
}