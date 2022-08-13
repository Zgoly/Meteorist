//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;

public class SlotClick extends Module {
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
            .name("slot")
            .description("Slot number to click. For example, in a double chest, \"1\" is first slot, \"54\" is last.")
            .defaultValue(1)
            .range(1, 90)
            .sliderRange(1, 90)
            .build()
    );

    private final Setting<Integer> button = sgGeneral.add(new IntSetting.Builder()
            .name("mouse-button")
            .description("\"0\" - left mouse button, \"1\" - right mouse button.")
            .defaultValue(0)
            .range(0, 1)
            .sliderRange(0, 1)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks (20 ticks = 1 sec).")
            .defaultValue(10)
            .range(1, 1200)
            .sliderRange(1, 40)
            .build()
    );

    private int timer;

    public SlotClick() {
        super(Meteorist.CATEGORY, "slot-click", "Automatically clicks on slot.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer >= delay.get()) {
            if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.get() - 1, button.get(), SlotActionType.valueOf(mode.get().name()), mc.player);
            timer = 0;
        } else timer ++;
    }

}