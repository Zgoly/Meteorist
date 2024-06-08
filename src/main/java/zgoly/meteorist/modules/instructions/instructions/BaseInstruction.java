package zgoly.meteorist.modules.instructions.instructions;

import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

public class BaseInstruction implements ISerializable<BaseInstruction> {
    public final Settings settings = new Settings();

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public BaseInstruction fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public String getTypeName() {
        return null;
    }

    public BaseInstruction copy() {
        return null;
    }
}
