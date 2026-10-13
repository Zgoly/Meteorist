package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import zgoly.meteorist.modules.inventoryorganizer.SlotRule;

public class SlotRuleScreen extends WindowScreen {
    private final SlotRule slotRule;
    private WContainer settingsContainer;

    public SlotRuleScreen(GuiTheme theme, SlotRule slotRule) {
        super(theme, "Edit Slot Rule (Slot " + slotRule.targetSlot.get() + ")");
        this.slotRule = slotRule;
    }

    @Override
    public void initWidgets() {
        if (!slotRule.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(slotRule.settings)).expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();
        slotRule.settings.tick(settingsContainer, theme);
    }

    @Override
    public void onClosed() {
        super.onClosed();
        if (parent instanceof meteordevelopment.meteorclient.gui.WidgetScreen widgetScreen) {
            widgetScreen.reload();
        }
    }
}
