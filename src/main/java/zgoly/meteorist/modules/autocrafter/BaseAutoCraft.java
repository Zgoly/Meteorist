package zgoly.meteorist.modules.autocrafter;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import zgoly.meteorist.settings.ItemsSetting;

import java.util.List;

public class BaseAutoCraft implements ISerializable<BaseAutoCraft> {
    Settings settings = new Settings();
    SettingGroup sgAutoCraft = settings.createGroup("Auto Craft");

    public final Setting<List<Item>> inputWhitelist = sgAutoCraft.add(new ItemsSetting.Builder()
            .name("input-whitelist")
            .description("Items allowed as craft ingredients.")
            .defaultValue(List.of(Items.GOLD_INGOT))
            .build()
    );

    public final Setting<Boolean> useFirstMatchingVariant = sgAutoCraft.add(new BoolSetting.Builder()
            .name("use-first-matching-variant")
            .description("If enabled, only the first matching variant per ingredient will be used for " + inputWhitelist.title + " filtering.")
            .defaultValue(true)
            .build()
    );

    public final Setting<List<Item>> inputBlacklist = sgAutoCraft.add(new ItemsSetting.Builder()
            .name("input-blacklist")
            .description("Items not allowed as craft ingredients.")
            .build()
    );

    public final Setting<Item> outputItem = sgAutoCraft.add(new ItemSetting.Builder()
            .name("output-item")
            .description("The item to be crafted.")
            .defaultValue(Items.GOLD_NUGGET)
            .build()
    );

    public final Setting<Boolean> stackPerTick = sgAutoCraft.add(new BoolSetting.Builder()
            .name("stack-per-tick")
            .description("Craft full stack per tick instead of one item per tick.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> keepIngredientsForExtra = sgAutoCraft.add(new BoolSetting.Builder()
            .name("keep-ingredients-for-extra")
            .description("Keep ingredients for one extra item. Will not work with " + stackPerTick.title + ".")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> dropOnCraft = sgAutoCraft.add(new BoolSetting.Builder()
            .name("drop-on-craft")
            .description("Drop the crafted item instead of placing it in inventory.")
            .defaultValue(false)
            .build()
    );

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public BaseAutoCraft fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public BaseAutoCraft copy() {
        return new BaseAutoCraft().fromTag(toTag());
    }
}
