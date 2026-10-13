package zgoly.meteorist.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PresetsScreen extends WindowScreen {
    private final Module module;
    private final MeteoristConfigManager.PresetConfig[] presets;

    public PresetsScreen(GuiTheme theme, Module module, MeteoristConfigManager.PresetConfig[] presets) {
        super(theme, module.title + " Presets");
        this.module = module;
        this.presets = presets;
    }

    @Override
    public void initWidgets() {
        if (presets == null || presets.length == 0) return;

        WVerticalList list = add(theme.verticalList()).expandX().widget();
        WTable table = list.add(theme.table()).expandX().widget();

        for (MeteoristConfigManager.PresetConfig preset : presets) {
            table.add(theme.label(preset.name())).expandX();

            WButton load = table.add(theme.button("Load")).widget();
            load.action = () -> {
                Runnable applyPreset = () -> {
                    try {
                        preset.applyAction().accept(module);
                        mc.gui.toastManager().addToast(new SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.nullToEmpty("Preset successfully loaded"),
                                Component.nullToEmpty("Loaded preset \"" + preset.name() + "\"")
                        ));
                    } catch (Exception e) {
                        mc.gui.toastManager().addToast(new SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.nullToEmpty("Failed to load preset"),
                                Component.nullToEmpty(e.getMessage())
                        ));
                    }
                    onClose();
                };

                int currentEntries = MeteoristConfigManager.countEntries(MeteoristConfigManager.toTag(module));
                if (currentEntries > 0) {
                    meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt.create()
                            .title("Apply Preset")
                            .message("Are you sure you want to load preset \"" + preset.name() + "\"? This will overwrite " + currentEntries + " config entry(ies).")
                            .onYes(applyPreset)
                            .dontShowAgainCheckboxVisible(false)
                            .show();
                } else {
                    applyPreset.run();
                }
            };

            table.row();
        }
    }

    @Override
    public void onClosed() {
        super.onClosed();
        if (parent instanceof meteordevelopment.meteorclient.gui.WidgetScreen widgetScreen) {
            widgetScreen.reload();
        }
    }
}
