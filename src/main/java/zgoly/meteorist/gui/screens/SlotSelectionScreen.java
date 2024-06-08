package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import zgoly.meteorist.modules.slotclick.selections.BaseSlotSelection;

public class SlotSelectionScreen extends WindowScreen {
    private final BaseSlotSelection slotSelection;
    private WContainer settingsContainer;

    public SlotSelectionScreen(GuiTheme theme, BaseSlotSelection slotSelection) {
        super(theme, slotSelection.getTypeName());

        this.slotSelection = slotSelection;
    }

    @Override
    public void initWidgets() {
        if (!slotSelection.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(slotSelection.settings)).expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();

        slotSelection.settings.tick(settingsContainer, theme);
    }
}
