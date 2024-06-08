package zgoly.meteorist.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.util.math.MathHelper;

import static zgoly.meteorist.Meteorist.EYE;

public class WVisibilityCheckbox extends WCheckbox implements MeteorWidget {
    public boolean checked;
    private double animProgress;

    public WVisibilityCheckbox(boolean checked) {
        super(checked);
        this.checked = checked;
        animProgress = checked ? 0 : 1;
    }

    @Override
    protected void onPressed(int button) {
        checked = !checked;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double pad = pad();
        double ts = theme.textHeight();

        renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, EYE, theme.textColor());

        animProgress += (checked ? -1 : 1) * delta * 14;
        animProgress = MathHelper.clamp(animProgress, 0, 1);

        renderBackground(renderer, this, pressed, mouseOver);

        if (animProgress > 0) renderer.rotatedQuad((x + width / 2) - (animProgress * width / 2),
                y + height / 2 - theme.scale(1) / 2,
                width * animProgress,
                theme.scale(1),
                45,
                GuiRenderer.CIRCLE,
                theme.textColor()
        );
    }
}
