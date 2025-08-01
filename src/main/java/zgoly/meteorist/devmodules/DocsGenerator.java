package zgoly.meteorist.devmodules;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.sound.SoundEvent;
import org.apache.commons.lang3.StringUtils;
import zgoly.meteorist.Meteorist;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO: delete after full transition to MeteoristInfoParser
public class DocsGenerator extends Module {
    private final SettingGroup sgWiki = settings.createGroup("Wiki");
    private final SettingGroup sgSource = settings.createGroup("Source");

    public final Setting<String> modulesPageLink = sgWiki.add(new StringSetting.Builder()
            .name("modules-page-link")
            .description("Link to the modules page on the GitHub wiki. Keep empty to disable.")
            .defaultValue("../../wiki/modules")
            .build()
    );
    public final Setting<String> commandsPageLink = sgWiki.add(new StringSetting.Builder()
            .name("commands-page-link")
            .description("Link to the commands page on the GitHub wiki. Keep empty to disable.")
            .defaultValue("../../wiki/commands")
            .build()
    );
    public final Setting<String> presetsPageLink = sgWiki.add(new StringSetting.Builder()
            .name("presets-page-link")
            .description("Link to the presets page on the GitHub wiki. Keep empty to disable.")
            .defaultValue("../../wiki/presets")
            .build()
    );
    private final Setting<String> sourceLink = sgSource.add(new StringSetting.Builder()
            .name("source-link")
            .description("Link to the java source directory. Keep empty to disable.")
            .defaultValue("../blob/main/src/main/java")
            .build()
    );
    private final Setting<String> sourceFileExtension = sgSource.add(new StringSetting.Builder()
            .name("source-file-extension")
            .description("File extension of the java source files.")
            .defaultValue(".java")
            .build()
    );

    public List<PackageInfo> packages = new ArrayList<>();

    public DocsGenerator() {
        super(Meteorist.DEV_CATEGORY, "generator", "Generate documentation for Meteor and its add-ons.");
    }

    public static class PackageInfo {
        public String name;
        public String title;
        public List<CategoryWrapper> categoryWrappers = new ArrayList<>();
        public List<Command> commands = new ArrayList<>();
        public List<HudElementInfoWrapper> hudElementInfoWrappers = new ArrayList<>();
        public boolean enabled = true;

        public PackageInfo(String name) {
            this.name = name;
        }

        public static class CategoryWrapper {
            public Category category;
            public List<Module> modules;
        }

        public static class HudElementInfoWrapper {
            public HudElementInfo<?> hudElementInfo;
            public List<TextHudWrapper> textHudWrappers;

            public static class TextHudWrapper {
                public HudElementInfo<?>.Preset preset;
                public TextHud textHud;
            }
        }
    }

    public void fillPackages() {
        Modules.get().getAll().forEach(module -> {
            String packageName = getPackageName(module.getClass());
            PackageInfo packageInfo = getPackageInfo(packageName);

            PackageInfo.CategoryWrapper existingCategory = packageInfo.categoryWrappers.stream()
                    .filter(wrapper -> wrapper.category.equals(module.category))
                    .findFirst()
                    .orElse(null);

            if (existingCategory != null) {
                existingCategory.modules.add(module);
            } else {
                PackageInfo.CategoryWrapper categoryWrapper = new PackageInfo.CategoryWrapper();
                categoryWrapper.category = module.category;
                categoryWrapper.modules = new ArrayList<>(List.of(module));
                packageInfo.categoryWrappers.add(categoryWrapper);
            }
        });

        Commands.COMMANDS.forEach(command -> {
            String packageName = getPackageName(command.getClass());
            PackageInfo packageInfo = getPackageInfo(packageName);
            packageInfo.commands.add(command);
        });

        Hud.get().infos.forEach((name, info) -> info.presets.forEach(preset -> {
            HudElement element = preset.info.create();
            @SuppressWarnings("unchecked")
            Consumer<HudElement> consumer = (Consumer<HudElement>) preset.callback;
            consumer.accept(element);
            if (element instanceof TextHud textHud) {
                String packageName = getPackageName(preset.callback.getClass());
                PackageInfo packageInfo = getPackageInfo(packageName);

                PackageInfo.HudElementInfoWrapper existingHudElementInfo = packageInfo.hudElementInfoWrappers.stream()
                        .filter(wrapper -> wrapper.hudElementInfo.equals(info))
                        .findFirst()
                        .orElse(null);

                if (existingHudElementInfo != null) {
                    PackageInfo.HudElementInfoWrapper.TextHudWrapper textHudWrapper = new PackageInfo.HudElementInfoWrapper.TextHudWrapper();
                    textHudWrapper.preset = preset;
                    textHudWrapper.textHud = textHud;
                    existingHudElementInfo.textHudWrappers.add(textHudWrapper);
                } else {
                    PackageInfo.HudElementInfoWrapper hudElementInfoWrapper = new PackageInfo.HudElementInfoWrapper();
                    hudElementInfoWrapper.hudElementInfo = info;
                    PackageInfo.HudElementInfoWrapper.TextHudWrapper textHudWrapper = new PackageInfo.HudElementInfoWrapper.TextHudWrapper();
                    textHudWrapper.preset = preset;
                    textHudWrapper.textHud = textHud;
                    hudElementInfoWrapper.textHudWrappers = new ArrayList<>(List.of(textHudWrapper));
                    packageInfo.hudElementInfoWrappers.add(hudElementInfoWrapper);
                }
            }
        }));
    }

    public PackageInfo getPackageInfo(String packageName) {
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.name.equals(packageName)) {
                return packageInfo;
            }
        }
        PackageInfo newPackageInfo = new PackageInfo(packageName);
        newPackageInfo.title = getNameFromPackageName(packageName);
        packages.add(newPackageInfo);
        return newPackageInfo;
    }

    public static String getPackageName(Class<?> clazz) {
        String[] split = clazz.getPackage().getName().split("\\.");
        return split[0] + "." + split[1];
    }

    public static String getNameFromPackageName(String packageName) {
        List<String> parts = List.of(packageName.split("\\."));
        return Utils.nameToTitle(parts.getLast());
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (packages.isEmpty()) fillPackages();

        WVerticalList list = theme.verticalList();
        WTable table = theme.table();
        packages.forEach(packageInfo -> {
            WLabel label = table.add(theme.label(packageInfo.title)).expandX().widget();
            label.tooltip = packageInfo.name;
            WCheckbox checkbox = table.add(theme.checkbox(packageInfo.enabled)).widget();
            checkbox.action = () -> packageInfo.enabled = checkbox.checked;
            table.row();
        });
        list.add(table);

        WHorizontalList actions = theme.horizontalList();
        WButton copyReadme = actions.add(theme.button("Copy Readme")).expandX().widget();
        WButton copyWiki = actions.add(theme.button("Copy Wiki")).expandX().widget();
        list.add(actions).expandX();

        copyReadme.action = () -> {
            String readme = generateReadme();
            mc.keyboard.setClipboard(readme);
        };

        copyWiki.action = () -> {
            String wiki = generateWiki();
            mc.keyboard.setClipboard(wiki);
        };

        return list;
    }

    public String generateReadme() {
        StringBuilder infosBuilder = new StringBuilder();
        packages.forEach(packageInfo -> {
            if (packageInfo.enabled) {
                StringBuilder infoBuilder = new StringBuilder();
                // Modules
                if (!packageInfo.categoryWrappers.isEmpty()) {
                    StringBuilder modulesText = new StringBuilder();
                    modulesText.append("## Modules").append("\n");
                    packageInfo.categoryWrappers.forEach(categoryWrapper -> {
                        if (!categoryWrapper.modules.isEmpty()) {
                            modulesText.append("### ").append(categoryWrapper.category.name).append("\n");
                            modulesText.append("| № | Module | Description |").append("\n");
                            modulesText.append("|---|---|---|").append("\n");
                            int i = 0;
                            for (Module module : categoryWrapper.modules) {
                                i++;
                                String moduleName = module.title;
                                if (!modulesPageLink.get().isEmpty()) {
                                    moduleName = "[" + moduleName + "]" + "(" + modulesPageLink.get() + "#" + module.name + ")";
                                }
                                modulesText.append("| ")
                                        .append(i)
                                        .append(" | ")
                                        .append(moduleName)
                                        .append(" | ")
                                        .append(module.description)
                                        .append(" |")
                                        .append("\n");
                            }
                            modulesText.append("\n");
                        }
                    });
                    infoBuilder.append(modulesText);
                }

                // Commands
                if (!packageInfo.commands.isEmpty()) {
                    StringBuilder commandsText = new StringBuilder();
                    commandsText.append("## Commands").append("\n");
                    commandsText.append("| № | Command | Description |").append("\n");
                    commandsText.append("|---|---|---|").append("\n");
                    int i = 0;
                    for (Command command : packageInfo.commands) {
                        i++;
                        String commandName = "`" + command.getName() + "`";
                        if (!commandsPageLink.get().isEmpty()) {
                            commandName = "[" + commandName + "]" + "(" + commandsPageLink.get() + "#" + command.getName() + ")";
                        }
                        commandsText.append("| ")
                                .append(i)
                                .append(" | ")
                                .append(commandName)
                                .append(" | ")
                                .append(command.getDescription())
                                .append(" |")
                                .append("\n");
                    }
                    commandsText.append("\n");
                    infoBuilder.append(commandsText);
                }

                // Presets
                if (!packageInfo.hudElementInfoWrappers.isEmpty()) {
                    StringBuilder presetsText = new StringBuilder();
                    presetsText.append("## Presets").append("\n");
                    for (PackageInfo.HudElementInfoWrapper hudElementInfoWrapper : packageInfo.hudElementInfoWrappers) {
                        if (!hudElementInfoWrapper.textHudWrappers.isEmpty()) {
                            presetsText.append("### ").append(hudElementInfoWrapper.hudElementInfo.title).append("\n");
                            presetsText.append(hudElementInfoWrapper.hudElementInfo.description).append("\n");
                            presetsText.append("| № | Title | Text |").append("\n");
                            presetsText.append("|---|---|---|").append("\n");
                            int i = 0;
                            for (PackageInfo.HudElementInfoWrapper.TextHudWrapper textHudWrapper : hudElementInfoWrapper.textHudWrappers) {
                                i++;
                                String presetName = textHudWrapper.preset.title;

                                if (!presetsPageLink.get().isEmpty()) {
                                    presetName = "[" + presetName + "]" + "(" + presetsPageLink.get() + "#" + Utils.titleToName(textHudWrapper.preset.title) + ")";
                                }
                                presetsText.append("| ")
                                        .append(i)
                                        .append(" | ")
                                        .append(presetName)
                                        .append(" | ")
                                        .append("`")
                                        .append(textHudWrapper.textHud.text)
                                        .append("`")
                                        .append(" |")
                                        .append("\n");
                            }
                            presetsText.append("\n");
                        }
                    }
                    infoBuilder.append(presetsText);
                }

                infosBuilder.append("# ").append(packageInfo.title).append("\n").append(infoBuilder);
            }
        });
        return infosBuilder.toString();
    }

    public String getClassLink(Class<?> clazz) {
        String filePath = clazz.getName();
        if (filePath.contains("$$Lambda")) filePath = filePath.substring(0, filePath.indexOf("$$Lambda"));
        return sourceLink.get() + "/" + filePath.replace(".", "/") + sourceFileExtension.get();
    }

    public static String toTitleCase(String str) {
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), " "));
    }

    public String generateWiki() {
        StringBuilder infos = new StringBuilder();
        List<PackageInfo> filteredPackages = packages.stream().filter(packageInfo -> packageInfo.enabled).toList();
        filteredPackages.forEach(packageInfo -> {
            infos.append("# ").append(packageInfo.title).append("\n");

            boolean hasModules = !packageInfo.categoryWrappers.isEmpty();
            boolean hasCommands = !packageInfo.commands.isEmpty();
            boolean hasPresets = !packageInfo.hudElementInfoWrappers.isEmpty();

            // Modules
            if (hasModules) {
                infos.append("# Modules\n");

                packageInfo.categoryWrappers.forEach(categoryWrapper -> {
                    if (!categoryWrapper.modules.isEmpty()) {
                        // Category name
                        infos.append("## ").append(categoryWrapper.category.name).append("\n");
                        categoryWrapper.modules.forEach(module -> {
                            // Module name
                            infos.append("### ").append(module.title).append("\n");

                            // Source code link
                            if (!sourceLink.get().isEmpty()) {
                                infos.append("[[📖 Source code]](").append(getClassLink(module.getClass())).append(")").append("\n\n");
                            }

                            // Description
                            infos.append(module.description).append("\n");

                            // Additional info
                            if (module.getWidget(new MeteorGuiTheme()) != null) {
                                infos.append("\n").append("> [!NOTE]").append("\n").append("> This module has additional functionality that cannot be displayed on this page.").append("\n");
                            }

                            // Settings
                            if (!module.settings.groups.isEmpty()) {
                                module.settings.forEach(settingGroup -> settingGroup.forEach(setting -> {
                                    infos.append("\n");
                                    infos.append("+ #### ").append(setting.title).append("\n");
                                    infos.append("  ").append(setting.description).append("\n");
                                    infos.append("  + Type: `").append(setting.getClass().getSimpleName()).append("`").append("\n");
                                    infos.append("  + **Default**: ").append(getValueName(setting.getDefaultValue())).append("\n");

                                    Class<?> clazz = setting.getClass();
                                    Field[] fields = clazz.getDeclaredFields();


                                    for (Field field : fields) {
                                        field.setAccessible(true);
                                        try {
                                            Object value = getValueName(field.get(setting));
                                            if (value != null)
                                                infos.append("  + ").append(toTitleCase(field.getName())).append(": ").append(value).append("\n");
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }));
                            }

                            if (!infos.isEmpty() && categoryWrapper.modules.getLast() != module)
                                infos.append("\n---\n\n");
                        });
                    }
                });

                // Add delimiter after Modules if it's not the last section
                if (hasCommands && hasPresets) infos.append("\n---\n\n");
            }

            // Commands
            if (hasCommands) {
                infos.append("# Commands\n");

                packageInfo.commands.forEach(command -> {
                    // Command name
                    infos.append("## `").append(command.getName()).append("`").append("\n");

                    // Source code link
                    if (!sourceLink.get().isEmpty()) {
                        infos.append("[[📖 Source code]](").append(getClassLink(command.getClass())).append(")").append("\n\n");
                    }

                    // Description
                    infos.append(command.getDescription()).append("\n");

                    // Aliases
                    if (!command.getAliases().isEmpty()) {
                        infos.append("+ Aliases: `").append(String.join("`, `", command.getAliases())).append("`").append("\n");
                    }

                    if (!infos.isEmpty() && packageInfo.commands.getLast() != command) infos.append("\n---\n\n");
                });

                // Add delimiter after Commands if it's not the last section
                if (hasPresets) infos.append("\n---\n\n");
            }

            // Presets
            if (hasPresets) {
                infos.append("# Presets\n");

                for (PackageInfo.HudElementInfoWrapper hudElementInfoWrapper : packageInfo.hudElementInfoWrappers) {
                    if (!hudElementInfoWrapper.textHudWrappers.isEmpty()) {
                        // Hud element name
                        infos.append("## ").append(hudElementInfoWrapper.hudElementInfo.title).append("\n");

                        // Hud element description
                        infos.append(hudElementInfoWrapper.hudElementInfo.description).append("\n");

                        for (PackageInfo.HudElementInfoWrapper.TextHudWrapper textHudWrapper : hudElementInfoWrapper.textHudWrappers) {
                            infos.append("\n").append("+ ### ").append(textHudWrapper.preset.title).append("\n");

                            // Source code link
                            if (!sourceLink.get().isEmpty()) {
                                infos.append("  [[📖 Source code]](").append(getClassLink(textHudWrapper.preset.callback.getClass())).append(")").append("\n");
                            }

                            // Settings
                            Class<?> clazz = textHudWrapper.textHud.getClass();
                            Field[] fields = clazz.getDeclaredFields();
                            for (Field field : fields) {
                                field.setAccessible(true);
                                try {
                                    Object value = getValueName(field.get(textHudWrapper.textHud));
                                    if (value != null)
                                        infos.append("  + ").append(toTitleCase(field.getName())).append(": ").append(value).append("\n");
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    // Add delimiter after the last section if it's not the last package
                    if (!infos.isEmpty() && packageInfo.hudElementInfoWrappers.getLast() != hudElementInfoWrapper)
                        infos.append("\n---\n\n");
                }
            }

            // Add delimiter after the last section if it's not the last package
            if (!infos.isEmpty() && filteredPackages.getLast() != packageInfo) infos.append("\n---\n\n");
        });
        return infos.toString();
    }

    public String getValueName(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof List<?> list) {
            return list.stream().map(this::getValueName).collect(Collectors.joining(", "));
        } else if (value.getClass().isArray()) {
            return Arrays.stream((Object[]) value).map(this::getValueName).collect(Collectors.joining(", "));
        } else if (value.getClass().getName().contains("$$Lambda")) {
            return "`Lambda expression (cannot be displayed)`";
        } else if (value instanceof SoundEvent soundEvent) {
            return "`" + soundEvent.id().toTranslationKey() + "`";
        } else {
            String stringValue = String.valueOf(value);
            // Check if value has meaningful string representation
            if (stringValue.equals(value.getClass().getName() + "@" + Integer.toHexString(value.hashCode()))) {
                return null;
            } else {
                if (stringValue.isEmpty()) return null;
                return "`" + stringValue + "`";
            }
        }
    }
}
