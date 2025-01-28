package zgoly.meteorist.modules;

import com.google.common.util.concurrent.AtomicDouble;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.events.*;
import zgoly.meteorist.utils.misc.TweenHandler;

import java.util.List;

public class ZoomPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgZoomIn = settings.createGroup("Zoom In");
    private final SettingGroup sgZoomOut = settings.createGroup("Zoom Out");
    private final SettingGroup sgScrollIn = settings.createGroup("Scroll In");
    private final SettingGroup sgScrollOut = settings.createGroup("Scroll Out");

    private final Setting<Double> zoomFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("zoom-factor")
            .description("The factor by which to zoom.")
            .defaultValue(6)
            .min(2)
            .build()
    );
    private final Setting<Double> scrollStep = sgGeneral.add(new DoubleSetting.Builder()
            .name("scroll-step")
            .description("The step size for scrolling.")
            .defaultValue(1)
            .min(0)
            .build()
    );
    private final Setting<Boolean> saveScrollStep = sgGeneral.add(new BoolSetting.Builder()
            .name("save-scroll-step")
            .description("Whether to save the scroll step value between sessions.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> cinematic = sgGeneral.add(new BoolSetting.Builder()
            .name("cinematic")
            .description("Enables cinematic mode when zooming.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> cinematicFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("cinematic-factor")
            .description("The factor for cinematic mode.")
            .defaultValue(1)
            .min(0.1)
            .visible(cinematic::get)
            .build()
    );
    private final Setting<Boolean> normalizeCinematic = sgGeneral.add(new BoolSetting.Builder()
            .name("normalize-cinematic")
            .description("Whether to normalize cinematic mode.")
            .defaultValue(true)
            .visible(cinematic::get)
            .build()
    );
    private final Setting<Boolean> normalizeSensitivity = sgGeneral.add(new BoolSetting.Builder()
            .name("normalize-sensitivity")
            .description("Whether to normalize sensitivity.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> sensitivityNormalizationFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("sensitivity-normalization-factor")
            .description("The factor for normalized sensitivity.")
            .defaultValue(0.5)
            .min(0)
            .visible(normalizeSensitivity::get)
            .build()
    );
    private final Setting<Boolean> normalizeZoom = sgGeneral.add(new BoolSetting.Builder()
            .name("normalize-zoom")
            .description("Whether to normalize zoom.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> zoomNormalizationFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("zoom-normalization-factor")
            .description("The factor for normalized zoom.")
            .defaultValue(0.25)
            .min(0)
            .visible(normalizeZoom::get)
            .build()
    );
    private final Setting<Boolean> hideHud = sgGeneral.add(new BoolSetting.Builder()
            .name("hide-HUD")
            .description("Whether to hide the HUD.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
            .name("render-hands")
            .description("Whether to render hands.")
            .defaultValue(true)
            .visible(() -> !hideHud.get())
            .build()
    );
    private final Setting<Boolean> scroll = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-scroll")
            .description("Whether to allow zoom scrolling.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> instantZoomIn = sgZoomIn.add(new BoolSetting.Builder()
            .name("instant-zoom-in")
            .description("Whether to zoom in instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<TweenHandler.EasingStyle> zoomInEasingStyle = sgZoomIn.add(new EnumSetting.Builder<TweenHandler.EasingStyle>()
            .name("easing-style")
            .description("The easing style for zooming in.")
            .defaultValue(TweenHandler.EasingStyle.Quint)
            .visible(() -> !instantZoomIn.get())
            .build()
    );
    private final Setting<TweenHandler.EasingDirection> zoomInEasingDirection = sgZoomIn.add(new EnumSetting.Builder<TweenHandler.EasingDirection>()
            .name("easing-direction")
            .description("The direction of the easing for zooming in.")
            .defaultValue(TweenHandler.EasingDirection.Out)
            .visible(() -> !instantZoomIn.get())
            .build()
    );
    private final Setting<Double> zoomInEasingDuration = sgZoomIn.add(new DoubleSetting.Builder()
            .name("easing-duration")
            .description("The duration of the easing for zooming in.")
            .defaultValue(1)
            .min(0.1)
            .visible(() -> !instantZoomIn.get())
            .build()
    );
    private final Setting<Boolean> zoomInPlaySound = sgZoomIn.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("Whether to play a sound when zooming in.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<SoundEvent>> zoomInSound = sgZoomIn.add(new SoundEventListSetting.Builder()
            .name("sound")
            .description("Sound to play.")
            .defaultValue(List.of(SoundEvents.UI_TOAST_IN))
            .visible(zoomInPlaySound::get)
            .build()
    );
    private final Setting<Double> zoomInSoundPitch = sgZoomIn.add(new DoubleSetting.Builder()
            .name("sound-pitch")
            .description("Pitch of the sound.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0.5, 2)
            .visible(zoomInPlaySound::get)
            .build()
    );
    private final Setting<Double> zoomInSoundVolume = sgZoomIn.add(new DoubleSetting.Builder()
            .name("sound-volume")
            .description("Volume of the sound.")
            .defaultValue(0.25)
            .min(0)
            .sliderRange(0, 1)
            .visible(zoomInPlaySound::get)
            .build()
    );

    private final Setting<Boolean> instantZoomOut = sgZoomOut.add(new BoolSetting.Builder()
            .name("instant-zoom-out")
            .description("Whether to zoom out instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<TweenHandler.EasingStyle> zoomOutEasingStyle = sgZoomOut.add(new EnumSetting.Builder<TweenHandler.EasingStyle>()
            .name("easing-style")
            .description("The easing style for zooming out.")
            .defaultValue(TweenHandler.EasingStyle.Quint)
            .visible(() -> !instantZoomOut.get())
            .build()
    );
    private final Setting<TweenHandler.EasingDirection> zoomOutEasingDirection = sgZoomOut.add(new EnumSetting.Builder<TweenHandler.EasingDirection>()
            .name("easing-direction")
            .description("The direction of the easing for zooming out.")
            .defaultValue(TweenHandler.EasingDirection.Out)
            .visible(() -> !instantZoomOut.get())
            .build()
    );
    private final Setting<Double> zoomOutEasingDuration = sgZoomOut.add(new DoubleSetting.Builder()
            .name("easing-duration")
            .description("The duration of the easing for zooming out.")
            .defaultValue(1)
            .min(0.1)
            .visible(() -> !instantZoomOut.get())
            .build()
    );
    private final Setting<Boolean> zoomOutPlaySound = sgZoomOut.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("Whether to play a sound when zooming out.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<SoundEvent>> zoomOutSound = sgZoomOut.add(new SoundEventListSetting.Builder()
            .name("sound")
            .description("Sound to play.")
            .defaultValue(List.of(SoundEvents.UI_TOAST_OUT))
            .visible(zoomOutPlaySound::get)
            .build()
    );
    private final Setting<Double> zoomOutSoundPitch = sgZoomOut.add(new DoubleSetting.Builder()
            .name("sound-pitch")
            .description("Pitch of the sound.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0.5, 2)
            .visible(zoomOutPlaySound::get)
            .build()
    );
    private final Setting<Double> zoomOutSoundVolume = sgZoomOut.add(new DoubleSetting.Builder()
            .name("sound-volume")
            .description("Volume of the sound.")
            .defaultValue(0.25)
            .min(0)
            .sliderRange(0, 1)
            .visible(zoomOutPlaySound::get)
            .build()
    );

    private final Setting<Boolean> instantScrollIn = sgScrollIn.add(new BoolSetting.Builder()
            .name("instant-scroll-in")
            .description("Whether to scroll in instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<TweenHandler.EasingStyle> scrollInEasingStyle = sgScrollIn.add(new EnumSetting.Builder<TweenHandler.EasingStyle>()
            .name("easing-style")
            .description("The easing style for scrolling in.")
            .defaultValue(TweenHandler.EasingStyle.Quint)
            .visible(() -> !instantScrollIn.get())
            .build()
    );
    private final Setting<TweenHandler.EasingDirection> scrollInEasingDirection = sgScrollIn.add(new EnumSetting.Builder<TweenHandler.EasingDirection>()
            .name("easing-direction")
            .description("The direction of the easing for scrolling in.")
            .defaultValue(TweenHandler.EasingDirection.Out)
            .visible(() -> !instantScrollIn.get())
            .build()
    );
    private final Setting<Double> scrollInEasingDuration = sgScrollIn.add(new DoubleSetting.Builder()
            .name("easing-duration")
            .description("The duration of the easing for scrolling in.")
            .defaultValue(0.5)
            .min(0.1)
            .visible(() -> !instantScrollIn.get())
            .build()
    );
    private final Setting<Boolean> scrollInPlaySound = sgScrollIn.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("Whether to play a sound when scrolling in.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<SoundEvent>> scrollInSound = sgScrollIn.add(new SoundEventListSetting.Builder()
            .name("sound")
            .description("Sound to play.")
            .defaultValue(List.of(SoundEvents.ITEM_SPYGLASS_USE))
            .visible(scrollInPlaySound::get)
            .build()
    );
    private final Setting<Double> scrollInSoundPitch = sgScrollIn.add(new DoubleSetting.Builder()
            .name("sound-pitch")
            .description("Pitch of the sound.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0.5, 2)
            .visible(scrollInPlaySound::get)
            .build()
    );
    private final Setting<Double> scrollInSoundVolume = sgScrollIn.add(new DoubleSetting.Builder()
            .name("sound-volume")
            .description("Volume of the sound.")
            .defaultValue(0.5)
            .min(0)
            .sliderRange(0, 1)
            .visible(scrollInPlaySound::get)
            .build()
    );

    private final Setting<Boolean> instantScrollOut = sgScrollOut.add(new BoolSetting.Builder()
            .name("instant-scroll-out")
            .description("Whether to scroll out instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<TweenHandler.EasingStyle> scrollOutEasingStyle = sgScrollOut.add(new EnumSetting.Builder<TweenHandler.EasingStyle>()
            .name("easing-style")
            .description("The easing style for scrolling out.")
            .defaultValue(TweenHandler.EasingStyle.Quint)
            .visible(() -> !instantScrollOut.get())
            .build()
    );
    private final Setting<TweenHandler.EasingDirection> scrollOutEasingDirection = sgScrollOut.add(new EnumSetting.Builder<TweenHandler.EasingDirection>()
            .name("easing-direction")
            .description("The direction of the easing for scrolling out.")
            .defaultValue(TweenHandler.EasingDirection.Out)
            .visible(() -> !instantScrollOut.get())
            .build()
    );
    private final Setting<Double> scrollOutEasingDuration = sgScrollOut.add(new DoubleSetting.Builder()
            .name("easing-duration")
            .description("The duration of the easing for scrolling out.")
            .defaultValue(0.5)
            .min(0.1)
            .visible(() -> !instantScrollOut.get())
            .build()
    );
    private final Setting<Boolean> scrollOutPlaySound = sgScrollOut.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("Whether to play a sound when scrolling out.")
            .defaultValue(true)
            .build()
    );
    private final Setting<List<SoundEvent>> scrollOutSound = sgScrollOut.add(new SoundEventListSetting.Builder()
            .name("sound")
            .description("Sound to play.")
            .defaultValue(List.of(SoundEvents.ITEM_SPYGLASS_STOP_USING))
            .visible(scrollOutPlaySound::get)
            .build()
    );
    private final Setting<Double> scrollOutSoundPitch = sgScrollOut.add(new DoubleSetting.Builder()
            .name("sound-pitch")
            .description("Pitch of the sound.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0.5, 2)
            .visible(scrollOutPlaySound::get)
            .build()
    );
    private final Setting<Double> scrollOutSoundVolume = sgScrollOut.add(new DoubleSetting.Builder()
            .name("sound-volume")
            .description("Volume of the sound.")
            .defaultValue(0.5)
            .min(0)
            .sliderRange(0, 1)
            .visible(scrollOutPlaySound::get)
            .build()
    );

    private final TweenHandler tweenHandler = new TweenHandler();

    private final AtomicDouble currentZoomFactor = new AtomicDouble(1);
    private final AtomicDouble currentScrollStep = new AtomicDouble(0);

    private double targetScrollStep = 0;
    private boolean isSubscribed = false;
    private boolean deactivated = false;

    public ZoomPlus() {
        super(Meteorist.CATEGORY, "zoom-plus", "Advanced Zoom module with more customizable settings.");
        autoSubscribe = false;
    }

    @EventHandler
    public void onActivate() {
        deactivated = false;

        if (!isSubscribed) {
            if (hideHud.get()) mc.options.hudHidden = true;
            MeteorClient.EVENT_BUS.subscribe(this);
            MeteorClient.EVENT_BUS.subscribe(tweenHandler);
            isSubscribed = true;
        }

        if (zoomInPlaySound.get()) {
            mc.getSoundManager().play(PositionedSoundInstance.master(zoomInSound.get().getFirst(), zoomInSoundPitch.get().floatValue(), zoomInSoundVolume.get().floatValue()));
        }

        tweenHandler.play(instantZoomIn.get(), currentZoomFactor, zoomFactor.get(), zoomInEasingDuration.get(), zoomInEasingStyle.get(), zoomInEasingDirection.get());

        if (scroll.get()) {
            if (saveScrollStep.get()) {
                tweenHandler.play(instantZoomIn.get(), currentScrollStep, targetScrollStep, zoomInEasingDuration.get(), zoomInEasingStyle.get(), zoomInEasingDirection.get());
            } else {
                targetScrollStep = 0;
            }
        }
    }

    @EventHandler
    public void onDeactivate() {
        deactivated = true;

        if (zoomOutPlaySound.get()) {
            mc.getSoundManager().play(PositionedSoundInstance.master(zoomOutSound.get().getFirst(), zoomOutSoundPitch.get().floatValue(), zoomOutSoundVolume.get().floatValue()));
        }

        tweenHandler.play(instantZoomOut.get(), currentZoomFactor, 1, zoomOutEasingDuration.get(), zoomOutEasingStyle.get(), zoomOutEasingDirection.get());
        tweenHandler.play(instantZoomOut.get(), currentScrollStep, 0, zoomOutEasingDuration.get(), zoomOutEasingStyle.get(), zoomOutEasingDirection.get());
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (deactivated || !scroll.get()) return;
        targetScrollStep = Math.max(0, event.value * scrollStep.get() + targetScrollStep);

        boolean scrollIn = event.value > 0;

        boolean playSound = scrollIn ? scrollInPlaySound.get() : scrollOutPlaySound.get();
        if (playSound) {
            SoundEvent sound = scrollIn ? scrollInSound.get().getFirst() : scrollOutSound.get().getFirst();
            float soundPitch = scrollIn ? scrollInSoundPitch.get().floatValue() : scrollOutSoundPitch.get().floatValue();
            float soundVolume = scrollIn ? scrollInSoundVolume.get().floatValue() : scrollOutSoundVolume.get().floatValue();
            mc.getSoundManager().play(PositionedSoundInstance.master(sound, soundPitch, soundVolume));
        }

        boolean instantScroll = scrollIn ? instantScrollIn.get() : instantScrollOut.get();
        double scrollEasingDuration = scrollIn ? scrollInEasingDuration.get() : scrollOutEasingDuration.get();
        TweenHandler.EasingStyle scrollEasingStyle = scrollIn ? scrollInEasingStyle.get() : scrollOutEasingStyle.get();
        TweenHandler.EasingDirection scrollEasingDirection = scrollIn ? scrollInEasingDirection.get() : scrollOutEasingDirection.get();

        tweenHandler.play(instantScroll, currentScrollStep, targetScrollStep, scrollEasingDuration, scrollEasingStyle, scrollEasingDirection);

        event.cancel();
    }

    @EventHandler
    private void onGetFov(GetFovEvent event) {
        event.fov /= (float) getScaling();
    }

    @EventHandler
    private void onHandRender(HandRenderEvent event) {
        if (!renderHands.get()) event.renderHand = false;
    }

    @EventHandler
    private void onGetMouseSensitivity(MouseSensitivityEvent event) {
        if (normalizeSensitivity.get()) {
            event.sensitivity /= 1 + (getScaling() - 1) * sensitivityNormalizationFactor.get();
        }
    }

    @EventHandler
    private void onUpdateCameraSmoothing(UpdateCameraSmoothingEvent event) {
        if (cinematic.get()) {
            double normalizedCinematic = normalizeCinematic.get() ? getScaling() : 1;
            event.timeDelta *= cinematicFactor.get() * normalizedCinematic;
        }
    }

    @EventHandler
    public void onTweenEnd(TweenEndEvent event) {
        if (currentZoomFactor.get() == 1) {
            if (hideHud.get()) mc.options.hudHidden = false;
            MeteorClient.EVENT_BUS.unsubscribe(this);
            MeteorClient.EVENT_BUS.unsubscribe(tweenHandler);
            isSubscribed = false;
        }
    }

    @EventHandler
    public void onSmoothCameraEnabled(SmoothCameraEnabledEvent event) {
        if (deactivated) return;
        event.enabled = cinematic.get();
    }

    private double getScaling() {
        double finalZoomFactor = currentZoomFactor.get() + currentScrollStep.get();
        if (normalizeZoom.get()) return Math.exp(zoomNormalizationFactor.get() * (finalZoomFactor - 1));
        return finalZoomFactor;
    }
}
