package zgoly.meteorist.modules.inventoryorganizer;

import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.meteordev.starscript.Script;
import zgoly.meteorist.settings.ItemsSetting;
import zgoly.meteorist.utils.misc.Filterist;

import java.util.List;

public class SlotRule implements ISerializable<SlotRule> {
    public enum Mode {
        Simple,
        Advanced
    }

    public SlotRule() {
        this(Mode.Simple);
    }

    public SlotRule(Mode defaultMode) {
        mode.set(defaultMode);
    }

    public final Settings settings = new Settings();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the rule.")
            .build()
    );
    public final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
            .name("target-slot")
            .description("Inventory slot index to place items into (0-8: Hotbar, 9-35: Main Inventory, 36-39: Armor, 40: Offhand).")
            .defaultValue(0)
            .sliderRange(0, 40)
            .range(0, 40)
            .build()
    );
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Simple: list of items. Advanced: Starscript expression.")
            .defaultValue(Mode.Simple)
            .build()
    );

    // Simple Mode settings
    public final Setting<List<Item>> items = sgGeneral.add(new ItemsSetting.Builder()
            .name("items")
            .description("Priority list of items for this slot.")
            .defaultValue(List.of(Items.NETHERITE_SWORD, Items.DIAMOND_SWORD))
            .visible(() -> mode.get() == Mode.Simple)
            .build()
    );

    // Advanced Mode settings
    public final Setting<String> expression = sgGeneral.add(new StringSetting.Builder()
            .name("expression")
            .description("Starscript expression per item; wrap it in {braces} and make it produce true/false.")
            .renderer(StarscriptTextBoxRenderer.class)
            .visible(() -> mode.get() == Mode.Advanced)
            .build()
    );

    private Script compiledExpression;
    private String compiledExpressionSource;
    private boolean compiledExpressionFailed;

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (mode.get() == Mode.Simple) {
            List<Item> itemList = items.get();
            if (itemList.isEmpty()) return false;
            return itemList.contains(stack.getItem());
        }

        // Advanced Mode
        Script script = compiledScript();
        if (script == null) return false;
        return Filterist.evaluate(script, stack);
    }

    private Script compiledScript() {
        String source = expression.get();
        if (source == null || source.trim().isEmpty()) return null;

        if (!source.equals(compiledExpressionSource)) {
            compiledExpressionSource = source;
            compiledExpression = MeteorStarscript.compile(source);
            compiledExpressionFailed = compiledExpression == null;
        }

        return compiledExpressionFailed ? null : compiledExpression;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public SlotRule fromTag(CompoundTag tag) {
        if (tag.get("settings") instanceof CompoundTag settingsTag) settings.fromTag(settingsTag);
        return this;
    }

    public SlotRule copy() {
        return new SlotRule().fromTag(toTag());
    }
}
