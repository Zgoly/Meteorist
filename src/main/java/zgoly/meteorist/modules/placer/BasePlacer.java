package zgoly.meteorist.modules.placer;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import zgoly.meteorist.gui.screens.PlacerScreen;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BasePlacer implements ISerializable<BasePlacer> {
    public final Settings settings = new Settings();

    protected final SettingGroup sgName = settings.createGroup("Name");
    protected final SettingGroup sgCornerPos = settings.createGroup("Corner Positions");
    protected final SettingGroup sgCornerAnchorPos = settings.createGroup("Corner Anchor Positions");
    protected final SettingGroup sgRotation = settings.createGroup("Rotation");
    protected final SettingGroup sgAnchor = settings.createGroup("Anchor");
    protected final SettingGroup sgBlocks = settings.createGroup("Blocks");
    protected final SettingGroup sgColor = settings.createGroup("Color");
    protected final SettingGroup sgControls = settings.createGroup("Controls");

    public final Setting<String> name = sgName.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the placer.")
            .onChanged(value -> reloadParent())
            .build()
    );

    public final Setting<BlockPos> cornerPos1 = sgCornerPos.add(new BlockPosSetting.Builder()
            .name("corner-pos-1")
            .description("The first corner position of the placer.")
            .build()
    );
    public final Setting<BlockPos> cornerPos2 = sgCornerPos.add(new BlockPosSetting.Builder()
            .name("corner-pos-2")
            .description("The second corner position of the placer.")
            .build()
    );

    public final Setting<BlockPos> cornerAnchorPos1 = sgCornerAnchorPos.add(new BlockPosSetting.Builder()
            .name("corner-anchor-pos-1")
            .description("The first corner anchor position of the placer.")
            .build()
    );
    public final Setting<BlockPos> cornerAnchorPos2 = sgCornerAnchorPos.add(new BlockPosSetting.Builder()
            .name("corner-anchor-pos-2")
            .description("The second corner anchor position of the placer.")
            .build()
    );

    public final Setting<Boolean> rotateX1 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-x-1")
            .description("Is the first X position should be rotated where is the player looking at.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> rotateY1 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-y-1")
            .description("Is the first Y position should be rotated where is the player looking at.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> rotateZ1 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-z-1")
            .description("Is the first Z position should be rotated where is the player looking at.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> rotateX2 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-x-2")
            .description("Is the second X position should be rotated where is the player looking at.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> rotateY2 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-y-2")
            .description("Is the second Y position should be rotated where is the player looking at.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> rotateZ2 = sgRotation.add(new BoolSetting.Builder()
            .name("rotate-z-2")
            .description("Is the second Z position should be rotated where is the player looking at.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> anchorX1 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-x-1")
            .description("Is the first X position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> anchorY1 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-y-1")
            .description("Is the first Y position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> anchorZ1 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-z-1")
            .description("Is the first Z position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> anchorX2 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-x-2")
            .description("Is the second X position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> anchorY2 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-y-2")
            .description("Is the second Y position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> anchorZ2 = sgAnchor.add(new BoolSetting.Builder()
            .name("anchor-z-2")
            .description("Is the second Z position should be anchored (fixed) in the world space or be relative to the player.")
            .defaultValue(false)
            .build()
    );

    public final Setting<List<Block>> blocks = sgBlocks.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("The blocks that the placer should place.")
            .defaultValue(List.of(Blocks.COBBLESTONE))
            .build()
    );

    public final Setting<SettingColor> sideColor = sgColor.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the side of the placer hologram.")
            .defaultValue(new SettingColor(0, 255, 0, 50))
            .build()
    );
    public final Setting<SettingColor> lineColor = sgColor.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the line of the placer hologram.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .build()
    );

    public final Setting<Boolean> visible = sgControls.add(new BoolSetting.Builder()
            .name("visible")
            .description("Is the placer visible.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );
    public final Setting<Boolean> active = sgControls.add(new BoolSetting.Builder()
            .name("active")
            .description("Is the placer active.")
            .defaultValue(true)
            .onChanged(value -> reloadParent())
            .build()
    );

    public BasePlacer() {
    }

    public static void reloadParent() {
        Screen screen = mc.currentScreen;
        if (screen instanceof PlacerScreen placerScreen) {
            if (placerScreen.parent instanceof WindowScreen windowScreen) {
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
    public BasePlacer fromTag(NbtCompound tag) {
        NbtCompound settingsTag = (NbtCompound) tag.get("settings");
        if (settingsTag != null) settings.fromTag(settingsTag);

        return this;
    }

    public BasePlacer copy() {
        return new BasePlacer().fromTag(toTag());
    }
}
