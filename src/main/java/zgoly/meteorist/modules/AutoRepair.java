package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.List;

public class AutoRepair extends Module {
    private final List<SlotConfig> slotConfigs = new ArrayList<>();

    public AutoRepair() {
        super(Meteorist.CATEGORY, "auto-repair", "Sends repair commands for equipped items when durability is low.");

        EquipmentSlot[] slotTypes = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
        };

        for (EquipmentSlot slotType : slotTypes) {
            SlotConfig slotConfig = new SlotConfig(slotType);
            slotConfigs.add(slotConfig);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (SlotConfig slotConfig : slotConfigs) {
            ItemStack item = mc.player.getEquippedStack(slotConfig.slot);

            if (!slotConfig.enabled.get() || item.isEmpty() || item.getMaxDamage() <= 0) continue;

            if (slotConfig.timer < slotConfig.delay.get()) {
                slotConfig.timer++;
                continue;
            }

            int currentDurability = item.getMaxDamage() - item.getDamage();

            boolean shouldRepair = false;
            if (slotConfig.mode.get() == Mode.Total && currentDurability <= slotConfig.minDurability.get()) {
                shouldRepair = true;
            } else if (slotConfig.mode.get() == Mode.Percentage && ((currentDurability * 100) / item.getMaxDamage()) <= slotConfig.minDurabilityPercentage.get()) {
                shouldRepair = true;
            }

            if (shouldRepair) {
                ChatUtils.sendPlayerMsg(slotConfig.repairCommand.get());
                slotConfig.timer = 0;
            }
        }
    }

    private class SlotConfig {
        public final EquipmentSlot slot;
        public final Setting<Boolean> enabled;
        public final Setting<String> repairCommand;
        public final Setting<Mode> mode;
        public final Setting<Integer> minDurability;
        public final Setting<Integer> minDurabilityPercentage;
        public final Setting<Integer> delay;
        public int timer;

        public SlotConfig(EquipmentSlot slot) {
            this.slot = slot;
            this.timer = 0;

            SettingGroup group = settings.createGroup(slot.getName());

            this.enabled = group.add(new BoolSetting.Builder()
                    .name("enabled")
                    .description("Enable durability check for the " + slot.getName() + " slot.")
                    .defaultValue(true)
                    .build()
            );
            this.repairCommand = group.add(new StringSetting.Builder()
                    .name("repair-command")
                    .description("Command to repair the " + slot.getName() + " slot.")
                    .defaultValue("/repair " + slot.getName().toLowerCase())
                    .build()
            );
            this.mode = group.add(new EnumSetting.Builder<Mode>()
                    .name("mode")
                    .description("Total - calculate item durability in numbers; Percentage - calculate item durability in percentage.")
                    .defaultValue(Mode.Percentage)
                    .build()
            );
            this.minDurability = group.add(new IntSetting.Builder()
                    .name("min-durability")
                    .description("Minimum durability for the " + slot.getName() + " slot.")
                    .defaultValue(10)
                    .min(1)
                    .sliderRange(1, 1000)
                    .visible(() -> mode.get() == Mode.Total)
                    .build()
            );
            this.minDurabilityPercentage = group.add(new IntSetting.Builder()
                    .name("min-durability-percentage")
                    .description("Minimum durability percentage for the " + slot.getName() + " slot.")
                    .defaultValue(10)
                    .min(1)
                    .sliderRange(1, 100)
                    .visible(() -> mode.get() == Mode.Percentage)
                    .build()
            );
            this.delay = group.add(new IntSetting.Builder()
                    .name("delay")
                    .description("Delay after sending a command in ticks (20 ticks = 1 sec).")
                    .defaultValue(20)
                    .min(1)
                    .sliderRange(1, 40)
                    .build()
            );
        }
    }

    public enum Mode {
        Total,
        Percentage
    }
}
