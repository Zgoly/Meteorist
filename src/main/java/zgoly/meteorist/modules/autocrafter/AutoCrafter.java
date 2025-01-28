package zgoly.meteorist.modules.autocrafter;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.gui.screens.AutoCraftScreen;
import zgoly.meteorist.modules.autocrafter.autocrafts.BaseAutoCraft;
import zgoly.meteorist.modules.autocrafter.autocrafts.CraftingTableAutoCraft;
import zgoly.meteorist.modules.autocrafter.autocrafts.InventoryAutoCraft;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static zgoly.meteorist.Meteorist.*;

public class AutoCrafter extends Module {
    private final AutoCraftFactory factory = new AutoCraftFactory();
    private final List<BaseAutoCraft> autoCrafts = new ArrayList<>();
    private BaseAutoCraft currentCraft = null;

    public AutoCrafter() {
        super(Meteorist.CATEGORY, "auto-crafter", "Automatically craft items.");
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BaseAutoCraft autoCraft : autoCrafts) {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", autoCraft.getTypeName());
            mTag.put("autoCraft", autoCraft.toTag());

            list.add(mTag);
        }
        tag.put("autoCrafts", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        autoCrafts.clear();
        NbtList list = tag.getList("autoCrafts", NbtElement.COMPOUND_TYPE);

        for (NbtElement tagI : list) {
            NbtCompound tagII = (NbtCompound) tagI;

            String type = tagII.getString("type");
            BaseAutoCraft autoCraft = factory.createAutoCraft(type);

            if (autoCraft != null) {
                NbtCompound autoCraftTag = tagII.getCompound("autoCraft");
                if (autoCraftTag != null) autoCraft.fromTag(autoCraftTag);

                autoCrafts.add(autoCraft);
            }
        }

        return this;
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

        for (BaseAutoCraft autoCraft : autoCrafts) {
            table.add(theme.label(autoCraft.getTypeName())).expandX().widget();

            WHorizontalList container = table.add(theme.horizontalList()).expandX().widget();
            WTable ingredients = container.add(theme.table()).expandX().widget();

            if (autoCraft instanceof InventoryAutoCraft inventoryAutoCraft) {
                addIngredients(theme, ingredients, inventoryAutoCraft.ingredients, 2);
            } else if (autoCraft instanceof CraftingTableAutoCraft craftingTableAutoCraft) {
                addIngredients(theme, ingredients, craftingTableAutoCraft.ingredients, 3);
            }

            if (autoCraft.checkOutputItem.get()) {
                container.add(theme.label(">")).expandX();
                WItem outputItem = theme.item(autoCraft.getOutputItem().get().getDefaultStack());
                outputItem.tooltip = autoCraft.getOutputItem().description;
                container.add(outputItem).expandX();
            }

            WTable conditions = container.add(theme.table()).expandX().widget();
            addCheckbox(theme, conditions, autoCraft.enabled);
            addCheckbox(theme, conditions, autoCraft.oneMore);
            addCheckbox(theme, conditions, autoCraft.clearSlots);

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (autoCrafts.size() > 1) {
                int index = autoCrafts.indexOf(autoCraft);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move auto craft up.";
                    moveUp.action = () -> {
                        autoCrafts.remove(index);
                        autoCrafts.add(index - 1, autoCraft);
                        fillWidget(theme, list);
                    };
                }

                if (index < autoCrafts.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move auto craft down.";
                    moveDown.action = () -> {
                        autoCrafts.remove(index);
                        autoCrafts.add(index + 1, autoCraft);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton editButton = table.add(theme.button("Edit")).expandX().widget();
            editButton.tooltip = "Edit auto craft.";
            editButton.action = () -> mc.setScreen(new AutoCraftScreen(theme, autoCraft));

            WButton copyButton = table.add(theme.button(COPY)).widget();
            copyButton.tooltip = "Copy auto craft.";
            copyButton.action = () -> {
                autoCrafts.add(autoCrafts.indexOf(autoCraft), autoCraft.copy());
                fillWidget(theme, list);
            };

            WMinus removeButton = table.add(theme.minus()).widget();
            removeButton.tooltip = "Remove auto craft.";
            removeButton.action = () -> {
                autoCrafts.remove(autoCraft);
                fillWidget(theme, list);
            };

            table.row();
        }

        list.add(theme.horizontalSeparator()).expandX();
        WTable controls = list.add(theme.table()).expandX().widget();

        WButton newInventoryButton = controls.add(theme.button("Inventory Auto Craft")).expandX().widget();
        newInventoryButton.action = () -> {
            autoCrafts.add(new InventoryAutoCraft());
            fillWidget(theme, list);
        };

        WButton newCraftingTableButton = controls.add(theme.button("Crafting Table Auto Craft")).expandX().widget();
        newCraftingTableButton.action = () -> {
            autoCrafts.add(new CraftingTableAutoCraft());
            fillWidget(theme, list);
        };

        WButton removeAllButton = controls.add(theme.button("Remove All Auto Crafts")).expandX().widget();
        removeAllButton.action = () -> {
            autoCrafts.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    private void addIngredients(GuiTheme theme, WTable ingredients, List<Setting<Item>> ingredientsList, int rowSize) {
        for (int i = 0; i < ingredientsList.size(); i++) {
            WItem ingredientItem = theme.item(ingredientsList.get(i).get().getDefaultStack());
            ingredientItem.tooltip = ingredientsList.get(i).description;
            ingredients.add(ingredientItem).widget();
            if ((i + 1) % rowSize == 0 && i < ingredientsList.size() - 1) {
                ingredients.row();
            }
        }
    }

    private void addCheckbox(GuiTheme theme, WTable conditions, Setting<Boolean> setting) {
        WCheckbox checkbox = conditions.add(theme.checkbox(setting.get())).widget();
        checkbox.action = () -> setting.set(!setting.get());
        WLabel label = conditions.add(theme.label(setting.title)).widget();
        label.tooltip = setting.description;
        conditions.row();
    }

    public void onDeactivate() {
        currentCraft = null;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        currentCraft = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;

        if (currentCraft == null) {
            for (BaseAutoCraft autoCraft : autoCrafts) {
                if ((autoCraft instanceof InventoryAutoCraft && currentScreenHandler instanceof PlayerScreenHandler) ||
                        (autoCraft instanceof CraftingTableAutoCraft && currentScreenHandler instanceof CraftingScreenHandler)) {
                    if (checkAutoCraft(autoCraft)) {
                        currentCraft = autoCraft;
                        break;
                    }
                }
            }
        }

        if (!(currentScreenHandler instanceof PlayerScreenHandler) && !(currentScreenHandler instanceof CraftingScreenHandler)) {
            currentCraft = null;
            return;
        }

        if (currentCraft != null) {
            if (currentCraft instanceof InventoryAutoCraft inventoryAutoCraft && currentScreenHandler instanceof PlayerScreenHandler screen) {
                processAutoCraft(inventoryAutoCraft, screen);
            } else if (currentCraft instanceof CraftingTableAutoCraft craftingTableAutoCraft && currentScreenHandler instanceof CraftingScreenHandler screen) {
                processAutoCraft(craftingTableAutoCraft, screen);
            }
        }
    }

    private void processAutoCraft(BaseAutoCraft autoCraft, ScreenHandler screen) {
        int totalActions = 0;
        for (Setting<Item> ingredient : autoCraft.ingredients) {
            if (totalActions >= autoCraft.getActionsPerTick()) return;
            Item item = ingredient.get();
            if (item != Items.STRUCTURE_VOID) {
                int toSlot = autoCraft.ingredients.indexOf(ingredient) + 1;
                ItemStack toSlotStack = screen.slots.get(toSlot).getStack();
                if (toSlotStack != null && toSlotStack.getItem() != item) {
                    FindItemResult result = findItem(item, autoCraft);
                    if (result.found()) {
                        if (autoCraft.clearSlots.get()) {
                            mc.interactionManager.clickSlot(screen.syncId, toSlot, autoCraft.clearSlotsButton.get(), autoCraft.clearSlotsAction.get(), mc.player);
                        }
                        mc.interactionManager.clickSlot(screen.syncId, result.slotId, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(screen.syncId, toSlot, 1, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(screen.syncId, result.slotId, 0, SlotActionType.PICKUP, mc.player);
                        totalActions++;
                    }
                }
            }
        }

        if (totalActions == 0) {
            Slot outputSlot = screen.getSlot(0);
            if (!outputSlot.getStack().isEmpty()) {
                if (autoCraft.checkOutputItem.get() && outputSlot.getStack().getItem() != autoCraft.getOutputItem().get())
                    return;
                mc.interactionManager.clickSlot(screen.syncId, outputSlot.id, autoCraft.outputButton.get(), autoCraft.outputAction.get(), mc.player);
                currentCraft = null;
            }
        }
    }

    public boolean checkAutoCraft(BaseAutoCraft autoCraft) {
        if (!autoCraft.enabled.get()) return false;

        Map<Item, Integer> requiredItems = new HashMap<>();

        autoCraft.ingredients.forEach(ingredient -> {
            if (ingredient.get() != Items.STRUCTURE_VOID) {
                requiredItems.put(ingredient.get(), requiredItems.getOrDefault(ingredient.get(), 0) + 1);
            }
        });

        for (Map.Entry<Item, Integer> entry : requiredItems.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();
            if (autoCraft.oneMore.get()) {
                count++;
            }

            FindItemResult result = findItem(item, autoCraft);

            if (!result.found() || result.totalCount < count) {
                return false;
            }
        }

        return true;
    }


    public FindItemResult findItem(Item item, BaseAutoCraft autoCraft) {
        if (mc.player == null) return new FindItemResult(-1, 0);

        int slotId = -1, totalCount = 0;
        ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (screenHandler instanceof CraftingScreenHandler || screenHandler instanceof PlayerScreenHandler) {
            for (Slot slot : screenHandler.slots) {
                // Determine the type of screen handler and skip the appropriate slots
                if (screenHandler instanceof CraftingScreenHandler && slot.id < 9) continue;
                if (screenHandler instanceof PlayerScreenHandler && slot.id < 4) continue;

                ItemStack stack = slot.getStack();
                if (stack != null && stack.getItem() == item) {
                    int count = stack.getCount();
                    if (autoCraft.keepSingleStacks.get() && count == 1) continue;
                    if (slotId == -1) slotId = slot.id;
                    totalCount += count;
                }
            }
        }

        return new FindItemResult(slotId, totalCount);
    }

    public record FindItemResult(int slotId, int totalCount) {
        public boolean found() {
            return slotId != -1;
        }
    }
}
