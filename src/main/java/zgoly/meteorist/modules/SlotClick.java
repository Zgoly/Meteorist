package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.SlotClickSyntaxHighlighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class SlotClick extends Module {
    public enum ItemFilteringMode {
        Whitelist,
        Blacklist,
        None
    }

    public enum SlotMode {
        Default,
        All
    }

    public enum SlotLimitation {
        HOTBAR_START,
        HOTBAR_END,
        OFFHAND,
        MAIN_START,
        MAIN_END,
        ARMOR_START,
        ARMOR_END;

        public int getValue() {
            return switch (this) {
                case HOTBAR_START -> SlotUtils.HOTBAR_START;
                case HOTBAR_END -> SlotUtils.HOTBAR_END;
                case OFFHAND -> SlotUtils.OFFHAND;
                case MAIN_END -> SlotUtils.MAIN_END;
                case ARMOR_START -> SlotUtils.ARMOR_START;
                case ARMOR_END -> SlotUtils.ARMOR_END;
                default -> SlotUtils.MAIN_START;
            };
        }
    }

    public enum ClickingButton {
        LeftMouse,
        RightMouse
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgSlots = settings.createGroup("Slots");
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    private final Setting<SlotActionType> actionMode = sgGeneral.add(new EnumSetting.Builder<SlotActionType>()
            .name("action-mode")
            .description("Defines what kind of action to execute when clicking.")
            .defaultValue(SlotActionType.PICKUP)
            .build()
    );

    private final Setting<ClickingButton> clickingButton = sgGeneral.add(new EnumSetting.Builder<ClickingButton>()
            .name("clicking-button")
            .description("The mouse button to be used for clicking a slot.")
            .defaultValue(ClickingButton.LeftMouse)
            .build()
    );

    private final Setting<List<ScreenHandlerType<?>>> containers = sgGeneral.add(new ScreenHandlerListSetting.Builder()
            .name("containers")
            .description("Determines the type of containers that can be interacted with.")
            .defaultValue(Arrays.asList(ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6))
            .build()
    );

    private final Setting<ItemFilteringMode> itemFilteringMode = sgFilter.add(new EnumSetting.Builder<ItemFilteringMode>()
            .name("item-filtering-mode")
            .description("Defines how items will be filtered when interacting with slots.")
            .defaultValue(ItemFilteringMode.Whitelist)
            .build()
    );

    private final Setting<List<Item>> itemWhitelist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-whitelist")
            .description("List of items that should be selected if filtering mode is set to Whitelist.")
            .visible(() -> itemFilteringMode.get() == ItemFilteringMode.Whitelist)
            .build()
    );

    private final Setting<List<Item>> itemBlacklist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-blacklist")
            .description("List of items that should be ignored if filtering mode is set to Blacklist.")
            .visible(() -> itemFilteringMode.get() == ItemFilteringMode.Blacklist)
            .build()
    );

    private final Setting<SlotMode> slotMode = sgSlots.add(new EnumSetting.Builder<SlotMode>()
            .name("slot-mode")
            .description("Determines how slots will be selected for interaction.")
            .defaultValue(SlotMode.Default)
            .build()
    );

    private final Setting<String> slotsToClick = sgSlots.add(new StringSetting.Builder()
            .name("slots-to-click")
            .description("Specific slots that should be interacted with when selection mode is set to Default.")
            .defaultValue("1-27")
            .renderer(SlotClickSyntaxHighlighting.class)
            .visible(() -> slotMode.get() == SlotMode.Default)
            .build()
    );

    private final Setting<SlotLimitation> slotLimitation = sgSlots.add(new EnumSetting.Builder<SlotLimitation>()
            .name("slots-limitation")
            .description("Defines the slot limitation. For example, when using 'MAIN_START', all slots will be limited to the inventory beginning.")
            .defaultValue(SlotLimitation.MAIN_START)
            .build()
    );

    private final Setting<Boolean> ignoreEmptySlots = sgSlots.add(new BoolSetting.Builder()
            .name("ignore-empty-slots")
            .description("Determines if empty slots should be skipped.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> slotClickDelay = sgDelay.add(new IntSetting.Builder()
            .name("slot-click-delay")
            .description("The delay in milliseconds between individual slot clicks.")
            .defaultValue(50)
            .sliderMax(1000)
            .build()
    );

    private final Setting<Integer> iterationDelay = sgDelay.add(new IntSetting.Builder()
            .name("iteration-delay")
            .description("The delay in milliseconds between each full iteration of slot clicks.")
            .defaultValue(50)
            .sliderMax(1000)
            .build()
    );

    public SlotClick() {
        super(Meteorist.CATEGORY, "slot-click", "Module that automates clicking on slots.");
    }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler.getType() == null || !containers.get().contains(handler.getType()) || event.packet.getSyncId() != handler.syncId) return;

        MeteorExecutor.execute(() -> {
            boolean ser = true;
            while (mc.currentScreen != null && Utils.canUpdate()) {
                List<Integer> slotsList = slotMode.get() == SlotMode.Default ? slotsToList(slotsToClick.get())
                        : IntStream.range(0, SlotUtils.indexToId(slotLimitation.get().getValue())).boxed().toList();

                for (Integer slot : slotsList) {
                    if (mc.currentScreen == null || !Utils.canUpdate()) break;
                    if ((ignoreEmptySlots.get() && handler.getSlot(slot).getStack().getItem() == Items.AIR) || !isAllowedItem(handler, slot)) continue;
                    if (mc.interactionManager == null) continue;
                    mc.execute(() -> mc.interactionManager.clickSlot(handler.syncId, slot - 1, clickingButton.get().ordinal(), actionMode.get(), mc.player));
                    sleep(slotClickDelay.get());
                }
                sleep(iterationDelay.get());
            }
        });
    }

    private boolean isAllowedItem(ScreenHandler handler, int slot) {
        Item item = handler.getSlot(slot).getStack().getItem();

        return switch (itemFilteringMode.get()) {
            case Whitelist -> itemWhitelist.get().contains(item);
            case Blacklist -> !itemBlacklist.get().contains(item);
            default -> true;
        };
    }

    private static List<Integer> slotsToList(String str) {
        List<Integer> result = new ArrayList<>();
        String[] parts = str.split("[^-\\d]+");

        for (String part : parts) {
            String[] numbers = part.split("-");

            if (numbers.length == 2) {
                int start = Integer.parseInt(numbers[0]);
                int end = Integer.parseInt(numbers[1]);
                int step = start <= end ? 1 : -1;

                for (int i = start; step > 0 ? i <= end : i >= end; i += step) result.add(i);
            } else {
                result.add(Integer.parseInt(numbers[0]));
            }
        }

        return result;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}