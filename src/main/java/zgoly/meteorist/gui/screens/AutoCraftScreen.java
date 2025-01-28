package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import zgoly.meteorist.modules.autocrafter.autocrafts.BaseAutoCraft;

public class AutoCraftScreen extends WindowScreen {
    private final BaseAutoCraft autoCraft;
    private WContainer settingsContainer;

    public AutoCraftScreen(GuiTheme theme, BaseAutoCraft autoCraft) {
        super(theme, autoCraft.getTypeName());

        this.autoCraft = autoCraft;
    }

    @Override
    public void initWidgets() {
        if (!autoCraft.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(autoCraft.settings)).expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();

        autoCraft.settings.tick(settingsContainer, theme);
    }
}
