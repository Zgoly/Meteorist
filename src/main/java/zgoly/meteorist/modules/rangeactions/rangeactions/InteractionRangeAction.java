package zgoly.meteorist.modules.rangeactions.rangeactions;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class InteractionRangeAction extends BaseRangeAction {
    public static final String type = "Interaction";

    protected final SettingGroup sgInteractionRangeAction = settings.createGroup("Interaction Range Action");

    public final Setting<Boolean> ignoreStartBreakingBlock = sgInteractionRangeAction.add(new BoolSetting.Builder()
            .name("ignore-start-breaking-block")
            .description("Ignore actions when starting to break a block.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> ignoreInteractBlock = sgInteractionRangeAction.add(new BoolSetting.Builder()
            .name("ignore-interact-block")
            .description("Ignore actions when interacting with a block.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> ignoreAttackEntity = sgInteractionRangeAction.add(new BoolSetting.Builder()
            .name("ignore-attack-entity")
            .description("Ignore actions when attacking an entity.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> ignoreInteractEntity = sgInteractionRangeAction.add(new BoolSetting.Builder()
            .name("ignore-interact-entity")
            .description("Ignore actions when interacting with an entity.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> enableSneak = sgInteractionRangeAction.add(new BoolSetting.Builder()
            .name("enable-sneak")
            .description("Enable sneak action.")
            .defaultValue(false)
            .build()
    );

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public InteractionRangeAction copy() {
        return (InteractionRangeAction) new InteractionRangeAction().fromTag(toTag());
    }
}