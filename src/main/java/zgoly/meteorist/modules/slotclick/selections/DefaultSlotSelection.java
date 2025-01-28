package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.settings.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Pair;
import zgoly.meteorist.settings.StringPairSetting;

import java.util.Arrays;
import java.util.List;

public class DefaultSlotSelection extends BaseSlotSelection {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> checkContainerType = sgGeneral.add(new BoolSetting.Builder()
            .name("check-container-type")
            .description("Checks the container type.")
            .defaultValue(false)
            .build()
    );
    public final Setting<List<ScreenHandlerType<?>>> containerType = sgGeneral.add(new ScreenHandlerListSetting.Builder()
            .name("container-type")
            .description("Determines the type of containers that can be interacted with.")
            .defaultValue(Arrays.asList(ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6))
            .visible(checkContainerType::get)
            .build()
    );
    public final Setting<ContainerTypeMode> containerTypeMode = sgGeneral.add(new EnumSetting.Builder<ContainerTypeMode>()
            .name("container-type-mode")
            .description("Determines how the container types are handled.")
            .defaultValue(ContainerTypeMode.Whitelist)
            .visible(checkContainerType::get)
            .build()
    );
    public final Setting<Boolean> ignoreMenuTypeMismatch = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-menu-type-mismatch")
            .description("Ignores menu type mismatches.")
            .defaultValue(true)
            .visible(checkContainerType::get)
            .build()
    );
    public final Setting<Boolean> checkScreenName = sgGeneral.add(new BoolSetting.Builder()
            .name("check-screen-name")
            .description("Checks the screen name.")
            .defaultValue(false)
            .build()
    );
    public final Setting<String> screenName = sgGeneral.add(new StringSetting.Builder()
            .name("screen-name")
            .description("Determines the name of the screen. Uses Regular Expressions (RegEx).")
            .visible(checkScreenName::get)
            .build()
    );
    public final Setting<Boolean> checkSlotItemData = sgGeneral.add(new BoolSetting.Builder()
            .name("check-slot-item")
            .description("Checks the slot item.")
            .defaultValue(false)
            .build()
    );
    public final Setting<List<Pair<String, String>>> slotItemData = sgGeneral.add(new StringPairSetting.Builder()
            .name("slot-item-data")
            .description("Checks data of the slot item.")
            .placeholder(new Pair<>("Path", "RegEx"))
            .defaultValue(List.of(new Pair<>("", "")))
            .visible(checkSlotItemData::get)
            .build()
    );
    public final Setting<SlotItemMatchMode> slotItemMatchMode = sgGeneral.add(new EnumSetting.Builder<SlotItemMatchMode>()
            .name("slot-item-match-mode")
            .description("Determines how the slot item data should be matched.")
            .defaultValue(SlotItemMatchMode.All)
            .visible(checkSlotItemData::get)
            .build()
    );

    public DefaultSlotSelection() {
    }

    public enum SlotItemMatchMode {
        All,
        Any
    }

    public enum ContainerTypeMode {
        Whitelist,
        Blacklist
    }
}
