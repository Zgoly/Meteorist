package zgoly.meteorist.modules.walkerist;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class Waypoint implements ISerializable<Waypoint> {
    public final Settings settings = new Settings();

    protected final SettingGroup sgPos = settings.createGroup("Position");

    public final Setting<BlockPos> pos = sgPos.add(new BlockPosSetting.Builder()
            .name("pos")
            .description("The position of the waypoint.")
            .build()
    );

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public Waypoint fromTag(CompoundTag tag) {
        CompoundTag settingsTag = (CompoundTag) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }
}
