package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import zgoly.meteorist.modules.placer.BasePlacer;

public class PlacerScreen extends WindowScreen {
    private final BasePlacer placer;
    private WContainer settingsContainer;

    public PlacerScreen(GuiTheme theme, BasePlacer placer) {
        super(theme, placer.name.get());

        this.placer = placer;
    }

    @Override
    public void initWidgets() {
        if (!placer.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(placer.settings)).expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();

        placer.settings.tick(settingsContainer, theme);
    }
}
