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
    /**
     * Reloads the currently displayed config screen.
     * If fromPrompt is true and the current screen is a child of another screen, the parent screen is reloaded instead.
     * This is used when the user is prompted to save a config and chooses not to, in which case the parent screen should be reloaded.
     * @param fromPrompt Whether the reload is from a prompt (i.e. the user was prompted to save a config and chose not to).
     */
    public static void reload(boolean fromPrompt) {
        if (mc.currentScreen instanceof WidgetScreen screen) {
            if (fromPrompt) {
                if (screen.parent instanceof WidgetScreen screen1) screen1.reload();
            } else {
                screen.reload();
            }
        }
    }

    /**
     * Returns the file path of the directory associated with the given module.
     * The directory is determined based on the module's name after removing invalid characters.
     *
     * @param module The module for which to get the directory path.
     * @return A File object representing the path of the module's directory.
     */

    public static File getFolderPath(Module module) {
        return new File(Paths.get(FabricLoader.getInstance().getGameDir().toString(), MOD_ID, removeInvalidChars(module.name)).toString());
    }

    /**
     * Adds a config manager to the given list for the given module, allowing the user to save and load configurations of the module.
     *
     * @param theme The GuiTheme to use for the config manager.
     * @param list The WVerticalList to add the config manager to.
     * @param module The module for which to display the config manager.
     */
    public static void configManager(GuiTheme theme, WVerticalList list, Module module) {
        File folderPath = getFolderPath(module);

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

    /**
     * Converts a module into an NbtCompound, removing unnecessary tags that will be reset by Meteor on load.
     * @param module The module to convert.
     * @return An NbtCompound containing the module's settings.
     */
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

    /**
     * Restores a module's settings from an NbtCompound, ensuring certain properties are retained.
     * This method reads the given NbtCompound to set the module's state but preserves specific
     * properties such as keybind, toggleOnBindRelease, chat feedback, favorite status, and active
     * status to prevent unintended changes by the NbtCompound data.
     *
     * @param module The module whose settings are to be restored.
     * @param nbtCompound The NbtCompound containing the module's settings.
     */
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

    /**
     * Fills the given WTable with WLabels and WButtons for each .nbt file in the given folderPath.
     * Each WLabel displays the name of the corresponding .nbt file.
     * Each WButton has a name of "Save" and an action that saves the given module to the corresponding .nbt file.
     * Each WButton has a name of "Load" and an action that loads the given module from the corresponding .nbt file.
     * Each WMinus has an action that prompts the user to delete the corresponding .nbt file.
     * If the folderPath does not exist, this method does nothing.
     * @param theme The GuiTheme to use for the WTable.
     * @param module The module whose configurations should be saved/loaded.
     * @param folderPath The folder path to search for .nbt files.
     * @param configTable The WTable to fill with the WLabels and WButtons.
     */
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

    /**
     * Saves the given module to the given file.
     * If the file does not exist, this method will create it and save the module to it.
     * If the file does exist, this method will prompt the user to overwrite the file.
     * If an exception occurs while saving the module, this method will display an error toast.
     * @param module The module to save.
     * @param file The file to save the module to.
     */
    private static void save(Module module, File file) {
        save(module, file, false);
    }

    /**
     * Saves the given module to the given file.
     * If the file does not exist, this method will create it and save the module to it.
     * If the file does exist and overwrite is false, this method will prompt the user to overwrite the file.
     * If the file does exist and overwrite is true, this method will overwrite the file without prompting the user.
     * If an exception occurs while saving the module, this method will display an error toast.
     * @param module The module to save.
     * @param file The file to save the module to.
     * @param overwrite Whether to overwrite the file if it already exists.
     */
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
