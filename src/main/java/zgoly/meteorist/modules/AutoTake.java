//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;

public class AutoTake extends Module {
    public enum SlotActionTypes {
        THROW,
        QUICK_MOVE,
        SWAP,
        PICKUP
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SlotActionTypes> mode = sgGeneral.add(new EnumSetting.Builder<SlotActionTypes>()
            .name("mode")
            .description("The mode used.")
            .defaultValue(SlotActionTypes.THROW)
            .build()
    );

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
            .name("slot:")
            .description("Slot number to take. For example, in a double chest, \"1\" is the first slot, \"54\" is the last.")
            .defaultValue(1)
            .range(1, 90)
            .sliderRange(1, 90)
            .build()
    );

    private final Setting<Integer> button = sgGeneral.add(new IntSetting.Builder()
            .name("button:")
            .description("\"0\" - left click, \"1\" - right click.")
            .defaultValue(0)
            .range(0, 1)
            .sliderRange(0, 1)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay:")
            .description("Delay in ticks (1 sec = 20 ticks).")
            .defaultValue(10)
            .range(1, 40)
            .sliderRange(1, 40)
            .build()
    );

    private int value = 0;

    public AutoTake() {
        super(Meteorist.CATEGORY, "auto-take", "Automatically takes items from slot.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (value >= delay.get()) {
            value = 0;
            if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.get() - 1, button.get(), SlotActionType.valueOf(mode.get().name()), mc.player);
        } else value++;
    }

}