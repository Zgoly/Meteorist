package zgoly.meteorist.modules.slotclick.selections;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import zgoly.meteorist.gui.screens.SlotSelectionScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaseSlotSelection implements ISerializable<BaseSlotSelection> {
    public Settings settings = new Settings();

    public static void reloadParent() {
        Screen screen = mc.currentScreen;
        if (screen instanceof SlotSelectionScreen slotSelectionScreen) {
            if (slotSelectionScreen.parent instanceof WindowScreen windowScreen) {
                windowScreen.reload();
            }
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BaseSlotSelection fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
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
