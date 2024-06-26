package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import zgoly.meteorist.modules.autotrade.offers.BaseOffer;

public class OfferScreen extends WindowScreen {
    private final BaseOffer autoTrade;
    private WContainer settingsContainer;

    public OfferScreen(GuiTheme theme, BaseOffer autoTrade) {
        super(theme, autoTrade.getTypeName());

        this.autoTrade = autoTrade;
    }

    @Override
    public void initWidgets() {
        if (!autoTrade.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(autoTrade.settings)).expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();

        autoTrade.settings.tick(settingsContainer, theme);
    }
}
