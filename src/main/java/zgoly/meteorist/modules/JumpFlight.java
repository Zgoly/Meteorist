package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;

public class JumpFlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Flight speed.")
            .defaultValue(5)
            .range(1, 75)
            .sliderRange(1, 75)
            .build()
    );
    private final Setting<Integer> verticalSpeed = sgGeneral.add(new IntSetting.Builder()
            .name("change-y-axis-force")
            .description("Force of change y-axis.")
            .defaultValue(1)
            .range(1, 10)
            .sliderRange(1, 10)
            .build()
    );
    private final Setting<Boolean> scrollBool = sgGeneral.add(new BoolSetting.Builder()
            .name("scroll-to-speed")
            .description("Allows change speed using scroll wheel.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> scrollSens = sgGeneral.add(new DoubleSetting.Builder()
            .name("scroll-sensitivity")
            .description("Change speed using scroll wheel sensitivity.")
            .defaultValue(1)
            .range(0.1, 5)
            .sliderRange(0.1, 5)
            .visible(scrollBool::get)
            .build()
    );

    private boolean work = true;
    private int level;

    public JumpFlight() {
        super(Meteorist.CATEGORY, "jump-flight", "Flight that using jumps for fly. No fall recommended.");
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
        ((IVec3d) event.movement).meteor$set(velX, event.movement.y, velZ);
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;
        if (Modules.get().isActive(Freecam.class) || mc.currentScreen != null) return;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            for (int i = 0; i < verticalSpeed.get(); i++) level++;
        } else if (mc.options.sneakKey.matchesKey(event.key, 0)) {
            for (int i = 0; i < verticalSpeed.get(); i++) level--;
        }
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (scrollBool.get()) speed.set(speed.get() + event.value * scrollSens.get());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (work) {
            work = false;
            level = mc.player.getBlockPos().getY();
        }
        if (mc.player.getBlockPos().getY() <= level) {
            mc.player.jump();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        work = true;
    }
}