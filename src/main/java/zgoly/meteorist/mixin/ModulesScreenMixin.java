package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.config.MeteoristConfigScreen;

import java.util.List;

@Mixin(ModulesScreen.class)
public class ModulesScreenMixin {
    @Inject(method = "createCategory", at = @At("HEAD"), remap = false, cancellable = true)
    private void createCategory(WContainer c, Category category, List<Module> moduleList, CallbackInfoReturnable<WWindow> cir) {
        if (category == Meteorist.DEV_CATEGORY && !MeteoristConfigScreen.showDevModules.get()) cir.cancel();
    }
}
