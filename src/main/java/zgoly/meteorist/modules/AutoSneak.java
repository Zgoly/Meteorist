package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import zgoly.meteorist.Meteorist;

import java.util.List;

public class AutoSneak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgRender = settings.createGroup("Render");

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
            .defaultValue(0.6)
            .range(0.05, 1)
            .sliderRange(0.05, 1)
            .build()
    );
    private final Setting<Double> playerPosPrediction = sgGeneral.add(new DoubleSetting.Builder()
            .name("player-pos-prediction")
            .description("Predict player position based on velocity to move box to it.")
            .defaultValue(1)
            .sliderRange(0, 5)
            .build()
    );

    private final Setting<SneakBlocksMode> sneakBlocksMode = sgFilter.add(new EnumSetting.Builder<SneakBlocksMode>()
            .name("sneak-blocks-mode")
            .description("Sneak blocks mode.")
            .defaultValue(SneakBlocksMode.Whitelist)
            .build()
    );
    private final Setting<List<Block>> sneakBlocksWhitelist = sgFilter.add(new BlockListSetting.Builder()
            .name("sneak-blocks-whitelist")
            .description("Sneak on blocks from list, but not others.")
            .visible(() -> sneakBlocksMode.get() == SneakBlocksMode.Whitelist)
            .build()
    );
    private final Setting<List<Block>> sneakBlocksBlacklist = sgFilter.add(new BlockListSetting.Builder()
            .name("sneak-blocks-blacklist")
            .description("Sneak on other blocks, but not from list.")
            .visible(() -> sneakBlocksMode.get() == SneakBlocksMode.Blacklist)
            .build()
    );
    private final Setting<IgnoreBlocksMode> ignoreBlocksMode = sgFilter.add(new EnumSetting.Builder<IgnoreBlocksMode>()
            .name("ignore-blocks-mode")
            .description("Ignore blocks mode.")
            .defaultValue(IgnoreBlocksMode.Whitelist)
            .build()
    );
    private final Setting<List<Block>> ignoreBlocksWhitelist = sgFilter.add(new BlockListSetting.Builder()
            .name("ignore-blocks-whitelist")
            .description("Ignore blocks from list, but not others.")
            .visible(() -> ignoreBlocksMode.get() == IgnoreBlocksMode.Whitelist)
            .build()
    );
    private final Setting<List<Block>> ignoreBlocksBlacklist = sgFilter.add(new BlockListSetting.Builder()
            .name("ignore-blocks-blacklist")
            .description("Ignore other blocks, but not from list.")
            .visible(() -> ignoreBlocksMode.get() == IgnoreBlocksMode.Blacklist)
            .build()
    );

    private final Setting<Boolean> showBox = sgRender.add(new BoolSetting.Builder()
            .name("show-box")
            .description("Show box.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColorOff = sgRender.add(new ColorSetting.Builder()
            .name("side-color-off")
            .description("The color of the sides of box when not sneaking.")
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .visible(showBox::get)
            .build()
    );
    private final Setting<SettingColor> lineColorOff = sgRender.add(new ColorSetting.Builder()
            .name("line-color-off")
            .description("The color of the lines of box when not sneaking.")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .visible(showBox::get)
            .build()
    );
    private final Setting<SettingColor> sideColorOn = sgRender.add(new ColorSetting.Builder()
            .name("side-color-on")
            .description("The color of the sides of box when sneaking.")
            .defaultValue(new SettingColor(0, 255, 0, 40))
            .visible(showBox::get)
            .build()
    );
    private final Setting<SettingColor> lineColorOn = sgRender.add(new ColorSetting.Builder()
            .name("line-color-on")
            .description("The color of the lines of box when sneaking.")
            .defaultValue(new SettingColor(0, 255, 0, 100))
            .visible(showBox::get)
            .build()
    );

    boolean sneaking = false;

    public AutoSneak() {
        super(Meteorist.CATEGORY, "auto-sneak", "Automatically sneaks at block edge (idea by kokqi).");
    }

    private AABB calcBox() {
        assert mc.player != null;
        Vec3 pos = mc.player.position().add(mc.player.getDeltaMovement().scale(playerPosPrediction.get()));
        pos = new Vec3(pos.x, mc.player.position().y, pos.z);
        return new AABB(
                pos.x() + (width.get() / 2), pos.y(), pos.z() + (width.get() / 2),
                pos.x() - (width.get() / 2), pos.y() - height.get(), pos.z() - (width.get() / 2)
        );
    }

    @Override
    public void onDeactivate() {
        sneaking = false;
        if (mc.player != null) mc.player.setShiftKeyDown(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if ((mc.player.getAbilities().flying || mc.player.fallDistance > 0) && sneaking) {
            mc.options.keyShift.setDown(false);
            sneaking = false;
        }
        if (mc.player.onGround()) {
            AABB box = calcBox();
            Iterable<VoxelShape> iterable = mc.level.getBlockCollisions(mc.player, box);
            if (iterable.iterator().hasNext()) {
                iterable.forEach(blockBox -> {
                    BlockPos blockPos = BlockPos.containing(blockBox.min(Direction.Axis.X), blockBox.min(Direction.Axis.Y), blockBox.min(Direction.Axis.Z));
                    if (mc.level.getBlockState(blockPos) == mc.player.getBlockStateOn()) {
                        if (sneaking) {
                            mc.options.keyShift.setDown(false);
                            sneaking = false;
                        }
                    } else if (mc.level.getBlockState(blockPos).canBeReplaced()) {
                        mc.options.keyShift.setDown(true);
                        sneaking = true;
                    }
                });
            } else {
                mc.options.keyShift.setDown(true);
                sneaking = true;
            }

            Block steppingBlock = mc.player.getBlockStateOn().getBlock();

            boolean shouldSneak = (sneakBlocksMode.get() == SneakBlocksMode.Whitelist)
                    ? sneakBlocksWhitelist.get().contains(steppingBlock)
                    : !sneakBlocksBlacklist.get().contains(steppingBlock);

            boolean shouldIgnore = (ignoreBlocksMode.get() == IgnoreBlocksMode.Whitelist)
                    ? ignoreBlocksWhitelist.get().contains(steppingBlock)
                    : !ignoreBlocksBlacklist.get().contains(steppingBlock);

            if (shouldSneak) {
                mc.options.keyShift.setDown(true);
                sneaking = true;
            }

            if (shouldIgnore) {
                mc.options.keyShift.setDown(false);
                sneaking = false;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (showBox.get()) {
            if (mc.player == null) return;
            event.renderer.box(calcBox(), sneaking ? sideColorOn.get() : sideColorOff.get(), sneaking ? lineColorOn.get() : lineColorOff.get(), ShapeMode.Both, 0);
        }
    }

    public enum SneakBlocksMode {
        Whitelist,
        Blacklist
    }

    public enum IgnoreBlocksMode {
        Whitelist,
        Blacklist
    }
}
