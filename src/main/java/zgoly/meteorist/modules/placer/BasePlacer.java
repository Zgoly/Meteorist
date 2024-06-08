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
    public final Setting<String> name = sgName.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the placer.")
            .onChanged(value -> reloadParent())
            .build()
    );
    protected final SettingGroup sgCornerPos = settings.createGroup("Corner Positions");
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
    protected final SettingGroup sgCornerAnchorPos = settings.createGroup("Corner Anchor Positions");
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
    protected final SettingGroup sgRotation = settings.createGroup("Rotation");
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
    protected final SettingGroup sgAnchor = settings.createGroup("Anchor");
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
    protected final SettingGroup sgBlocks = settings.createGroup("Blocks");
    public final Setting<List<Block>> blocks = sgBlocks.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("The blocks that the placer should place.")
            .defaultValue(List.of(Blocks.COBBLESTONE))
            .build()
    );
    protected final SettingGroup sgColor = settings.createGroup("Color");
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
    protected final SettingGroup sgControls = settings.createGroup("Controls");
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
        BasePlacer copy = new BasePlacer();

        copy.name.set(this.name.get());

        copy.cornerPos1.set(this.cornerPos1.get());
        copy.cornerPos2.set(this.cornerPos2.get());

        copy.cornerAnchorPos1.set(this.cornerAnchorPos1.get());
        copy.cornerAnchorPos2.set(this.cornerAnchorPos2.get());

        copy.rotateX1.set(this.rotateX1.get());
        copy.rotateY1.set(this.rotateY1.get());
        copy.rotateZ1.set(this.rotateZ1.get());
        copy.rotateX2.set(this.rotateX2.get());
        copy.rotateY2.set(this.rotateY2.get());
        copy.rotateZ2.set(this.rotateZ2.get());

        copy.anchorX1.set(this.anchorX1.get());
        copy.anchorY1.set(this.anchorY1.get());
        copy.anchorZ1.set(this.anchorZ1.get());
        copy.anchorX2.set(this.anchorX2.get());
        copy.anchorY2.set(this.anchorY2.get());
        copy.anchorZ2.set(this.anchorZ2.get());

        copy.blocks.set(this.blocks.get());

        copy.sideColor.set(this.sideColor.get());
        copy.lineColor.set(this.lineColor.get());

        copy.visible.set(this.visible.get());
        copy.active.set(this.active.get());

        return copy;
    }
}
