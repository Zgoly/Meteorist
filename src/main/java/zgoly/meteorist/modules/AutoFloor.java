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
import zgoly.meteorist.utils.Utils;

import java.util.List;

public class AutoFloor extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgColor = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to place.")
            .defaultValue(Blocks.COBBLESTONE)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotate head when placing a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgTiming.add(new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("Maximum blocks to try to place per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Integer> delay = sgTiming.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after placing block(s) in ticks (20 ticks = 1 sec).")
            .defaultValue(1)
            .range(1, 120)
            .sliderRange(1, 40)
            .build()
    );

    private final Setting<Integer> range = sgRange.add(new IntSetting.Builder()
            .name("range")
            .description("Range in which block(s) will be placed.")
            .defaultValue(1)
            .min(1)
            .build()
    );

    private final Setting<Integer> offset = sgRange.add(new IntSetting.Builder()
            .name("vertical-offset")
            .description("Vertical distance after which blocks will be placed.")
            .defaultValue(-1)
            .sliderRange(-5, 5)
            .build()
    );

    private final Setting<Boolean> show = sgColor.add(new BoolSetting.Builder()
            .name("show")
            .description("Shows overlay of the blocks being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sC = sgColor.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 200, 200, 40))
            .build()
    );

    private final Setting<SettingColor> lC = sgColor.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 200, 200, 100))
            .build()
    );

    private int timer;
    private boolean work;
    private boolean showBox;

    public AutoFloor() {
        super(Meteorist.CATEGORY, "auto-floor", "Put blocks under you like \"scaffold\" module, but in range.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
        showBox = true;
    }

    @Override
    public void onDeactivate() {
        showBox = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Iterable<BlockPos> BlockPoses = BlockPos.iterateOutwards(mc.player.getBlockPos().down(-offset.get()), range.get(), 0, range.get());
        if (work) {
            int count = 0;
            for (BlockPos blockPos : BlockPoses) {
                if (count >= maxBlocksPerTick.get()) break;
                if (!BlockUtils.canPlace(blockPos) || Utils.isCollidesEntity(blockPos)) continue;

                for (Block block : blocks.get()) {
                    FindItemResult item = InvUtils.findInHotbar(block.asItem());
                    BlockUtils.place(blockPos, item, rotate.get(), 0);
                }
                count++;
            }
            work = !work;
        }
        if (!work && timer >= delay.get()) {
            work = true;
            timer = 0;
        } else if (!work) {
            ++timer;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        BlockPos bP1 = mc.player.getBlockPos().add(range.get(), offset.get(), range.get()).add(1, 1, 1);
        BlockPos bP2 = mc.player.getBlockPos().add(-range.get(), offset.get(), -range.get());
        if (showBox && show.get()) {
            event.renderer.box(bP1.getX(), bP1.getY(), bP1.getZ(), bP2.getX(), bP2.getY(), bP2.getZ(), sC.get(), lC.get(), ShapeMode.Both, 0);
        } else event.renderer.end();
    }
}
