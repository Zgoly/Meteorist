package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoatControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> autoForward = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-forward")
            .description("Automatically moves forward when you get into the boat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> stopWhenTurning = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-when-turning")
            .description("Do not move forward when turning.")
            .defaultValue(false)
            .visible(autoForward::get)
            .build()
    );
    private final Setting<Boolean> smartTurning = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-turning")
            .description("Automatically turns the boat.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> turnToYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-to-yaw")
            .description("Always try (if possible) to turn the boat back to the yaw axis. Not effective but may be useful in some cases.")
            .defaultValue(false)
            .visible(smartTurning::get)
            .build()
    );
    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
            .name("yaw")
            .description("The yaw to turn to.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .visible(() -> smartTurning.get() && turnToYaw.get())
            .build()
    );
    private final Setting<Boolean> autoYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-yaw")
            .description("Automatically capture boat's yaw upon boarding.")
            .defaultValue(false)
            .visible(() -> smartTurning.get() && turnToYaw.get())
            .build()
    );
    private final Setting<Double> accuracy = sgGeneral.add(new DoubleSetting.Builder()
            .name("accuracy")
            .description("Accuracy of turning to yaw. The higher the value, the lower the accuracy.")
            .defaultValue(10)
            .min(0)
            .visible(() -> smartTurning.get() && turnToYaw.get())
            .build()
    );
    private final Setting<Vector3d> leftCollisionSize = sgGeneral.add(new Vector3dSetting.Builder()
            .name("left-collision-size")
            .description("The size of the left collision.")
            .defaultValue(new Vector3d(1, 1, 1))
            .visible(smartTurning::get)
            .build()
    );
    private final Setting<Vector3d> leftCollisionOffset = sgGeneral.add(new Vector3dSetting.Builder()
            .name("left-collision-offset")
            .description("The offset of the left collision.")
            .defaultValue(new Vector3d(1, 0, -1))
            .visible(smartTurning::get)
            .build()
    );
    private final Setting<Vector3d> rightCollisionSize = sgGeneral.add(new Vector3dSetting.Builder()
            .name("right-collision-size")
            .description("The size of the right collision.")
            .defaultValue(new Vector3d(1, 1, 1))
            .visible(smartTurning::get)
            .build()
    );
    private final Setting<Vector3d> rightCollisionOffset = sgGeneral.add(new Vector3dSetting.Builder()
            .name("right-collision-offset")
            .description("The offset of the right collision.")
            .defaultValue(new Vector3d(1, 0, 1))
            .visible(smartTurning::get)
            .build()
    );
    private final Setting<State> leftCollisionAction = sgGeneral.add(new EnumSetting.Builder<State>()
            .name("left-collision-action")
            .description("The action to do when left collision is detected.")
            .defaultValue(State.TURNING_RIGHT)
            .build()
    );
    private final Setting<State> rightCollisionAction = sgGeneral.add(new EnumSetting.Builder<State>()
            .name("right-collision-action")
            .description("The action to do when right collision is detected.")
            .defaultValue(State.TURNING_LEFT)
            .build()
    );
    private final Setting<State> bothCollisionAction = sgGeneral.add(new EnumSetting.Builder<State>()
            .name("both-collision-action")
            .description("The action to do when both left and right collisions are detected.")
            .defaultValue(State.TURNING_LEFT)
            .build()
    );

    private final Setting<Boolean> renderSideColor1 = sgRender.add(new BoolSetting.Builder()
            .name("render-left-collision")
            .description("Renders the left collision.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor1 = sgRender.add(new ColorSetting.Builder()
            .name("left-collision-side-color")
            .description("The color of the sides of the collision being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 40))
            .visible(renderSideColor1::get)
            .build()
    );
    private final Setting<SettingColor> lineColor1 = sgRender.add(new ColorSetting.Builder()
            .name("left-collision-line-color")
            .description("The color of the lines of the collision being rendered.")
            .defaultValue(new SettingColor(0, 0, 255, 100))
            .visible(renderSideColor1::get)
            .build()
    );
    private final Setting<Boolean> renderSideColor2 = sgRender.add(new BoolSetting.Builder()
            .name("render-right-collision")
            .description("Renders the right collision.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor2 = sgRender.add(new ColorSetting.Builder()
            .name("right-collision-side-color")
            .description("The color of the sides of the collision being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .visible(renderSideColor2::get)
            .build()
    );
    private final Setting<SettingColor> lineColor2 = sgRender.add(new ColorSetting.Builder()
            .name("right-collision-line-color")
            .description("The color of the lines of the collision being rendered.")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .visible(renderSideColor2::get)
            .build()
    );

    private final List<KeyBinding> toRelease = new ArrayList<>();
    private State currentState = State.NOTHING;
    private boolean wasInBoat = false;

    public BoatControl() {
        super(Meteorist.CATEGORY, "boat-control", "Automatically controls the boat for you.");
    }

    @Override
    public String getInfoString() {
        return currentState.toString();
    }

    @Override
    public void onDeactivate() {
        // We don't need pressed keys
        wasInBoat = false;
        toRelease.clear();
        mc.options.forwardKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        currentState = State.NOTHING;

        BoatEntity boat = (BoatEntity) mc.player.getVehicle();
        if (boat == null || !boat.isInFluid() || boat.getControllingPassenger() != mc.player) {
            wasInBoat = false;
            return;
        }

        if (!wasInBoat && turnToYaw.get() && autoYaw.get()) {
            yaw.set((double) MathHelper.wrapDegrees(boat.getYaw()));
            wasInBoat = true;
        }

        if (autoForward.get()) {
            currentState = State.MOVING_FORWARD;
            hold(mc.options.forwardKey);
        }

        if (!smartTurning.get()) return;

        Map<State, KeyBinding> stateToKeyBinding = Map.of(
                State.TURNING_LEFT, mc.options.leftKey,
                State.TURNING_RIGHT, mc.options.rightKey
        );

        State leftAction = leftCollisionAction.get();
        State rightAction = rightCollisionAction.get();

        List<BlockPos> leftList = getBlockPos(getBox(boat, leftCollisionOffset.get(), leftCollisionSize.get()));
        List<BlockPos> rightList = getBlockPos(getBox(boat, rightCollisionOffset.get(), rightCollisionSize.get()));

        if (!leftList.isEmpty()) {
            currentState = leftAction;
            hold(stateToKeyBinding.get(leftAction));
        } else if (!rightList.isEmpty()) {
            currentState = rightAction;
            hold(stateToKeyBinding.get(rightAction));
        }

        if (!leftList.isEmpty() && !rightList.isEmpty()) currentState = bothCollisionAction.get();

        double yawDifference = MathHelper.wrapDegrees(yaw.get() - boat.getYaw());
        if ((currentState == State.MOVING_FORWARD || currentState == State.NOTHING) && turnToYaw.get() && Math.abs(yawDifference) > accuracy.get()) {
            currentState = yawDifference < 0 ? State.TURNING_LEFT : State.TURNING_RIGHT;
            hold(stateToKeyBinding.get(currentState));
        }

        if ((currentState == State.TURNING_LEFT || currentState == State.TURNING_RIGHT) && autoForward.get() && stopWhenTurning.get()) {
            mc.options.forwardKey.setPressed(false);
        }
    }


    private void hold(KeyBinding keyBinding) {
        toRelease.add(keyBinding);
        keyBinding.setPressed(true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!toRelease.isEmpty()) {
            toRelease.forEach(keyBinding -> keyBinding.setPressed(false));
            toRelease.clear();
        }
    }

    private Vec3d vectorToVec(Vector3d vector3d) {
        return new Vec3d(vector3d.x, vector3d.y, vector3d.z);
    }

    private Box getBox(BoatEntity boat, Vector3d collisionOffset, Vector3d collisionSize) {
        Vec3d offsetPos = boat.getPos().add(vectorToVec(collisionOffset).rotateY((float) -Math.toRadians(boat.getYaw() + 90)));
        Vec3d size = vectorToVec(collisionSize);
        return Box.of(offsetPos, size.x, size.y, size.z);
    }

    private List<BlockPos> getBlockPos(Box box) {
        return BlockPos.stream(box).map(BlockPos::new)
                .filter(blockPos -> !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty())
                .filter(blockPos -> !(mc.world.getBlockState(blockPos).getBlock() instanceof LilyPadBlock))
                .toList();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player.getVehicle() instanceof net.minecraft.entity.vehicle.BoatEntity boat) {
            if (boat.isInFluid() && boat.getControllingPassenger() == mc.player) {
                if (renderSideColor1.get()) {
                    event.renderer.box(getBox(boat, leftCollisionOffset.get(), leftCollisionSize.get()), sideColor1.get(), lineColor1.get(), ShapeMode.Both, 0);
                }
                if (renderSideColor2.get()) {
                    event.renderer.box(getBox(boat, rightCollisionOffset.get(), rightCollisionSize.get()), sideColor2.get(), lineColor2.get(), ShapeMode.Both, 0);
                }
            }
        }
    }

    private enum State {
        NOTHING,
        MOVING_FORWARD,
        TURNING_LEFT,
        TURNING_RIGHT
    }
}
