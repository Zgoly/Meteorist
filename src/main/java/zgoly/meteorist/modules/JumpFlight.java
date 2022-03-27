//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;

public class JumpFlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed:")
            .description("Flight speed.")
            .defaultValue(5)
            .min(1)
            .range(1, 50)
            .sliderRange(1, 50)
            .build()
    );

    private final Setting<Boolean> scrollBool = sgGeneral.add(new BoolSetting.Builder()
            .name("scroll-to-speed")
            .description("Allows change speed using scroll wheel.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> scrollSens = sgGeneral.add(new DoubleSetting.Builder()
            .name("scroll-sensitivity:")
            .description("Allows change speed using scroll wheel.")
            .defaultValue(1)
            .range(0.1, 5)
            .sliderRange(0.1, 5)
            .visible(scrollBool::get)
            .build()
    );

    private int level;
    public JumpFlight() {
        super(Meteorist.CATEGORY, "jump-flight", "Flight that using jumps for fly. Can bypass some anti-cheats. No fall recommended.");
    }

    @Override
    public void onActivate() {
        level = mc.player.getBlockPos().getY();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        double velX = vel.getX();
        double velZ = vel.getZ();
        ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            mc.player.jump();
            level++;
        } else if (mc.options.sneakKey.matchesKey(event.key, 0)) {
            level--;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (scrollBool.get()) speed.set(speed.get() + event.value * scrollSens.get());
        //Voice в моих ушах — я позабыл все дни недели
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.getBlockPos().getY() == level) {
            mc.player.jump();
        }
    }
}