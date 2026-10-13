package zgoly.meteorist.utils;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// Helpers for collecting and describing module keybinds.
/// Used by the keybind overview screen, the keyboard widget and the keybinds HUD element.
public class KeybindUtils {
    /// A single occupied keybind, from either source.
    /// [color] is null for Minecraft binds (the caller picks a fallback).
    public record Bind(String title, Color color, boolean meteor, Module module, KeyMapping keyMapping) {
        public String keyName() {
            if (meteor) return module.keybind.toString();
            return ((KeyMappingAccessor) keyMapping).meteor$getKey().equals(InputConstants.UNKNOWN) ? "Unbound" : keyMapping.getTranslatedKeyMessage().getString();
        }
    }

    /// All occupied keybinds from the enabled sources, sorted by title.
    public static List<Bind> getBinds(boolean includeMeteor, boolean includeMc) {
        return getBinds(includeMeteor, includeMc, false);
    }

    public static List<Bind> getBinds(boolean includeMeteor, boolean includeMc, boolean includeUnboundMc) {
        List<Bind> binds = new ArrayList<>();

        if (includeMeteor) {
            for (Module module : Modules.get().getAll()) {
                if (module.keybind.isSet()) binds.add(new Bind(module.title, module.color, true, module, null));
            }
        }

        if (includeMc) {
            Minecraft mc = Minecraft.getInstance();
            for (KeyMapping keyMapping : mc.options.keyMappings) {
                InputConstants.Key key = ((KeyMappingAccessor) keyMapping).meteor$getKey();
                if (!includeUnboundMc && key.equals(InputConstants.UNKNOWN)) continue;

                binds.add(new Bind(Component.translatable(keyMapping.getName()).getString(), null, false, null, keyMapping));
            }
        }

        binds.sort(Comparator.comparing(bind -> bind.title().toLowerCase(Locale.ROOT)));
        return binds;
    }

    public static InputConstants.Key keyOf(Bind bind) {
        return bind.meteor() ? bind.module().keybind.key : ((KeyMappingAccessor) bind.keyMapping()).meteor$getKey();
    }

    public static void setMcKey(Bind bind, InputConstants.Key key) {
        if (bind.meteor()) throw new IllegalArgumentException("Meteor binds go through Modules.setModuleToBind");

        bind.keyMapping().setKey(key);
        Minecraft.getInstance().options.save();
    }

    public static void unbindMc(Bind bind) {
        setMcKey(bind, InputConstants.UNKNOWN);
    }

    /// Binds grouped by the physical key (keyboard key or mouse button) they are on.
    public static Map<InputConstants.Key, List<Bind>> getBoundKeys(boolean includeMeteor, boolean includeMc) {
        Map<InputConstants.Key, List<Bind>> map = new LinkedHashMap<>();

        for (Bind bind : getBinds(includeMeteor, includeMc)) {
            InputConstants.Key key = bind.meteor() ? bind.module().keybind.key : ((KeyMappingAccessor) bind.keyMapping()).meteor$getKey();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(bind);
        }

        return map;
    }

    /// Human-readable bind description, e.g. "Ctrl + C" or "Button 4".
    public static String describe(Keybind keybind) {
        return keybind.toString();
    }
}
