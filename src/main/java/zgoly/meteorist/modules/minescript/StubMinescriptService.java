package zgoly.meteorist.modules.minescript;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.Util;

public class StubMinescriptService implements MinescriptService {
    @Override
    public WWidget createWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        list.add(theme.label("Minescript is not installed!")).center().alignWidget();
        list.add(theme.label("This module requires the Minescript mod to function.")).center().alignWidget();

        WButton downloadBtn = list.add(theme.button("Minescript website")).center().widget();
        downloadBtn.action = () -> Util.getPlatform().openUri("https://minescript.net/");

        WButton downloadsBtn = list.add(theme.button("Go to Downloads")).center().widget();
        downloadsBtn.action = () -> Util.getPlatform().openUri("https://minescript.net/downloads/");

        return list;
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public void onTick() {
    }

    @Override
    public void refreshScripts() {
    }

    @Override
    public void refreshJobsIfNeeded() {
    }
}