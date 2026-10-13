package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorWindow;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.config.MeteoristGui;

@Mixin(value = WMeteorWindow.class, remap = false)
public abstract class WMeteorWindowMixin implements MeteorWidget {
    @Unique private static boolean meteorist$lastActive;
    @Unique private static SettingColor meteorist$prevAccent;
    @Unique private static SettingColor meteorist$prevBackground;
    @Unique private static SettingColor meteorist$prevModuleBackground;
    @Unique private boolean meteorist$active;

    @Inject(method = "onRender", at = @At("HEAD"))
    private void onRenderHead(GuiRenderer renderer, double mouseX, double mouseY, double delta, CallbackInfo ci) {
        var theme = MeteoristGui.theme;

        meteorist$active = MeteoristGui.imNothingLikeYall.get()
                && Meteorist.CATEGORY.name.equals(((WMeteorWindowAccessor) this).meteorist$getTitle());

        if (meteorist$active) {
            if (!meteorist$lastActive) {
                meteorist$prevAccent = new SettingColor(theme.accentColor.get());
                meteorist$prevBackground = new SettingColor(((ThreeSCAccessor) theme.backgroundColor).normal().get());
                meteorist$prevModuleBackground = new SettingColor(theme.moduleBackground.get());
            }

            theme.accentColor.set(new SettingColor(MeteoristGui.accent.get()));
            ((ThreeSCAccessor) theme.backgroundColor).normal().set(new SettingColor(MeteoristGui.background.get()));
            theme.moduleBackground.set(new SettingColor(MeteoristGui.moduleBackground.get()));
        } else if (meteorist$lastActive && meteorist$prevAccent != null) {
            theme.accentColor.set(meteorist$prevAccent);
            ((ThreeSCAccessor) theme.backgroundColor).normal().set(meteorist$prevBackground);
            theme.moduleBackground.set(meteorist$prevModuleBackground);
        }

        meteorist$lastActive = meteorist$active;
    }
}
