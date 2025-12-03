package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import zgoly.meteorist.gui.screens.SlotSelectionScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseSlotSelection implements ISerializable<BaseSlotSelection> {
    public Settings settings = new Settings();

    public static void reloadParent() {
        Screen screen = mc.screen;
        if (screen instanceof SlotSelectionScreen slotSelectionScreen) {
            if (slotSelectionScreen.parent instanceof WindowScreen windowScreen) {
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
    public BaseSlotSelection fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public String getTypeName() {
        return null;
    }

    public BaseSlotSelection copy() {
        return null;
    }
}
