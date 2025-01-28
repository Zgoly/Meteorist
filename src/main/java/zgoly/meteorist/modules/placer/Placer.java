package zgoly.meteorist.modules.placer;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import zgoly.meteorist.gui.screens.PlacerScreen;
import zgoly.meteorist.gui.widgets.WVisibilityCheckbox;
import zgoly.meteorist.utils.MeteoristUtils;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static zgoly.meteorist.Meteorist.*;

public class Placer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgPause = settings.createGroup("Pause");

    private final Setting<Boolean> checkConditions = sgGeneral.add(new BoolSetting.Builder()
            .name("check-conditions")
            .description("Check conditions for placing blocks, such as whether block can be placed and whether there is entity that blocking block from being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotateHead = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate-head")
            .description("Rotate head when placing a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> limitRange = sgGeneral.add(new BoolSetting.Builder()
            .name("limit-range")
            .description("Limit block placement range.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> maxRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-range")
            .description("Max range to place blocks.")
            .defaultValue(4)
            .visible(limitRange::get)
            .sliderRange(1, 6)
            .build()
    );

    private final Setting<Boolean> renderEachBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("render-each-block")
            .description("Render each block in placer area.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> useDelay = sgTiming.add(new BoolSetting.Builder()
            .name("use-delay")
            .description("Use delay between placing blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgTiming.add(new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("Maximum blocks to place per tick.")
            .defaultValue(1)
            .visible(useDelay::get)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<Integer> delay = sgTiming.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after placing block(s) in ticks (20 ticks = 1 sec).")
            .defaultValue(1)
            .visible(useDelay::get)
            .range(1, 120)
            .sliderRange(1, 40)
            .build()
    );

    private final Setting<Boolean> pauseOnAutoEat = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-auto-eat")
            .description("Pause when Auto Eat is active and eats food.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseOnAutoGap = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-auto-gap")
            .description("Pause when Auto Gap is active and eats gaps.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseOnKillAura = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-kill-aura")
            .description("Pause when Kill Aura is active and attacks target entities.")
            .defaultValue(true)
            .build()
    );
    List<BasePlacer> placers = new ArrayList<>();

    private int timer;
    private boolean work;

    public Placer() {
        super(CATEGORY, "placer", "Places blocks in range.");
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BasePlacer placer : placers) {
            NbtCompound mTag = new NbtCompound();
            mTag.put("placer", placer.toTag());

            list.add(mTag);
        }

        tag.put("placers", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        placers.clear();
        NbtList list = tag.getList("placers", NbtElement.COMPOUND_TYPE);
        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;

            BasePlacer placer = new BasePlacer();
            NbtCompound placerTag = tagI.getCompound("placer");

            if (placerTag != null) placer.fromTag(placerTag);

            placer.settings.registerColorSettings(null);
            placers.add(placer);
        }

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    public void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        WTable table = list.add(theme.table()).expandX().widget();

        for (BasePlacer placer : placers) {
            WTextBox name = table.add(theme.textBox(placer.name.get(), "Name")).expandX().widget();
            name.tooltip = placer.name.description;
            name.actionOnUnfocused = () -> placer.name.set(name.get());

            WVisibilityCheckbox visible = new WVisibilityCheckbox(placer.visible.get());
            table.add(visible).widget();
            visible.tooltip = placer.visible.description;
            visible.action = () -> placer.visible.set(visible.checked);

            WCheckbox active = table.add(theme.checkbox(placer.active.get())).widget();
            active.tooltip = placer.active.description;
            active.action = () -> placer.active.set(active.checked);

            WButton edit = table.add(theme.button("Edit")).expandX().widget();
            edit.tooltip = "Edit the placer.";
            edit.action = () -> mc.setScreen(new PlacerScreen(theme, placer));

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (placers.size() > 1) {
                int index = placers.indexOf(placer);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move placer up.";
                    moveUp.action = () -> {
                        placers.remove(index);
                        placers.add(index - 1, placer);
                        fillWidget(theme, list);
                    };
                }

                if (index < placers.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move placer down.";
                    moveDown.action = () -> {
                        placers.remove(index);
                        placers.add(index + 1, placer);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = table.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate placer.";
            copy.action = () -> {
                BasePlacer newPlacer = placer.copy();
                newPlacer.settings.registerColorSettings(null);
                placers.add(placers.indexOf(placer), newPlacer);
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove placer.";
            remove.action = () -> {
                placer.settings.unregisterColorSettings();
                placers.remove(placer);
                fillWidget(theme, list);
            };

            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();
        WButton create = controls.add(theme.button("New Placer")).expandX().widget();
        create.action = () -> {
            BasePlacer placer = new BasePlacer();
            placer.settings.registerColorSettings(null);
            placer.name.set("Placer #" + (placers.size() + 1));
            placers.add(placer);
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Placers")).expandX().widget();
        removeAll.action = () -> {
            placers.forEach(placer -> placer.settings.unregisterColorSettings());
            placers.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    private Pair<BlockPos, BlockPos> getPlacePos(BasePlacer placer) {
        BlockPos finalPos1 = rotateBlockPos(placer.cornerPos1.get(), placer.rotateY1.get());

        if (!placer.rotateX1.get())
            finalPos1 = new BlockPos(placer.cornerPos1.get().getX(), finalPos1.getY(), finalPos1.getZ());
        if (!placer.rotateZ1.get())
            finalPos1 = new BlockPos(finalPos1.getX(), finalPos1.getY(), placer.cornerPos1.get().getZ());

        finalPos1 = finalPos1.add(mc.player.getBlockPos());

        if (placer.anchorX1.get())
            finalPos1 = new BlockPos(placer.cornerPos1.get().getX(), finalPos1.getY(), finalPos1.getZ());
        if (placer.anchorY1.get())
            finalPos1 = new BlockPos(finalPos1.getX(), placer.cornerPos1.get().getY(), finalPos1.getZ());
        if (placer.anchorZ1.get())
            finalPos1 = new BlockPos(finalPos1.getX(), finalPos1.getY(), placer.cornerPos1.get().getZ());

        finalPos1 = finalPos1.add(placer.cornerAnchorPos1.get());

        BlockPos finalPos2 = rotateBlockPos(placer.cornerPos2.get(), placer.rotateY2.get());

        if (!placer.rotateX2.get())
            finalPos2 = new BlockPos(placer.cornerPos2.get().getX(), finalPos2.getY(), finalPos2.getZ());
        if (!placer.rotateZ2.get())
            finalPos2 = new BlockPos(finalPos2.getX(), finalPos2.getY(), placer.cornerPos2.get().getZ());

        finalPos2 = finalPos2.add(mc.player.getBlockPos());

        if (placer.anchorX2.get())
            finalPos2 = new BlockPos(placer.cornerPos2.get().getX(), finalPos2.getY(), finalPos2.getZ());
        if (placer.anchorY2.get())
            finalPos2 = new BlockPos(finalPos2.getX(), placer.cornerPos2.get().getY(), finalPos2.getZ());
        if (placer.anchorZ2.get())
            finalPos2 = new BlockPos(finalPos2.getX(), finalPos2.getY(), placer.cornerPos2.get().getZ());

        finalPos2 = finalPos2.add(placer.cornerAnchorPos2.get());

        return new Pair<>(finalPos1, finalPos2);
    }

    private Stream<BlockPos> getPlaceStream(Pair<BlockPos, BlockPos> finalPos) {
        return BlockPos.stream(finalPos.getLeft(), finalPos.getRight());
    }

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pauseOnAutoEat.get() && Modules.get().get(AutoEat.class).eating) return;
        if (pauseOnAutoGap.get() && Modules.get().get(AutoGap.class).isEating()) return;
        if (pauseOnKillAura.get() && Modules.get().get(KillAura.class).attacking) return;

        if (work) {
            int loopCount = 0;
            placersLoop:
            for (BasePlacer placer : placers) {
                if (placer.active.get()) {
                    // Block pos sorting and other stuff
                    List<BlockPos> blockPosList = new ArrayList<>();
                    Pair<BlockPos, BlockPos> finalPos = getPlacePos(placer);
                    BlockPos.stream(finalPos.getLeft(), finalPos.getRight()).filter(blockPos ->
                            (!limitRange.get() || !(blockPos.toCenterPos().distanceTo(mc.player.getEyePos()) > maxRange.get()))
                    ).forEach(blockPos -> blockPosList.add(new BlockPos(blockPos)));
                    blockPosList.sort(Comparator.comparingDouble(blockPos -> blockPos.toCenterPos().distanceTo(mc.player.getEyePos())));

                    for (BlockPos blockPos : blockPosList) {
                        if (useDelay.get() && loopCount >= maxBlocksPerTick.get()) {
                            break placersLoop;
                        }

                        if (checkConditions.get() && (!BlockUtils.canPlace(blockPos) || (MeteoristUtils.isCollidesEntity(blockPos))))
                            continue;

                        Random random = new Random();
                        List<Block> hotbarBlocks = new ArrayList<>();
                        for (Block block : placer.blocks.get()) {
                            FindItemResult itemResult = InvUtils.findInHotbar(block.asItem());
                            if (itemResult.found()) hotbarBlocks.add(block);
                        }
                        if (!hotbarBlocks.isEmpty()) {
                            Block blockToPlace = hotbarBlocks.get(random.nextInt(hotbarBlocks.size()));
                            FindItemResult itemResult = InvUtils.findInHotbar(blockToPlace.asItem());
                            BlockUtils.place(blockPos, itemResult, rotateHead.get(), 0);
                        }

                        if (useDelay.get()) loopCount++;
                    }
                }
            }
            if (useDelay.get()) work = false;
        }
        if (useDelay.get()) {
            if (timer + 1 >= delay.get()) {
                timer = 0;
                work = true;
            } else {
                timer++;
            }
        }
    }

    // Probably the laziest way to do this
    public BlockPos rotateBlockPos(BlockPos pos, boolean rotateY) {
        if (rotateY) {
            float pitch = mc.player.getPitch();
            if (pitch > 45) {
                pos = new BlockPos(pos.getY(), -pos.getX(), pos.getZ());
            } else if (pitch < -45) {
                pos = new BlockPos(-pos.getY(), pos.getX(), pos.getZ());
            }
        }

        Direction direction = Direction.fromHorizontalDegrees(mc.player.getYaw());
        return switch (direction) {
            case NORTH -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case SOUTH -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case WEST -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            default -> pos;
        };
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BasePlacer placer : placers) {
            if (placer.visible.get()) {
                if (renderEachBlock.get()) {
                    getPlaceStream(getPlacePos(placer)).forEach(blockPos -> event.renderer.box(blockPos, placer.sideColor.get(), placer.lineColor.get(), ShapeMode.Both, 0));
                } else {
                    Pair<BlockPos, BlockPos> finalPos = getPlacePos(placer);
                    event.renderer.box(Box.enclosing(finalPos.getLeft(), finalPos.getRight()), placer.sideColor.get(), placer.lineColor.get(), ShapeMode.Both, 0);
                }
            }
        }
    }
}