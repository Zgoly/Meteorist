package zgoly.meteorist.modules.autocrafter;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.mixin.ClientRecipeBookAccessor;
import zgoly.meteorist.mixin.IngredientAccessor;
import zgoly.meteorist.mixin.RecipeFinderAccessor;
import zgoly.meteorist.utils.config.MeteoristConfigManager;
import zgoly.meteorist.utils.misc.DebugLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.COPY;
import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;
import static zgoly.meteorist.utils.MeteoristUtils.canDisplayRecipe;

public class AutoCrafter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> fixDesync = sgGeneral.add(new BoolSetting.Builder()
            .name("fix-desync")
            .description("Attempts to fix inventory desync when crafting. Use it only if you have a desync issue.")
            .defaultValue(false)
            .build()
    );

    public static List<BaseAutoCraft> autoCrafts = new ArrayList<>();
    public boolean shouldCraft = false;
    public ClickType actionType;

    private final DebugLogger debugLogger;

    public AutoCrafter() {
        super(Meteorist.CATEGORY, "auto-crafter", "Automatically craft items.");

        debugLogger = new DebugLogger(this, settings);
    }

    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        ListTag list = new ListTag();
        for (BaseAutoCraft autoCraft : autoCrafts) {
            CompoundTag mTag = new CompoundTag();
            mTag.put("autoCraft", autoCraft.toTag());

            list.add(mTag);
        }

        tag.put("autoCrafts", list);
        return tag;
    }

    public Module fromTag(CompoundTag tag) {
        super.fromTag(tag);

        autoCrafts.clear();
        ListTag list = tag.getListOrEmpty("autoCrafts");
        for (Tag tagII : list) {
            CompoundTag tagI = (CompoundTag) tagII;

            BaseAutoCraft autoCraft = new BaseAutoCraft();
            CompoundTag autoCraftTag = (CompoundTag) tagI.get("autoCraft");

            if (autoCraftTag != null) autoCraft.fromTag(autoCraftTag);

            autoCrafts.add(autoCraft);
        }

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        for (BaseAutoCraft autoCraft : autoCrafts) {
            list.add(theme.settings(autoCraft.settings)).expandX();

            WContainer controls = list.add(theme.horizontalList()).widget();

            if (autoCrafts.size() > 1) {
                WContainer moveContainer = controls.add(theme.horizontalList()).expandX().widget();
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

            WButton copy = controls.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate auto craft.";
            copy.action = () -> {
                autoCrafts.add(autoCrafts.indexOf(autoCraft), autoCraft.copy());
                fillWidget(theme, list);
            };

            WMinus remove = controls.add(theme.minus()).widget();
            remove.tooltip = "Remove auto craft.";
            remove.action = () -> {
                autoCrafts.remove(autoCraft);
                fillWidget(theme, list);
            };
        }

        if (!autoCrafts.isEmpty()) list.add(theme.horizontalSeparator()).expandX();

        WContainer controls = list.add(theme.horizontalList()).expandX().widget();

        WButton add = controls.add(theme.button("New Auto Craft")).expandX().widget();
        add.action = () -> {
            BaseAutoCraft autoCraft = new BaseAutoCraft();
            autoCrafts.add(autoCraft);
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Auto Crafts")).expandX().widget();
        removeAll.action = () -> {
            autoCrafts.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.gameMode == null) return;
        if (!(mc.player.containerMenu instanceof AbstractCraftingMenu currentScreenHandler)) return;

        if (shouldCraft) {
            if (currentScreenHandler.slots.getFirst().hasItem()) {
                mc.gameMode.handleInventoryMouseClick(currentScreenHandler.containerId, 0, 1, actionType, mc.player);
                debugLogger.info("Crafting...");
            }
            shouldCraft = false;
        } else {
            for (BaseAutoCraft autoCraft : autoCrafts) {
                if (fixDesync.get()) mc.player.getInventory().tick();

                debugLogger.info(" ");
                debugLogger.info(autoCraft.outputItem.get() + " searching...");
                RecipeDisplayEntry foundRecipe = findRecipe(autoCraft);
                debugLogger.info(autoCraft.outputItem.get() + " found: " + foundRecipe);
                if (foundRecipe == null) continue;

                mc.gameMode.handlePlaceRecipe(currentScreenHandler.containerId, foundRecipe.id(), autoCraft.stackPerTick.get());
                actionType = autoCraft.dropOnCraft.get() ? ClickType.THROW : ClickType.QUICK_MOVE;
                debugLogger.info("Selecting recipe...");
                shouldCraft = true;
                return;
            }
        }
    }

    @Override
    public void onDeactivate() {
        shouldCraft = false;
    }

    @Nullable
    private RecipeDisplayEntry findRecipe(BaseAutoCraft baseAutoCraft) {
        List<Item> inputWhitelist = baseAutoCraft.inputWhitelist.get();
        List<Item> inputBlacklist = baseAutoCraft.inputBlacklist.get();

        ClientRecipeBookAccessor recipeBook = (ClientRecipeBookAccessor) mc.player.getRecipeBook();

        recipeLabel:
        for (Map.Entry<RecipeDisplayId, RecipeDisplayEntry> mapEntry : recipeBook.getRecipes().entrySet()) {
            RecipeDisplayEntry recipe = mapEntry.getValue();

            Optional<List<Ingredient>> craftingRequirements = recipe.craftingRequirements();
            if (craftingRequirements.isEmpty()) continue;

            if (!canDisplayRecipe((AbstractCraftingMenu) mc.player.containerMenu, recipe.display()))
                continue;
            debugLogger.info("Can display check passed");

            StackedItemContents customRecipeFinder = new StackedItemContents();
            mc.player.getInventory().fillStackedContents(customRecipeFinder);
            if (!customRecipeFinder.canCraft(craftingRequirements.get(), null)) continue;
            debugLogger.info("Craftable check passed");

            // Output check
            List<ItemStack> outputStacks = recipe.display().result().resolveForStacks(SlotDisplayContext.fromLevel(mc.level));
            if (!outputStacks.getFirst().is(baseAutoCraft.outputItem.get())) continue;
            debugLogger.info("Output check passed");

            // Check input for extra item
            if (baseAutoCraft.keepIngredientsForExtra.get()) {
                boolean isCraftable = ((RecipeFinderAccessor) customRecipeFinder)
                        .invokeIsCraftable(craftingRequirements.get(), 2, null);
                if (!isCraftable) continue;
            }
            debugLogger.info("Extra item check passed");

            // Blacklist
            for (Ingredient ingredient : craftingRequirements.get()) {
                HolderSet<Item> entries = ((IngredientAccessor) (Object) ingredient).getEntries();
                for (Holder<Item> entry : entries) {
                    if (inputBlacklist.contains(entry.value())) {
                        continue recipeLabel;
                    }
                }
            }
            debugLogger.info("Blacklist check passed");

            // Whitelist
            if (!inputWhitelist.isEmpty()) {
                List<Item> whitelistCopy = new ArrayList<>(inputWhitelist);

                for (Ingredient ingredient : craftingRequirements.get()) {
                    HolderSet<Item> entries = ((IngredientAccessor) (Object) ingredient).getEntries();

                    for (Holder<Item> entry : entries) {
                        Item item = entry.value();

                        if (whitelistCopy.contains(item)) {
                            whitelistCopy.remove(item);
                            if (baseAutoCraft.useFirstMatchingVariant.get()) {
                                break;
                            }
                        }
                    }
                }

                if (!whitelistCopy.isEmpty()) continue;
            }
            debugLogger.info("Whitelist check passed");

            return recipe;
        }

        return null;
    }
}
