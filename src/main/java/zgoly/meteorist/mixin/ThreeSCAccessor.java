package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MeteorGuiTheme.ThreeStateColorSetting.class, remap = false)
public interface ThreeSCAccessor {
    @Accessor("normal") Setting<SettingColor> normal();
}
