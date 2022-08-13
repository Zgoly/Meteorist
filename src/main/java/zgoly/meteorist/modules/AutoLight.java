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
            .defaultValue(4)
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
    BlockPos xP = new BlockPos(0,0,0);
    BlockPos xM = new BlockPos(0,0,0);
    BlockPos zP = new BlockPos(0,0,0);
    BlockPos zM = new BlockPos(0,0,0);


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
        for (int x = -range.get(); x <= range.get(); x++) {
            for (int y = -range.get(); y <= range.get(); y++) {
                for (int z = -range.get(); z <= range.get(); z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    final Block block = mc.world.getBlockState(pos).getBlock();
                    if (blocks.get().contains(block)) {
                        finalPos = pos;
                        xP = finalPos.add(gRange.get(), 0, 0);
                        xM = finalPos.add(-gRange.get(), 0, 0);
                        zP = finalPos.add(0, 0, gRange.get());
                        zM = finalPos.add(0, 0, -gRange.get());

                        if (dynHeight.get()) {
                            while (!mc.world.getBlockState(xP).isAir()) xP = xP.add(0, 1, 0);
                            while (mc.world.getBlockState(xP.add(0, -1, 0)).isAir()) xP = xP.add(0, -1, 0);
                            while (!mc.world.getBlockState(xM).isAir()) xM = xM.add(0, 1, 0);
                            while (mc.world.getBlockState(xM.add(0, -1, 0)).isAir()) xM = xM.add(0, -1, 0);
                            while (!mc.world.getBlockState(zP).isAir()) zP = zP.add(0, 1, 0);
                            while (mc.world.getBlockState(zP.add(0, -1, 0)).isAir()) zP = zP.add(0, -1, 0);
                            while (!mc.world.getBlockState(zM).isAir()) zM = zM.add(0, 1, 0);
                            while (mc.world.getBlockState(zM.add(0, -1, 0)).isAir()) zM = zM.add(0, -1, 0);
                        }

                        showBox = true;
                    }
                    if (place.get() & mc.world.getBlockState(pos).isAir()) {
                        if (pos.toString().contains(xP.toString()) ||
                                pos.toString().contains(xM.toString()) ||
                                pos.toString().contains(zP.toString()) ||
                                pos.toString().contains(zM.toString())) {
                            for (Block b : blocks.get()) {
                                FindItemResult item = InvUtils.findInHotbar(b.asItem());
                                BlockUtils.place(pos, item, rotate.get(), 0, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (showBox && show.get()) {
            event.renderer.box(finalPos, sC1.get(), lC1.get(), ShapeMode.Both, 0);
            event.renderer.box(xP, sC2.get(), lC2.get(), ShapeMode.Both, 0);
            event.renderer.box(xM, sC2.get(), lC2.get(), ShapeMode.Both, 0);
            event.renderer.box(zP, sC2.get(), lC2.get(), ShapeMode.Both, 0);
            event.renderer.box(zM, sC2.get(), lC2.get(), ShapeMode.Both, 0);
        } else event.renderer.end();
    }
}
