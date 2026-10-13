package zgoly.meteorist.modules.inventoryorganizer;

import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.gui.screens.SlotRuleScreen;
import zgoly.meteorist.utils.config.MeteoristConfigManager;
import zgoly.meteorist.utils.misc.DebugLogger;
import zgoly.meteorist.utils.misc.Filterist;

import java.util.*;

import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.COPY;
import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.EDIT;
import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class InventoryOrganizer extends Module {
    public final List<SlotRule> slotRules = new ArrayList<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSearch = settings.createGroup("Search");

    private final Setting<ExecutionMode> executionMode = sgGeneral.add(new EnumSetting.Builder<ExecutionMode>()
            .name("execution-mode")
            .description("Continuous: organize every tick. OnEvent: on container close or item pickup. Once: run once and disable.")
            .defaultValue(ExecutionMode.Continuous)
            .build()
    );
    private final Setting<Boolean> triggerOnContainerClose = sgGeneral.add(new BoolSetting.Builder()
            .name("trigger-on-container-close")
            .description("Automatically organize inventory when closing containers.")
            .defaultValue(true)
            .visible(() -> executionMode.get() == ExecutionMode.OnEvent)
            .build()
    );
    private final Setting<Boolean> triggerOnPickup = sgGeneral.add(new BoolSetting.Builder()
            .name("trigger-on-pickup")
            .description("Automatically organize inventory when items are picked up.")
            .defaultValue(true)
            .visible(() -> executionMode.get() == ExecutionMode.OnEvent)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks between sorts (1 = every tick).")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .build()
    );
    private final Setting<Integer> maxSortsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-sorts-per-tick")
            .description("Maximum number of swaps to perform per tick.")
            .defaultValue(1)
            .min(1)
            .build()
    );
    private final Setting<Boolean> preventDropSelected = sgGeneral.add(new BoolSetting.Builder()
            .name("prevent-drop-selected")
            .description("Prevents dropping selected items (e.g., via Q key) that match active slot rules.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> searchHotbar = sgSearch.add(new BoolSetting.Builder()
            .name("hotbar")
            .description("Search for items to sort in the hotbar (slots 0-8).")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchMainInventory = sgSearch.add(new BoolSetting.Builder()
            .name("main-inventory")
            .description("Search for items to sort in the main inventory (slots 9-35).")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchArmor = sgSearch.add(new BoolSetting.Builder()
            .name("armor")
            .description("Search for items to sort in the armor slots (36-39).")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchOffhand = sgSearch.add(new BoolSetting.Builder()
            .name("offhand")
            .description("Search for items to sort in the offhand slot (40).")
            .defaultValue(true)
            .build()
    );

    private final DebugLogger debugLogger;
    private final Set<Integer> claimedSlots = new HashSet<>();

    private int delayTimer = 0;
    private boolean wasInContainer = false;
    private boolean pendingOrganizeOnEvent = false;

    public InventoryOrganizer() {
        super(Meteorist.CATEGORY, "inventory-organizer", "Organizes inventory slots based on filters.");
        debugLogger = new DebugLogger(this, settings);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        ListTag list = new ListTag();

        for (SlotRule rule : slotRules) {
            CompoundTag rTag = new CompoundTag();
            rTag.put("rule", rule.toTag());
            list.add(rTag);
        }

        tag.put("slotRules", list);
        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        super.fromTag(tag);
        slotRules.clear();

        ListTag list = tag.getListOrEmpty("slotRules");
        for (Tag itemTag : list) {
            CompoundTag rTag = (CompoundTag) itemTag;
            SlotRule rule = new SlotRule();
            CompoundTag ruleTag = (CompoundTag) rTag.get("rule");
            if (ruleTag != null) rule.fromTag(ruleTag);
            slotRules.add(rule);
        }

        return this;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        boolean inContainerScreen = mc.gui.screen() instanceof AbstractContainerScreen && !(mc.gui.screen() instanceof InventoryScreen);
        if (wasInContainer && !inContainerScreen && triggerOnContainerClose.get() && executionMode.get() == ExecutionMode.OnEvent) {
            pendingOrganizeOnEvent = true;
        }
        wasInContainer = inContainerScreen;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        if (executionMode.get() == ExecutionMode.Continuous) {
            organizeInventory();
        } else if (executionMode.get() == ExecutionMode.Once) {
            if (!organizeInventory()) toggle();
        } else if (executionMode.get() == ExecutionMode.OnEvent && pendingOrganizeOnEvent) {
            if (!organizeInventory()) pendingOrganizeOnEvent = false;
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (executionMode.get() == ExecutionMode.OnEvent && triggerOnPickup.get() && event.packet instanceof ClientboundContainerSetSlotPacket) {
            pendingOrganizeOnEvent = true;
            delayTimer = delay.get() - 1;
        }
    }

    @EventHandler
    private void onDropItems(DropItemsEvent event) {
        if (!preventDropSelected.get() || mc.player == null) return;

        ItemStack stack = event.itemStack;
        if (stack != null && !stack.isEmpty()) {
            Filterist.clearNbtCache();
            for (SlotRule rule : slotRules) {
                if (rule.matches(stack)) {
                    event.cancel();
                    debugLogger.info("Prevented dropping rule-matched item: (highlight)%s", stack.getItem().toString());
                    break;
                }
            }
        }
    }

    public boolean organizeInventory() {
        if (mc.player == null || mc.gameMode == null || slotRules.isEmpty()) return false;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return false;

        Filterist.clearNbtCache();
        claimedSlots.clear();

        int ruleCount = slotRules.size();

        int[] bestCandidate = new int[ruleCount];
        boolean[] ruleSatisfied = new boolean[ruleCount];
        Arrays.fill(bestCandidate, -1);

        for (int r = 0; r < ruleCount; r++) {
            SlotRule rule = slotRules.get(r);
            int targetMcSlot = playerSlotToContainerSlot(rule.targetSlot.get());
            if (targetMcSlot == -1) continue;

            ItemStack stack = getStackInSlot(targetMcSlot);
            if (!stack.isEmpty() && rule.matches(stack)) {
                ruleSatisfied[r] = true;
                claimedSlots.add(targetMcSlot);
            }
        }

        for (int s = 0; s < 45; s++) {
            if (!isSearchSlot(s)) continue;

            int mcSlot = playerSlotToContainerSlot(s);
            if (mcSlot == -1 || claimedSlots.contains(mcSlot)) continue;

            ItemStack stack = getStackInSlot(mcSlot);
            if (stack.isEmpty()) continue;

            for (int r = 0; r < ruleCount; r++) {
                if (ruleSatisfied[r] || bestCandidate[r] != -1) continue;

                int targetMcSlot = playerSlotToContainerSlot(slotRules.get(r).targetSlot.get());
                if (claimedSlots.contains(targetMcSlot)) continue;

                if (slotRules.get(r).matches(stack)) {
                    bestCandidate[r] = mcSlot;
                    break;
                }
            }
        }

        boolean didSwap = false;
        int sortsThisTick = 0;

        for (int r = 0; r < ruleCount; r++) {
            if (ruleSatisfied[r]) continue;

            SlotRule rule = slotRules.get(r);
            int targetMcSlot = playerSlotToContainerSlot(rule.targetSlot.get());
            if (targetMcSlot == -1 || claimedSlots.contains(targetMcSlot)) continue;

            int sourceSlot = bestCandidate[r];

            if (sourceSlot != -1 && sourceSlot != targetMcSlot && !claimedSlots.contains(sourceSlot)) {
                swapSlots(sourceSlot, rule.targetSlot.get());
                claimedSlots.add(targetMcSlot);
                claimedSlots.remove(sourceSlot);
                didSwap = true;
                sortsThisTick++;

                debugLogger.info("Swapped slot (highlight)%d(default) for rule (highlight)%d", sourceSlot, targetMcSlot, r);

                if (sortsThisTick >= maxSortsPerTick.get()) {
                    delayTimer = delay.get() - 1;
                    return true;
                }
            }
        }

        if (didSwap && executionMode.get() != ExecutionMode.Continuous) {
            delayTimer = delay.get() - 1;
        }

        return false;
    }

    private boolean isSearchSlot(int playerSlot) {
        if (searchHotbar.get() && SlotUtils.isHotbar(playerSlot)) return true;
        if (searchMainInventory.get() && SlotUtils.isMain(playerSlot)) return true;
        if (searchArmor.get() && SlotUtils.isArmor(playerSlot)) return true;
        return searchOffhand.get() && playerSlot == SlotUtils.OFFHAND;
    }

    private int playerSlotToContainerSlot(int playerSlot) {
        if (playerSlot < 0) return -1;
        return SlotUtils.indexToId(playerSlot);
    }

    private ItemStack getStackInSlot(int mcSlot) {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.containerMenu.getSlot(mcSlot).getItem();
    }

    private void swapSlots(int fromMcSlot, int targetPlayerSlot) {
        if (mc.gameMode == null || mc.player == null) return;

        int containerId = mc.player.containerMenu.containerId;

        if (SlotUtils.isHotbar(targetPlayerSlot) || targetPlayerSlot == SlotUtils.OFFHAND) {
            debugLogger.info("Hotbar-swapping item from slot (highlight)%d(default) into (highlight)%d(default)", fromMcSlot, targetPlayerSlot);
            mc.gameMode.handleContainerInput(containerId, fromMcSlot, targetPlayerSlot, ContainerInput.SWAP, mc.player);
            return;
        }

        int toMcSlot = playerSlotToContainerSlot(targetPlayerSlot);
        if (toMcSlot == -1) return;

        debugLogger.info("Swapping item from slot (highlight)%d(default) to slot (highlight)%d(default)", fromMcSlot, toMcSlot);
        mc.gameMode.handleContainerInput(containerId, fromMcSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, toMcSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, fromMcSlot, 0, ContainerInput.PICKUP, mc.player);
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

        for (SlotRule rule : slotRules) {
            WTextBox name = table.add(theme.textBox(rule.name.get(), "Rule #" + (slotRules.indexOf(rule) + 1))).expandX().widget();
            name.tooltip = rule.name.description;
            name.actionOnUnfocused = () -> rule.name.set(name.get());

            WContainer infoContainer = table.add(theme.horizontalList()).expandX().widget();
            WIntEdit targetSlotEdit = infoContainer.add(theme.intEdit(rule.targetSlot.get(), 0, 40, true)).widget();
            targetSlotEdit.tooltip = rule.targetSlot.description;
            targetSlotEdit.actionOnRelease = () -> rule.targetSlot.set(targetSlotEdit.get());

            WButton edit = table.add(theme.button(EDIT)).widget();
            edit.tooltip = "Edit the rule.";
            edit.action = () -> mc.gui.setScreen(new SlotRuleScreen(theme, rule));

            if (slotRules.size() > 1) {
                WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
                int index = slotRules.indexOf(rule);

                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move rule up.";
                    moveUp.action = () -> {
                        slotRules.remove(index);
                        slotRules.add(index - 1, rule);
                        fillWidget(theme, list);
                    };
                }

                if (index < slotRules.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move rule down.";
                    moveDown.action = () -> {
                        slotRules.remove(index);
                        slotRules.add(index + 1, rule);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = table.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate rule.";
            copy.action = () -> {
                SlotRule newRule = rule.copy();
                slotRules.add(slotRules.indexOf(rule) + 1, newRule);
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove rule.";
            remove.action = () -> {
                slotRules.remove(rule);
                fillWidget(theme, list);
            };

            table.row();
        }

        if (!slotRules.isEmpty()) list.add(theme.horizontalSeparator()).expandX();

        WTable controls = list.add(theme.table()).expandX().widget();

        WButton addSimple = controls.add(theme.button("New Simple Rule")).expandX().widget();
        addSimple.tooltip = "Add a new simple rule.";
        addSimple.action = () -> {
            slotRules.add(new SlotRule(SlotRule.Mode.Simple));
            fillWidget(theme, list);
        };

        WButton addAdvanced = controls.add(theme.button("New Advanced Rule")).expandX().widget();
        addAdvanced.tooltip = "Add a new advanced rule.";
        addAdvanced.action = () -> {
            slotRules.add(new SlotRule(SlotRule.Mode.Advanced));
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Rules")).expandX().widget();
        removeAll.tooltip = "Remove all rules.";
        removeAll.action = () -> {
            slotRules.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this,
                MeteoristConfigManager.PresetConfig.of("fr13nd001's grief", mod -> {
                    InventoryOrganizer organizer = (InventoryOrganizer) mod;
                    organizer.slotRules.clear();

                    SlotRule food = new SlotRule(SlotRule.Mode.Advanced);
                    food.name.set("Any Food");
                    food.targetSlot.set(0);
                    food.expression.set("{item.nbt_verbose.path(\"components.\\\"minecraft:food\\\"\") != null}");
                    organizer.slotRules.add(food);

                    SlotRule sword = new SlotRule(SlotRule.Mode.Advanced);
                    sword.name.set("Sword");
                    sword.targetSlot.set(1);
                    sword.expression.set("{contains(item.id, \"_sword\")}");
                    organizer.slotRules.add(sword);

                    SlotRule silk = new SlotRule(SlotRule.Mode.Advanced);
                    silk.name.set("Silk pick");
                    silk.targetSlot.set(2);
                    silk.expression.set("{contains(item.id, \"_pickaxe\") and item.nbt.path(\"components.\\\"minecraft:enchantments\\\".\\\"minecraft:silk_touch\\\"\") != null}");
                    organizer.slotRules.add(silk);

                    SlotRule axe = new SlotRule(SlotRule.Mode.Advanced);
                    axe.name.set("Axe");
                    axe.targetSlot.set(3);
                    axe.expression.set("{contains(item.id, \"_axe\")}");
                    organizer.slotRules.add(axe);

                    SlotRule shovel = new SlotRule(SlotRule.Mode.Advanced);
                    shovel.name.set("Shovel");
                    shovel.targetSlot.set(4);
                    shovel.expression.set("{contains(item.id, \"_shovel\")}");
                    organizer.slotRules.add(shovel);

                    SlotRule lighter = new SlotRule(SlotRule.Mode.Simple);
                    lighter.name.set("Lighter");
                    lighter.targetSlot.set(5);
                    lighter.items.set(new ArrayList<>(List.of(Items.FLINT_AND_STEEL)));
                    organizer.slotRules.add(lighter);

                    SlotRule tnt = new SlotRule(SlotRule.Mode.Simple);
                    tnt.name.set("TNT");
                    tnt.targetSlot.set(6);
                    tnt.items.set(new ArrayList<>(List.of(Items.TNT)));
                    organizer.slotRules.add(tnt);

                    SlotRule lava = new SlotRule(SlotRule.Mode.Simple);
                    lava.name.set("Lava");
                    lava.targetSlot.set(7);
                    lava.items.set(new ArrayList<>(List.of(Items.LAVA_BUCKET)));
                    organizer.slotRules.add(lava);

                    SlotRule water = new SlotRule(SlotRule.Mode.Simple);
                    water.name.set("Water");
                    water.targetSlot.set(8);
                    water.items.set(new ArrayList<>(List.of(Items.WATER_BUCKET)));
                    organizer.slotRules.add(water);
                }),

                MeteoristConfigManager.PresetConfig.of("Zgoly's Anarchy", mod -> {
                    InventoryOrganizer organizer = (InventoryOrganizer) mod;
                    organizer.slotRules.clear();

                    SlotRule food = new SlotRule(SlotRule.Mode.Advanced);
                    food.name.set("Any Food");
                    food.targetSlot.set(0);
                    food.expression.set("{item.nbt_verbose.path(\"components.\\\"minecraft:food\\\"\") != null}");
                    organizer.slotRules.add(food);

                    SlotRule sword = new SlotRule(SlotRule.Mode.Advanced);
                    sword.name.set("Any Sword");
                    sword.targetSlot.set(1);
                    sword.expression.set("{contains(item.id, \"_sword\")}");
                    organizer.slotRules.add(sword);

                    SlotRule fortune = new SlotRule(SlotRule.Mode.Advanced);
                    fortune.name.set("Fortune Pickaxe");
                    fortune.targetSlot.set(2);
                    fortune.expression.set("{contains(item.id, \"_pickaxe\") and item.nbt.path(\"components.\\\"minecraft:enchantments\\\".\\\"minecraft:fortune\\\"\") != null}");
                    organizer.slotRules.add(fortune);

                    SlotRule silk = new SlotRule(SlotRule.Mode.Advanced);
                    silk.name.set("Silk Touch Pickaxe");
                    silk.targetSlot.set(3);
                    silk.expression.set("{contains(item.id, \"_pickaxe\") and item.nbt.path(\"components.\\\"minecraft:enchantments\\\".\\\"minecraft:silk_touch\\\"\") != null}");
                    organizer.slotRules.add(silk);

                    SlotRule axe = new SlotRule(SlotRule.Mode.Advanced);
                    axe.name.set("Any Axe");
                    axe.targetSlot.set(4);
                    axe.expression.set("{contains(item.id, \"_axe\")}");
                    organizer.slotRules.add(axe);

                    SlotRule shovel = new SlotRule(SlotRule.Mode.Advanced);
                    shovel.name.set("Any Shovel");
                    shovel.targetSlot.set(5);
                    shovel.expression.set("{contains(item.id, \"_shovel\")}");
                    organizer.slotRules.add(shovel);

                    SlotRule bow = new SlotRule(SlotRule.Mode.Simple);
                    bow.name.set("Bow");
                    bow.targetSlot.set(6);
                    bow.items.set(new ArrayList<>(List.of(Items.BOW)));
                    organizer.slotRules.add(bow);

                    SlotRule obsidian = new SlotRule(SlotRule.Mode.Simple);
                    obsidian.name.set("Obsidian");
                    obsidian.targetSlot.set(7);
                    obsidian.items.set(new ArrayList<>(List.of(Items.OBSIDIAN)));
                    organizer.slotRules.add(obsidian);

                    SlotRule crystal = new SlotRule(SlotRule.Mode.Simple);
                    crystal.name.set("End Crystal");
                    crystal.targetSlot.set(8);
                    crystal.items.set(new ArrayList<>(List.of(Items.END_CRYSTAL)));
                    organizer.slotRules.add(crystal);
                }),

                MeteoristConfigManager.PresetConfig.of("Zgoly's PvE", mod -> {
                    InventoryOrganizer organizer = (InventoryOrganizer) mod;
                    organizer.slotRules.clear();

                    SlotRule food = new SlotRule(SlotRule.Mode.Advanced);
                    food.name.set("Any Food");
                    food.targetSlot.set(0);
                    food.expression.set("{item.nbt_verbose.path(\"components.\\\"minecraft:food\\\"\") != null}");
                    organizer.slotRules.add(food);

                    SlotRule sword = new SlotRule(SlotRule.Mode.Advanced);
                    sword.name.set("Any Sword");
                    sword.targetSlot.set(1);
                    sword.expression.set("{contains(item.id, \"_sword\")}");
                    organizer.slotRules.add(sword);

                    SlotRule fortune = new SlotRule(SlotRule.Mode.Advanced);
                    fortune.name.set("Fortune Pickaxe");
                    fortune.targetSlot.set(2);
                    fortune.expression.set("{contains(item.id, \"_pickaxe\") and item.nbt.path(\"components.\\\"minecraft:enchantments\\\".\\\"minecraft:fortune\\\"\") != null}");
                    organizer.slotRules.add(fortune);

                    SlotRule silk = new SlotRule(SlotRule.Mode.Advanced);
                    silk.name.set("Silk Touch Pickaxe");
                    silk.targetSlot.set(3);
                    silk.expression.set("{contains(item.id, \"_pickaxe\") and item.nbt.path(\"components.\\\"minecraft:enchantments\\\".\\\"minecraft:silk_touch\\\"\") != null}");
                    organizer.slotRules.add(silk);

                    SlotRule axe = new SlotRule(SlotRule.Mode.Advanced);
                    axe.name.set("Any Axe");
                    axe.targetSlot.set(4);
                    axe.expression.set("{contains(item.id, \"_axe\")}");
                    organizer.slotRules.add(axe);

                    SlotRule chestplate = new SlotRule(SlotRule.Mode.Advanced);
                    chestplate.name.set("Any Chestplate");
                    chestplate.targetSlot.set(5);
                    chestplate.expression.set("{contains(item.id, \"_chestplate\")}");
                    organizer.slotRules.add(chestplate);

                    SlotRule fireworks = new SlotRule(SlotRule.Mode.Advanced);
                    fireworks.name.set("Fireworks Level 3");
                    fireworks.targetSlot.set(6);
                    fireworks.expression.set("{contains(item.id, \"firework_rocket\") and item.nbt.path(\"components.\\\"minecraft:fireworks\\\".flight_duration\").number == 3}");
                    organizer.slotRules.add(fireworks);

                    SlotRule cobble = new SlotRule(SlotRule.Mode.Simple);
                    cobble.name.set("Cobblestone");
                    cobble.targetSlot.set(7);
                    cobble.items.set(new ArrayList<>(List.of(Items.COBBLESTONE)));
                    organizer.slotRules.add(cobble);

                    SlotRule bucket = new SlotRule(SlotRule.Mode.Simple);
                    bucket.name.set("Water Bucket or Empty Bucket");
                    bucket.targetSlot.set(8);
                    bucket.items.set(new ArrayList<>(List.of(Items.WATER_BUCKET, Items.BUCKET)));
                    organizer.slotRules.add(bucket);
                })
        );
    }

    public enum ExecutionMode {
        Continuous,
        OnEvent,
        Once
    }
}
