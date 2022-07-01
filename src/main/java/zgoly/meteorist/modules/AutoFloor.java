//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import zgoly.meteorist.Meteorist;

import java.util.List;

public class AutoFloor extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to place.")
            .defaultValue(Blocks.COBBLESTONE)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotate camera?")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> hRange = sgRange.add(new IntSetting.Builder()
            .name("Horizontal range")
            .description("Range in which blocks will be placed.")
            .defaultValue(1)
            .min(1)
            .build()
    );

    private final Setting<Integer> vRange = sgRange.add(new IntSetting.Builder()
            .name("Vertical range")
            .description("Distance after which blocks will be placed by \"Y\" axis")
            .defaultValue(1)
            .min(1)
            .build()
    );

    public AutoFloor() {
        super(Meteorist.CATEGORY, "auto-floor", "Put blocks under you like \"scaffold\" module, but in range.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (int x = -hRange.get(); x <= hRange.get(); x++) {
            for (int z = -hRange.get(); z <= hRange.get(); z++) {
                BlockPos pos = mc.player.getBlockPos().add(x, -vRange.get(), z);
                if (mc.world.getBlockState(pos).isAir()) {
                    for (Block b : blocks.get()) {
                        FindItemResult item = InvUtils.findInHotbar(b.asItem());
                        BlockUtils.place(pos, item, rotate.get(), 0, true);
                    }
                }
            }
        }
    }
}
