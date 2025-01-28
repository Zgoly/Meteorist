package zgoly.meteorist.modules.rangeactions.rangeactions;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

import java.util.Set;

public class BaseRangeAction implements ISerializable<BaseRangeAction> {
    public final Settings settings = new Settings();

    protected final SettingGroup sgBaseRangeAction = settings.createGroup("Base Range Action");

    public final Setting<Double> rangeFrom = sgBaseRangeAction.add(new DoubleSetting.Builder()
            .name("range-from")
            .description("Minimum range to react.")
            .defaultValue(0.0)
            .min(0.0)
            .build()
    );

    public final Setting<Double> rangeTo = sgBaseRangeAction.add(new DoubleSetting.Builder()
            .name("range-to")
            .description("Maximum range to react.")
            .defaultValue(10.0)
            .min(0.0)
            .build()
    );

    public final Setting<Set<EntityType<?>>> entities = sgBaseRangeAction.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entity types to react on.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );

    public final Setting<Boolean> ignoreFriends = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignore players added as friends.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> ignoreBabies = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Ignore baby variants of mobs.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> ignoreNamed = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Ignore named mobs.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> ignorePassive = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Ignore passive mobs unless they are targeting you.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> ignoreTamed = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Ignore tamed mobs.")
            .defaultValue(false)
            .build()
    );

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public BaseRangeAction fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);
        return this;
    }

    public String getTypeName() {
        return null;
    }

    public BaseRangeAction copy() {
        return null;
    }
}
