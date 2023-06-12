package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;

import java.util.List;

public class ContainerCleaner extends Module {
    public enum Mode {
        Blacklist,
        Whitelist
    }

    public enum DelayMode {
        One_Tick,
        Wait
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("\"Blacklist\" - throw items from list; \"Whitelist\" - keep items from list, throw others.")
            .defaultValue(Mode.Blacklist)
            .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to throw/keep.")
            .build()
    );

    private final Setting<Boolean> bThrowAngle = sgGeneral.add(new BoolSetting.Builder()
            .name("change-throw-angle")
            .description("Change throw angle if you want throw items out of the way you're looking.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> throwAngle = sgGeneral.add(new IntSetting.Builder()
            .name("throw-angle")
            .description("For example, \"-30\" is throw over container, \"90\" is throw under your legs.")
            .defaultValue(0)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .visible(bThrowAngle::get)
            .build()
    );

    private final Setting<Boolean> chests = sgGeneral.add(new BoolSetting.Builder()
            .name("chests")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shulkerBoxes = sgGeneral.add(new BoolSetting.Builder()
            .name("shulker-boxes")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> delay = sgDelay.add(new BoolSetting.Builder()
            .name("delay")
            .description("Delay between throws.")
            .defaultValue(true)
            .build()
    );

    private final Setting<DelayMode> delayMode = sgDelay.add(new EnumSetting.Builder<DelayMode>()
            .name("delay-mode")
            .description("The mode used to add delay between throws.")
            .defaultValue(DelayMode.One_Tick)
            .visible(delay::get)
            .build()
    );

    private final Setting<Integer> waitDouble = sgDelay.add(new IntSetting.Builder()
            .name("wait-(laggy)")
            .description("Wait in milliseconds between throws.")
            .defaultValue(100)
            .sliderRange(1, 100)
            .visible(() -> delayMode.get() == DelayMode.Wait)
            .build()
    );
    public ContainerCleaner() {
        super(Meteorist.CATEGORY, "container-cleaner", "Throw items from container.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) throws InterruptedException {
        if (items.get().isEmpty()) return;
        if (!((chests.get() && mc.currentScreen instanceof GenericContainerScreen) || (shulkerBoxes.get() && mc.currentScreen instanceof ShulkerBoxScreen))) return;
        if (bThrowAngle.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), throwAngle.get(), mc.player.isOnGround()));
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            if (item.asItem() != Items.AIR && ((mode.get() == Mode.Blacklist && items.get().contains(item)) || (mode.get() == Mode.Whitelist && !items.get().contains(item)))) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
                if (delayMode.get() == DelayMode.One_Tick) break;
                if (delayMode.get() == DelayMode.Wait) { Thread.sleep(waitDouble.get()); break; }
            }
        }
    }
}



