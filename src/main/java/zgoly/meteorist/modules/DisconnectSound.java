package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.events.DisconnectedScreenEvent;

import java.util.List;

public class DisconnectSound extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<SoundEvent>> sound = sgGeneral.add(new SoundEventListSetting.Builder()
            .name("sound")
            .description("Sound to play.")
            .defaultValue(List.of(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()))
            .build()
    );
    private final Setting<Double> soundPitch = sgGeneral.add(new DoubleSetting.Builder()
            .name("sound-pitch")
            .description("Pitch of the sound.")
            .defaultValue(0.5)
            .min(0)
            .sliderRange(0.5, 2)
            .build()
    );
    private final Setting<Double> soundVolume = sgGeneral.add(new DoubleSetting.Builder()
            .name("sound-volume")
            .description("Volume of the sound.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 1)
            .build()
    );

    public DisconnectSound() {
        super(Meteorist.CATEGORY, "disconnect-sound", "Plays a sound when the Disconnected Screen appears (e.g., when kicked).");
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void playSound() {
        mc.getSoundManager().play(PositionedSoundInstance.master(sound.get().getFirst(), soundPitch.get().floatValue(), soundVolume.get().floatValue()));
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton button = theme.button("Preview");
        button.action = this::playSound;
        return button;
    }

    @EventHandler
    private void onDisconnectedScreen(DisconnectedScreenEvent event) {
        if (this.isActive()) playSound();
    }
}
