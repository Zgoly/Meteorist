package zgoly.meteorist.gui.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screens.Screen;
import zgoly.meteorist.gui.screens.KeybindsScreen;

public class KeybindsTab extends Tab {
    public KeybindsTab() {
        super("Keybinds");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new KeybindsScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof KeybindsScreen;
    }
}
