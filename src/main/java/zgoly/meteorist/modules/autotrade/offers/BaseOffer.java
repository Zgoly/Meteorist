package zgoly.meteorist.modules.autotrade.offers;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import zgoly.meteorist.gui.screens.OfferScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseOffer implements ISerializable<BaseOffer> {
    public Settings settings = new Settings();

    SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables this offer.")
            .defaultValue(true)
            .build()
    );

    public static void reloadParent() {
        Screen screen = mc.screen;
        if (screen instanceof OfferScreen offerScreen) {
            if (offerScreen.parent instanceof WindowScreen windowScreen) {
                windowScreen.reload();
            }
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BaseOffer fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public String getTypeName() {
        return null;
    }

    public BaseOffer copy() {
        return null;
    }
}
