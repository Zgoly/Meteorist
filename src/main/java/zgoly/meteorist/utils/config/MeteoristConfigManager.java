package zgoly.meteorist.utils.config;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static zgoly.meteorist.Meteorist.MOD_ID;
import static zgoly.meteorist.utils.MeteoristUtils.removeInvalidChars;

public class MeteoristConfigManager {
    public static void reload(boolean fromPrompt) {
        if (mc.currentScreen instanceof WidgetScreen screen) {
            if (fromPrompt) {
                if (screen.parent instanceof WidgetScreen screen1) screen1.reload();
            } else {
                screen.reload();
            }
        }
    }

    public static void configManager(GuiTheme theme, WVerticalList list, Module module) {
        File folderPath = new File(Paths.get(FabricLoader.getInstance().getGameDir().toString(), MOD_ID, removeInvalidChars(module.name)).toString());

        WSection configSection = list.add(theme.section("Config Manager")).expandX().widget();
        WTable control = configSection.add(theme.table()).expandX().widget();

        WTable configTable = control.add(theme.table()).expandX().widget();

        fillConfigTable(theme, module, folderPath, configTable);

        control.row();

        WHorizontalList buttons = control.add(theme.horizontalList()).expandX().widget();

        WButton reloadButton = buttons.add(theme.button("Reload")).expandX().widget();
        reloadButton.action = () -> {
            configTable.clear();
            fillConfigTable(theme, module, folderPath, configTable);
        };

        WButton openFolder = buttons.add(theme.button("Open Folder")).expandX().widget();
        openFolder.action = () -> {
            if (!folderPath.exists()) folderPath.mkdirs();
            Util.getOperatingSystem().open(folderPath);
        };

        control.row();

        control.add(theme.horizontalSeparator()).expandX().widget();

        control.row();

        WTextBox textBox = control.add(theme.textBox("", "Config Name")).expandX().widget();

        WButton save = control.add(theme.button("Save")).widget();
        save.action = () -> {
            if (!folderPath.exists()) folderPath.mkdirs();
            File file = new File(folderPath, textBox.get() + ".nbt");
            save(module, file);
        };
    }

    // We don't want to modify module class, so we remove all unnecessary tags
    public static NbtCompound toTag(Module module) {
        NbtCompound nbtCompound = module.toTag();
        nbtCompound.remove("name");
        nbtCompound.remove("keybind");
        nbtCompound.remove("toggleOnKeyRelease");
        nbtCompound.remove("chatFeedback");
        nbtCompound.remove("favorite");
        nbtCompound.remove("active");
        return nbtCompound;
    }

    // Meteor resets settings that are not included in compound (strange behaviour imo), so we prevent it
    public static void fromTag(Module module, NbtCompound nbtCompound) {
        Keybind keybind = module.keybind.copy();
        boolean toggleOnBindRelease = module.toggleOnBindRelease;
        boolean chatFeedback = module.chatFeedback;
        boolean favorite = module.favorite;
        boolean isActive = module.isActive();

        module.fromTag(nbtCompound);

        if (module.keybind != keybind) module.keybind.set(keybind);
        if (module.toggleOnBindRelease != toggleOnBindRelease) module.toggleOnBindRelease = toggleOnBindRelease;
        if (module.chatFeedback != chatFeedback) module.chatFeedback = chatFeedback;
        if (module.favorite != favorite) module.favorite = favorite;
        if (module.isActive() != isActive) module.toggle();
    }

    private static void fillConfigTable(GuiTheme theme, Module module, File folderPath, WTable configTable) {
        if (folderPath.exists()) {
            Arrays.stream(folderPath.listFiles()).filter(file -> file.getName().endsWith(".nbt")).forEach(file -> {
                configTable.add(theme.label(file.getName().replace(".nbt", ""))).expandX().widget();
                WButton save = configTable.add(theme.button("Save")).widget();
                save.action = () -> save(module, file);

                WButton load = configTable.add(theme.button("Load")).widget();
                load.action = () -> {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        NbtCompound nbtCompound = NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes());
                        fromTag(module, nbtCompound);
                        mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Config successfully loaded"), Text.of("Loaded as \"" + file.getName() + "\" with " + nbtCompound.getSize() + " entries.")));
                    } catch (FileNotFoundException e) {
                        mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Failed to load config"), Text.of("File not found. Did you delete/rename it?")));
                    } catch (Exception e) {
                        mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Failed to load config"), Text.of(e.getMessage())));
                    }
                    reload(false);
                };

                WMinus delete = configTable.add(theme.minus()).widget();
                delete.action = () -> YesNoPrompt.create().title("Delete Config").message("Are you sure you want to delete \"" + file.getName() + "\"? This cannot be undone.").onYes(() -> {
                    file.delete();
                    reload(true);
                }).dontShowAgainCheckboxVisible(false).show();

                configTable.row();
            });
        }
    }

    private static void save(Module module, File file) {
        save(module, file, false);
    }

    private static void save(Module module, File file, boolean overwrite) {
        if (!file.exists() || overwrite) {
            try {
                NbtCompound nbtCompound = toTag(module);
                FileOutputStream outputStream = new FileOutputStream(file);
                NbtIo.writeCompressed(nbtCompound, outputStream);
                outputStream.close();
                mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Config successfully saved"), Text.of("Saved as \"" + file.getName() + "\" with " + nbtCompound.getSize() + " entries.")));
            } catch (Exception e) {
                mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Failed to save config"), Text.of(e.getMessage())));
            }
            reload(false);
        } else {
            YesNoPrompt.create().title("Overwrite Config").message("Are you sure you want to overwrite \"" + file.getName() + "\"? This cannot be undone.").onYes(() -> save(module, file, true)).dontShowAgainCheckboxVisible(false).show();
        }
    }
}
