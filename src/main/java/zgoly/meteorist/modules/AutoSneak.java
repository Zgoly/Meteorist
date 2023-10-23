package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import zgoly.meteorist.Meteorist;

public class AutoSneak extends Module {
    private final SafeWalk safeWalkModule = Modules.get().get(SafeWalk.class);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Render");

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
            .name("width")
            .description("Width of the box.")
            .defaultValue(0.25)
            .range(0.05, 1)
            .sliderRange(0.05, 1)
            .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("Height of the box.")
            .defaultValue(0.55)
            .range(0.05, 1)
            .sliderRange(0.05, 1)
            .build()
    );

    private final Setting<Boolean> safeWalk = sgGeneral.add(new BoolSetting.Builder()
            .name(safeWalkModule.name)
            .description("Also enable '" + safeWalkModule.name + "' module to avoid falling.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> showBox = sgColor.add(new BoolSetting.Builder()
            .name("show-box")
            .description("Show box.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sideColorOff = sgColor.add(new ColorSetting.Builder()
            .name("side-color-off")
            .description("The color of the sides of box when not sneaking.")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .visible(showBox::get)
            .build()
    );

    private final Setting<SettingColor> lineColorOff = sgColor.add(new ColorSetting.Builder()
            .name("line-color-off")
            .description("The color of the lines of box when not sneaking.")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .visible(showBox::get)
            .build()
    );

    private final Setting<SettingColor> sideColorOn = sgColor.add(new ColorSetting.Builder()
            .name("side-color-on")
            .description("The color of the sides of box when sneaking.")
            .defaultValue(new SettingColor(0, 255, 0, 40))
            .visible(showBox::get)
            .build()
    );

    private final Setting<SettingColor> lineColorOn = sgColor.add(new ColorSetting.Builder()
            .name("line-color-on")
            .description("The color of the lines of box when sneaking.")
            .defaultValue(new SettingColor(0, 255, 0, 100))
            .visible(showBox::get)
            .build()
    );

    public AutoSneak() {
        super(Meteorist.CATEGORY, "auto-sneak", "Automatically sneaks at block edge (idea by kokqi).");
    }

    private Box calcBox(Vec3d pos) {
        return new Box(
                pos.getX() + (width.get() / 2), pos.getY(), pos.getZ() + (width.get() / 2),
                pos.getX() - (width.get() / 2), pos.getY() - height.get(), pos.getZ() - (width.get() / 2)
        );
    }

    private boolean safeWalkWasEnabled = false;

    @Override
    public void onActivate() {
        safeWalkWasEnabled = safeWalkModule.isActive();
        if (!safeWalkWasEnabled && safeWalk.get()) {
            safeWalkModule.toggle();
        }
    }

    boolean sneaking = false;

    @Override
    public void onDeactivate() {
        sneaking = false;
        if (mc.player != null) mc.player.setSneaking(false);
        if (!safeWalkWasEnabled && safeWalk.get() && safeWalkModule.isActive()) {
            safeWalkModule.toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if ((mc.player.getAbilities().flying || mc.player.fallDistance > 0) && sneaking) {
            mc.options.sneakKey.setPressed(false);
            sneaking = false;
        }
        if (mc.player.isOnGround()) {
            Box box = calcBox(mc.player.getPos());
            Iterable<VoxelShape> iterable = mc.world.getBlockCollisions(mc.player, box);
            if (iterable.iterator().hasNext()) {
                iterable.forEach(blockBox -> {
                    BlockPos blockPos = BlockPos.ofFloored(blockBox.getMin(Direction.Axis.X), blockBox.getMin(Direction.Axis.Y), blockBox.getMin(Direction.Axis.Z));
                    if (mc.world.getBlockState(blockPos) == mc.player.getSteppingBlockState()) {
                        if (sneaking) {
                            mc.options.sneakKey.setPressed(false);
                            sneaking = false;
                        }
                    } else if (mc.world.getBlockState(blockPos).isReplaceable()) {
                        mc.options.sneakKey.setPressed(true);
                        sneaking = true;
                    }
                });
            } else {
                mc.options.sneakKey.setPressed(true);
                sneaking = true;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (showBox.get()) {
            if (mc.player == null) return;
            Box box = calcBox(mc.player.getPos());
            event.renderer.box(box, sneaking ? sideColorOn.get() : sideColorOff.get(), sneaking ? lineColorOn.get() : lineColorOff.get(), ShapeMode.Both, 0);
        }
    }
}
