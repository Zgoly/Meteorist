package zgoly.meteorist.modules.rangeactions.rangeactions;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class DespawnerRangeAction extends BaseRangeAction {
    public static final String type = "Despawner";

    protected final SettingGroup sgDespawnerRangeAction = settings.createGroup("Despawner Range Action");

    public final Setting<Boolean> checkRoof = sgDespawnerRangeAction.add(new BoolSetting.Builder()
            .name("check-roof")
            .description("Check if the player is under a roof.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> upVelocity = sgDespawnerRangeAction.add(new DoubleSetting.Builder()
            .name("up-velocity")
            .description("Up velocity to apply to the player.")
            .defaultValue(8.0)
            .min(0.0)
            .build()
    );

    public DespawnerRangeAction() {
        super();
        Set<EntityType<?>> mutableEntities = new HashSet<>();
        mutableEntities.add(EntityType.PHANTOM);
        entities.set(mutableEntities);
        // Yeah
        // entities.set(Set.of(EntityType.PHANTOM));
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public DespawnerRangeAction copy() {
        return (DespawnerRangeAction) new DespawnerRangeAction().fromTag(toTag());
    }
}