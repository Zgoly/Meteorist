//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.Meteorist;

public class FastBridge extends Module {
    public FastBridge() {
        super(Meteorist.CATEGORY, "fast-bridge", "Automatically sneaks at block edge (idea by kokqi).");
    }

    boolean turn = true;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getBlockState(mc.player.getSteppingPos()).isAir()) {
            if (!mc.player.isOnGround()) return;
            turn = true;
            mc.options.sneakKey.setPressed(true);
        } else if (turn) {
            turn = false;
            mc.options.sneakKey.setPressed(false);
        }
    }
}
