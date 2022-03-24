//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;
import java.util.List;

public class ContainerCleaner extends Module {
    public enum Mode {
        Blacklist,
        Whitelist
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("\"Blacklist\" - throw items from list; \"Whitelist\" - keep items from list, throw others")
            .defaultValue(Mode.Blacklist)
            .build()
    );
    private final Setting<Boolean> bThrowAngle = sgGeneral.add(new BoolSetting.Builder()
            .name("Throw angle")
            .description("For example, \"-30\" is throw over container, \"90\" is throw under your legs")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> throwAngle = sgGeneral.add(new IntSetting.Builder()
            .name("Throw angle")
            .description("For example, \"-30\" is throw over container, \"90\" is throw under your legs")
            .defaultValue(0)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .visible(bThrowAngle::get)
            .build()
    );

    private final Setting<List<Item>> Items = sgGeneral.add(new ItemListSetting.Builder()
            .name("Items")
            .description("Items to throw/keep.")
            .build()
    );

    private final Setting<Boolean> chests = sgGeneral.add(new BoolSetting.Builder()
            .name("Chests")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> shulkerBoxes = sgGeneral.add(new BoolSetting.Builder()
            .name("Shulker Boxes")
            .defaultValue(true)
            .build()
    );

    public ContainerCleaner() {
        super(Meteorist.CATEGORY, "container-cleaner", "Throw items from container.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Items.get().isEmpty()) return;
        if (!(chests.get() && mc.currentScreen instanceof GenericContainerScreen
                || shulkerBoxes.get() && mc.currentScreen instanceof ShulkerBoxScreen)) return;
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
            if ((mode.get() == Mode.Blacklist) && !Items.get().contains(mc.player.currentScreenHandler.getSlot(i).getStack().getItem())) continue;
            if ((mode.get() == Mode.Whitelist) && Items.get().contains(mc.player.currentScreenHandler.getSlot(i).getStack().getItem())) continue;
            if (bThrowAngle.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), throwAngle.get(), mc.player.isOnGround()));
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
        }
    }
}



