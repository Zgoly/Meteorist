package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import zgoly.meteorist.Meteorist;

public class AutoFix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> fixCommand = sgGeneral.add(new StringSetting.Builder()
            .name("fix-command")
            .description("Command to fix item.")
            .defaultValue("/fix all")
            .build()
    );
    private final Setting<AutoFix.Mode> mode = sgGeneral.add(new EnumSetting.Builder<AutoFix.Mode>()
            .name("mode")
            .description("Percentage - calculate item durability in percentage, Default - calculate item durability in numbers.")
            .defaultValue(Mode.Default)
            .build()
    );
    private final Setting<Integer> minDurability = sgGeneral.add(new IntSetting.Builder()
            .name("min-durability")
            .description("The durability number to send the command.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 1000)
            .visible(() -> mode.get() == Mode.Default)
            .build()
    );
    private final Setting<Integer> minDurabilityPercentage = sgGeneral.add(new IntSetting.Builder()
            .name("min-durability")
            .description("The durability percentage to send the command.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 100)
            .visible(() -> mode.get() == Mode.Percentage)
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

    public AutoFix() {
        super(Meteorist.CATEGORY, "auto-fix", "Writes command in chat when item close to break.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean work = false;

        if (timer >= delay.get()) {
            for (ItemStack item : mc.player.getArmorItems()) {
                if (item.getDamage() > 0 && item.getMaxDamage() > 0) {
                    if ((mode.get() == Mode.Default && item.getMaxDamage() - item.getDamage() >= minDurability.get()) ||
                            (mode.get() == Mode.Percentage && (((item.getMaxDamage() - item.getDamage()) * 100) / item.getMaxDamage()) >= minDurabilityPercentage.get())) {
                        work = true;
                    }
                }
            }

            if (work) {
                ChatUtils.sendPlayerMsg(fixCommand.get());
                timer = 0;
            }
        } else {
            timer++;
        }
    }

    public enum Mode {
        Default,
        Percentage
    }
}