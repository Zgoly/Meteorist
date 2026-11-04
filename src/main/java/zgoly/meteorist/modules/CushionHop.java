package zgoly.meteorist.modules;

import com.mojang.math.Constants;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import zgoly.meteorist.Meteorist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CushionHop extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> repeatRate = sgGeneral.add(new IntSetting.Builder()
            .name("repeat-rate")
            .description("Ticks between each hop when holding a movement key.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Boolean> useInitialDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("initial-delay")
            .description("Adds a delay after the first hop before continuous hopping starts.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> initialDelayTicks = sgGeneral.add(new IntSetting.Builder()
            .name("initial-delay-ticks")
            .description("How many ticks to wait after the first hop before starting continuous hopping.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 40)
            .visible(useInitialDelay::get)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to the next cushion.")
            .defaultValue(3.0)
            .min(0.0)
            .sliderRange(0.5, 6.0)
            .build()
    );

    private final Setting<Double> searchAngle = sgGeneral.add(new DoubleSetting.Builder()
            .name("search-angle")
            .description("Maximum angle in degrees from your look direction to search for cushions.")
            .defaultValue(75.0)
            .min(10.0)
            .sliderRange(10, 180)
            .build()
    );

    private final Setting<Boolean> autoPathing = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-pathing")
            .description("Automatically hops to the nearest available cushion if none is in your cursor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates the camera towards the cushion before sitting on it.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swings your hand when moving.")
            .defaultValue(true)
            .build()
    );

    private final Set<Cushion> visitedCushions = new HashSet<>();
    private int cooldownTicks;
    private boolean initialDelayApplied;

    public CushionHop() {
        super(Meteorist.CATEGORY, "cushion-hop", "Lets you ride cushions by moving between them with WASD.");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        visitedCushions.clear();
        cooldownTicks = 0;
        initialDelayApplied = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive() || mc.player == null || mc.level == null) return;

        Cushion current = mc.player.getVehicle() instanceof Cushion c ? c : null;
        if (current == null || current.isRemoved() || mc.player.isShiftKeyDown()) {
            resetState();
            return;
        }

        double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double strafe = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);

        if (forward == 0 && strafe == 0) {
            resetState();
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Vec3 dir = getDirection(mc.player.getYRot(), mc.player.getXRot(), forward, strafe);
        Cushion target = findCushion(current, dir, false);

        if (target == null && autoPathing.get()) {
            target = findCushion(current, Vec3.ZERO, true);
        }

        if (target == null) return;

        if (autoPathing.get()) visitedCushions.add(current);
        moveTo(target);

        if (!initialDelayApplied && useInitialDelay.get()) {
            cooldownTicks = Math.max(0, initialDelayTicks.get() - 1);
            initialDelayApplied = true;
        } else {
            cooldownTicks = Math.max(0, repeatRate.get() - 1);
        }
    }

    private void moveTo(Cushion target) {
        if (target == null) return;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 100, true, () -> interact(target));
        } else {
            interact(target);
        }
    }

    private void interact(Cushion target) {
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (!(mc.player.getVehicle() instanceof Cushion)) return;
        if (target.isRemoved()) return;

        if (swingHand.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getInteractAnimation(), false);
        }

        EntityHitResult hitResult = new EntityHitResult(target, target.getBoundingBox().getCenter());
        mc.gameMode.interact(mc.player, target, hitResult, InteractionHand.MAIN_HAND);
    }

    private Cushion findCushion(Cushion origin, Vec3 dir, boolean ignoreDirection) {
        Vec3 eyePos = mc.player.getEyePosition();
        AABB searchBox = new AABB(eyePos, eyePos).inflate(range.get());
        List<Cushion> cushions = mc.level.getEntitiesOfClass(Cushion.class, searchBox);

        Cushion best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double angleThreshold = Math.cos(Math.toRadians(searchAngle.get()));

        for (Cushion cushion : cushions) {
            if (cushion == origin || cushion.isRemoved()) continue;
            if (autoPathing.get() && visitedCushions.contains(cushion)) continue;

            Vec3 diff = cushion.getBoundingBox().getCenter().subtract(eyePos);
            double distance = diff.length();

            if (distance < Constants.EPSILON || distance > range.get()) continue;

            double score;

            if (!ignoreDirection) {
                double dot = diff.dot(dir);
                if (dot <= 0) continue;

                double cosAngle = dot / distance;
                if (cosAngle < angleThreshold) continue;

                score = cosAngle * 3.0 - distance * 0.5;
            } else {
                score = -distance;
            }

            if (score > bestScore) {
                bestScore = score;
                best = cushion;
            }
        }

        return best;
    }

    private Vec3 getDirection(float yaw, float pitch, double forward, double strafe) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double cosPitch = Math.cos(pitchRad);
        double viewX = -Math.sin(yawRad) * cosPitch;
        double viewY = -Math.sin(pitchRad);
        double viewZ = Math.cos(yawRad) * cosPitch;

        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        double x = viewX * forward + rightX * strafe;
        double y = viewY * forward;
        double z = viewZ * forward + rightZ * strafe;

        Vec3 vec = new Vec3(x, y, z);
        double length = vec.length();
        return length < Constants.EPSILON ? Vec3.ZERO : vec.scale(1.0 / length);
    }
}
