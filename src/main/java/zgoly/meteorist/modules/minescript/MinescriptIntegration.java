package zgoly.meteorist.modules.minescript;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class MinescriptIntegration extends Module {

    private final MinescriptService service;

    public MinescriptIntegration() {
        super(Meteorist.CATEGORY, "minescript-integration", "Control your Minescript custom scripts.");
        this.service = MinescriptServiceFactory.create(this);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return service.createWidget(theme);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        service.onTick();
    }

    @Override
    public void onActivate() {
        service.onActivate();
    }

    @Override
    public void onDeactivate() {
        service.onDeactivate();
    }
}