//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import zgoly.meteorist.Meteorist;
import java.util.Arrays;
import java.util.List;

public class AutoLight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");
    private final SettingGroup sgColor = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Light blocks to check.")
            .defaultValue(Blocks.TORCH)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("place")
            .description("Place blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotate head when placing a block.")
            .defaultValue(true)
            .visible(place::get)
            .build()
    );

    private final Setting<Boolean> dynHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("dynamic-height")
            .description("Places light source on the ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> range = sgRange.add(new IntSetting.Builder()
            .name("range")
            .description("Range to light source.")
            .defaultValue(2)
            .min(1)
            .build()
    );

    private final Setting<Integer> gRange = sgRange.add(new IntSetting.Builder()
            .name("grid-range")
            .description("Grid range in blocks.")
            .defaultValue(13)
            .min(1)
            .build()
    );

    private final Setting<Boolean> show = sgColor.add(new BoolSetting.Builder()
            .name("show")
            .description("Shows overlay of suggested places for light.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sC1 = sgColor.add(new ColorSetting.Builder()
            .name("main-side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .build()
    );

    private final Setting<SettingColor> lC1 = sgColor.add(new ColorSetting.Builder()
            .name("main-line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .build()
    );

    private final Setting<SettingColor> sC2 = sgColor.add(new ColorSetting.Builder()
            .name("hologram-side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 255, 0, 40))
            .build()
    );

    private final Setting<SettingColor> lC2 = sgColor.add(new ColorSetting.Builder()
            .name("hologram-line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(255, 255, 0, 100))
            .build()
    );

    public AutoLight() {
        super(Meteorist.CATEGORY, "auto-light", "Shows best place to place light source block.");
    }

    boolean showBox = false;
    BlockPos finalPos = new BlockPos(0,0,0);
    List<BlockPos> sides = Arrays.asList(new BlockPos(0,0,0), new BlockPos(0,0,0), new BlockPos(0,0,0), new BlockPos(0,0,0));

    @Override
    public void onActivate() {
        showBox = true;
    }

    @Override
    public void onDeactivate() {
        showBox = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Iterable<BlockPos> BlockPoses = BlockPos.iterateOutwards(mc.player.getBlockPos(), range.get(), range.get(), range.get());
        for (BlockPos blockPos : BlockPoses) {
            blockPos = blockPos.toImmutable();
            if (blocks.get().contains(mc.world.getBlockState(blockPos).getBlock())) {
                finalPos = blockPos;

                sides.set(0, finalPos.north(gRange.get()));
                sides.set(1, finalPos.south(gRange.get()));
                sides.set(2, finalPos.west(gRange.get()));
                sides.set(3, finalPos.east(gRange.get()));

                if (dynHeight.get()) {
                    for (BlockPos elem : sides) {
                        while (!mc.world.getBlockState(elem).getMaterial().isReplaceable()) elem = elem.up(1);

                        while (mc.world.getBlockState(elem.down(1)).getMaterial().isReplaceable()) elem = elem.down(1);
                    }
                }

                showBox = true;
            }
            if (place.get() & mc.world.getBlockState(blockPos).isAir()) {
                if (sides.contains(blockPos)) {
                    for (Block b : blocks.get()) {
                        FindItemResult item = InvUtils.findInHotbar(b.asItem());
                        BlockUtils.place(blockPos, item, rotate.get(), 0);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (showBox && show.get()) {
            event.renderer.box(finalPos, sC1.get(), lC1.get(), ShapeMode.Both, 0);
            for (BlockPos elem : sides) {
                event.renderer.box(elem, sC2.get(), lC2.get(), ShapeMode.Both, 0);
            }
        } else event.renderer.end();
    }
}
