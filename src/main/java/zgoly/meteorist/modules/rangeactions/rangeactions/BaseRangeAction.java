package zgoly.meteorist.modules.rangeactions.rangeactions;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

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
    public final Setting<KillAura.EntityAge> mobAgeFilter = sgBaseRangeAction.add(new EnumSetting.Builder<KillAura.EntityAge>()
            .name("mob-age-filter")
            .description("Determines the age of the mobs to react (baby, adult, or both).")
            .defaultValue(KillAura.EntityAge.Adult)
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
    public final Setting<Boolean> ignoreCreative = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-creative")
            .description("Will avoid reacting to players in creative mode.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> ignoreFriends = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Will avoid reacting to players on your friends list.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> ignoreShield = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-shield")
            .description("Will avoid reacting to players who are using a shield.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> useFovRange = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("use-fov-range")
            .description("Restrict reactions to entities within the specified FOV.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Double> fovRange = sgBaseRangeAction.add(new DoubleSetting.Builder()
            .name("fov-range")
            .description("Maximum Field of View (FOV) range for reacting to entities.")
            .sliderRange(0, 180)
            .defaultValue(90)
            .visible(useFovRange::get)
            .build()
    );
    public final Setting<Boolean> ignoreWalls = sgBaseRangeAction.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allow reacting through walls.")
            .defaultValue(false)
            .build()
    );
    public final Setting<SortPriority> priority = sgBaseRangeAction.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("Sorting method to prioritize targets within range.")
            .defaultValue(SortPriority.ClosestAngle)
            .build()
    );

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public BaseRangeAction fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
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
