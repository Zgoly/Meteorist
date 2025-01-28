package zgoly.meteorist.modules.slotclick;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Pair;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.gui.screens.SlotSelectionScreen;
import zgoly.meteorist.modules.slotclick.selections.*;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static zgoly.meteorist.Meteorist.*;

public class SlotClick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableAfterIteration = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-after-iteration")
            .description("Disables the module after one iteration.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> printDebugInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("print-debug-info")
            .description("Prints debug information.")
            .defaultValue(false)
            .build()
    );

    public static List<BaseSlotSelection> slotSelections = new ArrayList<>();
    private final SelectionFactory factory = new SelectionFactory();
    private int startTick = -1;

    public SlotClick() {
        super(Meteorist.CATEGORY, "slot-click", "Module that automates clicking on slots.");
    }

    public static List<Integer> createList(int start, int end) {
        var list = IntStream.rangeClosed(Math.min(start, end), Math.max(start, end)).boxed().toList();
        return (start > end) ? list.reversed() : list;
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BaseSlotSelection slotSelection : slotSelections) {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", slotSelection.getTypeName());
            mTag.put("slotSelection", slotSelection.toTag());

            list.add(mTag);
        }
        tag.put("slotSelections", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        slotSelections.clear();
        NbtList list = tag.getList("slotSelections", NbtElement.COMPOUND_TYPE);

        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;

            String type = tagI.getString("type");
            BaseSlotSelection slotSelection = factory.createSelection(type);

            if (slotSelection != null) {
                NbtCompound slotSelectionTag = tagI.getCompound("slotSelection");
                if (slotSelectionTag != null) slotSelection.fromTag(slotSelectionTag);

                slotSelections.add(slotSelection);
            }
        }

        return this;
    }

    public void onDeactivate() {
        startTick = -1;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        startTick = -1;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    public void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        WTable table = list.add(theme.table()).expandX().widget();

        for (BaseSlotSelection slotSelection : slotSelections) {
            table.add(theme.label(slotSelection.getTypeName())).expandX().widget();
            WContainer infoContainer = table.add(theme.horizontalList()).expandX().widget();
            switch (slotSelection) {
                case SingleSlotSelection singleSlotSelection -> {
                    WIntEdit slot = infoContainer.add(theme.intEdit(singleSlotSelection.slot.get(), 0, Integer.MAX_VALUE, true)).widget();
                    slot.tooltip = singleSlotSelection.slot.description;
                    slot.actionOnRelease = () -> singleSlotSelection.slot.set(slot.get());
                }
                case SlotRangeSelection slotRangeSelection -> {
                    WIntEdit fromSlot = infoContainer.add(theme.intEdit(slotRangeSelection.fromSlot.get(), 0, Integer.MAX_VALUE, true)).widget();
                    fromSlot.tooltip = slotRangeSelection.fromSlot.description;
                    fromSlot.actionOnRelease = () -> slotRangeSelection.fromSlot.set(fromSlot.get());

                    WIntEdit toSlot = infoContainer.add(theme.intEdit(slotRangeSelection.toSlot.get(), 0, Integer.MAX_VALUE, true)).widget();
                    toSlot.tooltip = slotRangeSelection.toSlot.description;
                    toSlot.actionOnRelease = () -> slotRangeSelection.toSlot.set(toSlot.get());

                }
                case SwapSlotSelection swapSlotSelection -> {
                    WIntEdit fromSlot = infoContainer.add(theme.intEdit(swapSlotSelection.fromSlot.get(), 0, Integer.MAX_VALUE, true)).widget();
                    fromSlot.tooltip = swapSlotSelection.fromSlot.description;
                    fromSlot.actionOnRelease = () -> swapSlotSelection.fromSlot.set(fromSlot.get());

                    WIntEdit toSlot = infoContainer.add(theme.intEdit(swapSlotSelection.toSlot.get(), 0, Integer.MAX_VALUE, true)).widget();
                    toSlot.tooltip = swapSlotSelection.toSlot.description;
                    toSlot.actionOnRelease = () -> swapSlotSelection.toSlot.set(toSlot.get());

                }
                case DelaySelection delaySelection -> {
                    WIntEdit delay = infoContainer.add(theme.intEdit(delaySelection.delay.get(), 0, Integer.MAX_VALUE, true)).widget();
                    delay.tooltip = delaySelection.delay.description;
                    delay.actionOnRelease = () -> delaySelection.delay.set(delay.get());
                }
                default -> {
                }
            }

            WButton edit = table.add(theme.button("Edit")).expandX().widget();
            edit.tooltip = "Edit the slot selection.";
            edit.action = () -> mc.setScreen(new SlotSelectionScreen(theme, slotSelection));

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (slotSelections.size() > 1) {
                int index = slotSelections.indexOf(slotSelection);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move slot selection up.";
                    moveUp.action = () -> {
                        slotSelections.remove(index);
                        slotSelections.add(index - 1, slotSelection);
                        fillWidget(theme, list);
                    };
                }

                if (index < slotSelections.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move slot selection down.";
                    moveDown.action = () -> {
                        slotSelections.remove(index);
                        slotSelections.add(index + 1, slotSelection);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = table.add(theme.button(COPY)).widget();
            copy.tooltip = "Copy slot selection.";
            copy.action = () -> {
                slotSelections.add(slotSelections.indexOf(slotSelection), slotSelection.copy());
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove slot selection.";
            remove.action = () -> {
                slotSelections.remove(slotSelection);
                fillWidget(theme, list);
            };

            table.row();
        }

        list.add(theme.horizontalSeparator()).expandX();
        WTable controls = list.add(theme.table()).expandX().widget();

        WButton createSingleSlotSelection = controls.add(theme.button("New " + SingleSlotSelection.type)).widget();
        createSingleSlotSelection.action = () -> {
            slotSelections.add(new SingleSlotSelection());
            fillWidget(theme, list);
        };

        WButton createSlotRangeSelection = controls.add(theme.button("New " + SlotRangeSelection.type)).widget();
        createSlotRangeSelection.action = () -> {
            slotSelections.add(new SlotRangeSelection());
            fillWidget(theme, list);
        };

        WButton createSwapSlotSelection = controls.add(theme.button("New " + SwapSlotSelection.type)).widget();
        createSwapSlotSelection.action = () -> {
            slotSelections.add(new SwapSlotSelection());
            fillWidget(theme, list);
        };

        WButton createDelaySelection = controls.add(theme.button("New " + DelaySelection.type)).widget();
        createDelaySelection.action = () -> {
            slotSelections.add(new DelaySelection());
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Selections")).widget();
        removeAll.action = () -> {
            slotSelections.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        int currentTick = (int) mc.world.getTime();
        if (startTick == -1) startTick = currentTick;

        Map<Integer, List<BaseSlotSelection>> map = new HashMap<>();

        int tick = 0;
        for (BaseSlotSelection slotSelection : slotSelections) {
            if (slotSelection instanceof DelaySelection delaySelection) {
                tick += delaySelection.delay.get();
            } else if (slotSelection instanceof SingleSlotSelection || slotSelection instanceof SwapSlotSelection) {
                map.computeIfAbsent(tick, ArrayList::new).add(slotSelection);
            } else if (slotSelection instanceof SlotRangeSelection slotRangeSelection) {
                List<Integer> slots = createList(slotRangeSelection.fromSlot.get(), slotRangeSelection.toSlot.get());
                for (int slot : slots) {
                    SlotRangeSelection copy = slotRangeSelection.copy();
                    copy.calculatedSlot = slot;
                    map.computeIfAbsent(tick, ArrayList::new).add(copy);
                    if (slots.indexOf(slot) < slots.size() - 1) {
                        tick += copy.delay.get();
                    }
                }
            }
        }

        for (Map.Entry<Integer, List<BaseSlotSelection>> entry : map.entrySet()) {
            if (startTick + entry.getKey() == currentTick) {
                for (BaseSlotSelection baseSlotSelection : entry.getValue()) {
                    if (baseSlotSelection instanceof DefaultSlotSelection defaultSlotSelection) {
                        printInfo("Slot selection: " + defaultSlotSelection.getTypeName());
                        Screen screen = mc.currentScreen;
                        ScreenHandler screenHandler = mc.player.currentScreenHandler;

                        if (defaultSlotSelection.checkContainerType.get()) {
                            printInfo("Checking container type...");
                            try {
                                boolean containerTypeMatched;
                                if (defaultSlotSelection.containerTypeMode.get() == DefaultSlotSelection.ContainerTypeMode.Whitelist) {
                                    containerTypeMatched = defaultSlotSelection.containerType.get().contains(screenHandler.getType());
                                } else {
                                    containerTypeMatched = !defaultSlotSelection.containerType.get().contains(screenHandler.getType());
                                }
                                if (containerTypeMatched) {
                                    printInfo("Container type matched!");
                                } else {
                                    printWarning("Container type not matched!");
                                    continue;
                                }
                            } catch (Exception e) {
                                printError(e.getMessage());
                                if (!defaultSlotSelection.ignoreMenuTypeMismatch.get()) continue;
                            }
                        }

                        if (defaultSlotSelection.checkScreenName.get()) {
                            printInfo("Checking screen name...");
                            try {
                                Pattern screenNamePattern = Pattern.compile(defaultSlotSelection.screenName.get());
                                String screenName = screen != null ? screen.getTitle().getString() : "null";
                                printInfo("Screen name: " + screenName);
                                printInfo("Regular expression: " + screenNamePattern.pattern());

                                boolean checkScreenName = screenNamePattern.matcher(screenName).find();
                                if (checkScreenName) {
                                    printInfo("Screen name matched!");
                                } else {
                                    printWarning("Screen name not matched!");
                                    continue;
                                }
                            } catch (Exception e) {
                                printError(e.getMessage());
                            }
                        }

                        if (defaultSlotSelection.checkSlotItemData.get()) {
                            printInfo("Checking slot item data...");
                            int slot = 0;
                            switch (defaultSlotSelection) {
                                case SingleSlotSelection singleSlotSelection -> slot = singleSlotSelection.slot.get();
                                case SwapSlotSelection swapSlotSelection -> slot = swapSlotSelection.fromSlot.get();
                                case SlotRangeSelection slotRangeSelection -> slot = slotRangeSelection.calculatedSlot;
                                default -> {
                                }
                            }

                            try {
                                ItemStack itemStack = screenHandler.getSlot(slot).getStack();
                                if (!itemStack.isEmpty()) {
                                    NbtElement element = itemStack.toNbt(mc.player.getRegistryManager());
                                    printInfo("Item data: " + element.asString());

                                    boolean matchedAny = false;
                                    boolean matchedAll = true;

                                    for (Pair<String, String> pair : defaultSlotSelection.slotItemData.get()) {
                                        try {
                                            String value = NbtPathArgumentType.NbtPath.parse(pair.getLeft()).get(element).getFirst().asString();
                                            Pattern pattern = Pattern.compile(pair.getRight());
                                            printInfo("Element: " + value);
                                            printInfo("Pattern: " + pattern.pattern());
                                            if (pattern.matcher(value).find()) {
                                                matchedAny = true;
                                                printInfo("Matched!");
                                            } else {
                                                matchedAll = false;
                                                printWarning("Not matched!");
                                            }
                                        } catch (Exception e) {
                                            printError(e.getMessage());
                                        }
                                    }

                                    boolean isAnyMatch = defaultSlotSelection.slotItemMatchMode.get() == DefaultSlotSelection.SlotItemMatchMode.Any;
                                    if (!(isAnyMatch && matchedAny || !isAnyMatch && matchedAll)) continue;
                                } else {
                                    printWarning("There is no item in the slot " + slot + "!");
                                }
                            } catch (Exception e) {
                                printError(e.getMessage());
                            }
                        }

                        if (mc.interactionManager != null && screenHandler != null) {
                            try {
                                switch (defaultSlotSelection) {
                                    case SingleSlotSelection singleSlotSelection ->
                                            mc.interactionManager.clickSlot(screenHandler.syncId, singleSlotSelection.slot.get(), singleSlotSelection.button.get(), singleSlotSelection.action.get(), mc.player);
                                    case SwapSlotSelection swapSlotSelection -> {
                                        mc.interactionManager.clickSlot(screenHandler.syncId, swapSlotSelection.fromSlot.get(), 0, SlotActionType.PICKUP, mc.player);
                                        mc.interactionManager.clickSlot(screenHandler.syncId, swapSlotSelection.toSlot.get(), 0, SlotActionType.PICKUP, mc.player);
                                    }
                                    case SlotRangeSelection slotRangeSelection ->
                                            mc.interactionManager.clickSlot(screenHandler.syncId, slotRangeSelection.calculatedSlot, slotRangeSelection.button.get(), slotRangeSelection.action.get(), mc.player);
                                    default -> {
                                    }
                                }
                            } catch (Exception e) {
                                printError(e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        if (startTick + tick <= currentTick) {
            if (disableAfterIteration.get()) {
                toggle();
            } else {
                startTick = -1;
            }
        }
    }

    private void printInfo(String message) {
        if (printDebugInfo.get()) info(message);
    }

    private void printWarning(String message) {
        if (printDebugInfo.get()) warning(message);
    }

    private void printError(String message) {
        if (printDebugInfo.get()) error(message);
    }
}