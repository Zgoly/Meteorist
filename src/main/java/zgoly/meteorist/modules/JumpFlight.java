package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import zgoly.meteorist.Meteorist;

public class JumpFlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> modifyHorizontalSpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("modify-horizontal-speed")
            .description("Whether to override player's horizontal movement speed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("How fast you move forward/backward and strafe left/right while flying.")
            .defaultValue(16)
            .sliderRange(1, 128)
            .min(1)
            .visible(modifyHorizontalSpeed::get)
            .build()
    );

    private final Setting<Integer> verticalStep = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-step")
            .description("How many blocks to ascend or descend per adjustment.")
            .defaultValue(1)
            .min(1)
            .build()
    );

    private final Setting<Integer> verticalAdjustTickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-adjust-delay")
            .description("Delay in ticks between vertical adjustments while holding Jump/Sneak.")
            .defaultValue(4)
            .min(1)
            .build()
    );

    private final Setting<Boolean> scrollControl = sgGeneral.add(new BoolSetting.Builder()
            .name("scroll-to-adjust-speed")
            .description("Allow adjusting horizontal speed with the mouse scroll wheel.")
            .defaultValue(true)
            .visible(modifyHorizontalSpeed::get)
            .build()
    );

    private final Setting<Double> scrollSensitivity = sgGeneral.add(new DoubleSetting.Builder()
            .name("scroll-sensitivity")
            .description("How much the speed changes per scroll tick.")
            .defaultValue(1)
            .sliderRange(0.1, 5)
            .min(0.1)
            .visible(() -> modifyHorizontalSpeed.get() && scrollControl.get())
            .build()
    );

    private boolean targetYInitialized, jumpHeld, sneakHeld;
    private int tickCounter, targetYLevel;

    public JumpFlight() {
        super(Meteorist.CATEGORY, "jump-flight", "Flight that uses repeated jumps to stay in the air. Works best with NoFall.");
    }

    @Override
    public void onActivate() {
        targetYInitialized = false;
        jumpHeld = false;
        sneakHeld = false;
        tickCounter = 0;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (modifyHorizontalSpeed.get()) {
            Vec3 vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
            ((IVec3d) event.movement).meteor$set(vel.x(), event.movement.y, vel.z());
        }
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (modifyHorizontalSpeed.get() && scrollControl.get()) {
            horizontalSpeed.set(horizontalSpeed.get() + event.value * scrollSensitivity.get());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        if (!targetYInitialized) {
            targetYLevel = mc.player.blockPosition().getY();
            targetYInitialized = true;
        }

        if (!Modules.get().isActive(Freecam.class) && mc.screen == null) {
            jumpHeld = mc.options.keyJump.isDown();
            sneakHeld = mc.options.keyShift.isDown();
        }

        tickCounter++;
        if (tickCounter >= verticalAdjustTickDelay.get()) {
            if (jumpHeld) {
                targetYLevel += verticalStep.get();
                tickCounter = 0;
            } else if (sneakHeld) {
                targetYLevel -= verticalStep.get();
                tickCounter = 0;
            }
        }

        if (mc.player.blockPosition().getY() <= targetYLevel) {
            mc.player.jumpFromGround();
        }
    }

    @Override
    public String getInfoString() {
        return "y=" + targetYLevel + (modifyHorizontalSpeed.get() ? "; s=" + horizontalSpeed.get() : "");
    }
}