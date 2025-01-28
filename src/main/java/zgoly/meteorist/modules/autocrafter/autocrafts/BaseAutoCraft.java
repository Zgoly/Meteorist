package zgoly.meteorist.modules.autocrafter.autocrafts;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.SlotActionType;
import zgoly.meteorist.gui.screens.AutoCraftScreen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseAutoCraft implements ISerializable<BaseAutoCraft> {
    public final Settings settings = new Settings();
    public final List<Setting<Item>> ingredients = new ArrayList<>();

    SettingGroup sgGeneral = settings.getDefaultGroup();
    SettingGroup sgClear = settings.createGroup("Clear");
    SettingGroup sgIngredients = settings.createGroup("Ingredients");
    SettingGroup sgOutput = settings.createGroup("Output");

    public final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables this auto craft.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Boolean> oneMore = sgGeneral.add(new BoolSetting.Builder()
            .name("one-more")
            .description("For each ingredient in the recipe, wait for one more ingredient.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Boolean> keepSingleStacks = sgGeneral.add(new BoolSetting.Builder()
            .name("keep-single-stacks")
            .description("When searching for ingredients in the inventory, ignore ingredient stacks that contain only one item.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<Boolean> clearSlots = sgClear.add(new BoolSetting.Builder()
            .name("clear-slots")
            .description("Clears crafting slots before crafting.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<SlotActionType> clearSlotsAction = sgClear.add(new EnumSetting.Builder<SlotActionType>()
            .name("clear-slots-action")
            .description("Action to perform when clearing slots.")
            .defaultValue(SlotActionType.THROW)
            .visible(clearSlots::get)
            .build()
    );
    public final Setting<Integer> clearSlotsButton = sgClear.add(new IntSetting.Builder()
            .name("clear-slots-button")
            .description("Button to use when clearing slots.")
            .defaultValue(1)
            .range(0, 1)
            .visible(clearSlots::get)
            .build()
    );

    public final Setting<SlotActionType> outputAction = sgOutput.add(new EnumSetting.Builder<SlotActionType>()
            .name("output-action")
            .description("Action to perform when taking the output item.")
            .defaultValue(SlotActionType.QUICK_MOVE)
            .build()
    );
    public final Setting<Integer> outputButton = sgOutput.add(new IntSetting.Builder()
            .name("output-button")
            .description("Button to use when taking the output item.")
            .defaultValue(1)
            .range(0, 1)
            .build()
    );
    public final Setting<Boolean> checkOutputItem = sgOutput.add(new BoolSetting.Builder()
            .name("check-output-item")
            .description("Whether to check the output item. Recommended when server TPS is low.")
            .defaultValue(false)
            .onChanged(value -> reloadParent())
            .build()
    );

    public static void reloadParent() {
        Screen screen = mc.currentScreen;
        if (screen instanceof AutoCraftScreen autoCraftScreen) {
            if (autoCraftScreen.parent instanceof WindowScreen windowScreen) {
                windowScreen.reload();
            }
        }

        // Also handle selecting ingredient
        if (screen instanceof ItemSettingScreen itemSettingScreen) {
            if (itemSettingScreen.parent instanceof AutoCraftScreen autoCraftScreen) {
                if (autoCraftScreen.parent instanceof WindowScreen windowScreen) {
                    windowScreen.reload();
                }
            }
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BaseAutoCraft fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public String getTypeName() {
        return null;
    }

    public BaseAutoCraft copy() {
        return null;
    }

    public Setting<Item> getOutputItem() {
        return null;
    }

    public int getActionsPerTick() {
        return 0;
    }
}
