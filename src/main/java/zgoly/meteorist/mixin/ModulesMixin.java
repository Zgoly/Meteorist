package zgoly.meteorist.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.config.MeteoristConfigScreen;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mixin(Modules.class)
public abstract class ModulesMixin {
    @ModifyReturnValue(method = "loopCategories", at = @At("RETURN"))
    private static Iterable<Category> filterCategories(Iterable<Category> categories) {
        if (MeteoristConfigScreen.showDevModules.get()) return categories;

        return StreamSupport.stream(categories.spliterator(), false)
                .filter(category -> category != Meteorist.DEV_CATEGORY)
                .collect(Collectors.toList());
    }

    @ModifyReturnValue(method = "searchTitles", at = @At("RETURN"))
    private static List<Tuple<Module, String>> filterSearchTitles(List<Tuple<Module, String>> results) {
        if (MeteoristConfigScreen.showDevModules.get()) return results;

        return results.stream()
                .filter(result -> result.getA().category != Meteorist.DEV_CATEGORY)
                .collect(Collectors.toList());
    }

    @ModifyReturnValue(method = "searchSettingTitles", at = @At("RETURN"))
    private static Set<Module> filterSearchSettings(Set<Module> results) {
        if (MeteoristConfigScreen.showDevModules.get()) return results;

        return results.stream()
                .filter(module -> module.category != Meteorist.DEV_CATEGORY)
                .collect(Collectors.toSet());
    }
}