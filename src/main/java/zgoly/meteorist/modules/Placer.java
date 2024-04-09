package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import zgoly.meteorist.utils.MeteoristUtils;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static zgoly.meteorist.Meteorist.*;

public class Placer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");

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

    private final Setting<Boolean> renderEachBlock = sgGeneral.add(new BoolSetting.Builder()
            .name("render-each-block")
            .description("Render each block in placer area.")
            .defaultValue(true)
            .build()
    );

    public Placer() {
        super(CATEGORY, "placer", "Places blocks in range.");

        // Rainbow
        RainbowColors.register(this::onTickRainbow);
    }

    /*
    BIG thanks for Maxsupermanhd's Villager Roller. Finally figured out how that work
    https://github.com/maxsupermanhd/meteor-villager-roller
    */

    List<PlacerData> placers = new ArrayList<>();

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList nbtList = new NbtList();
        for (PlacerData placerData : placers) {
            nbtList.add(placerData.toTag());
        }
        tag.put("placer", nbtList);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        NbtList nbtList = tag.getList("placer", NbtElement.COMPOUND_TYPE);
        placers.clear();
        for (NbtElement nbtElement : nbtList) {
            if (nbtElement.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                continue;
            }
            placers.add(new PlacerData().fromTag((NbtCompound) nbtElement));
        }
        return super.fromTag(tag);
    }

    public static class PlacerData implements ISerializable<PlacerData> {
        public String name = "Placer";
        public BlockPos cornerPos1 = new BlockPos(0, 0, 0);
        public BlockPos cornerPos2 = new BlockPos(0, 0, 0);
        public BlockPos cornerAnchorPos1 = new BlockPos(0, 0, 0);
        public BlockPos cornerAnchorPos2 = new BlockPos(0, 0, 0);
        public boolean x1Rotate = true;
        public boolean y1Rotate = false;
        public boolean z1Rotate = true;
        public boolean x2Rotate = true;
        public boolean y2Rotate = false;
        public boolean z2Rotate = true;
        public boolean x1Anchor = false;
        public boolean y1Anchor = false;
        public boolean z1Anchor = false;
        public boolean x2Anchor = false;
        public boolean y2Anchor = false;
        public boolean z2Anchor = false;
        public List<Block> blocks = new ArrayList<>(Collections.singletonList(Blocks.COBBLESTONE));
        public SettingColor sideColor = new SettingColor(0, 255, 0, 50);
        public SettingColor lineColor = new SettingColor(0, 255, 0, 255);
        public boolean visible = true;
        public boolean active = false;

        public PlacerData() {}

        public PlacerData(String name, BlockPos cornerPos1, BlockPos cornerPos2, BlockPos cornerAnchorPos1, BlockPos cornerAnchorPos2, boolean x1Rotate, boolean y1Rotate, boolean z1Rotate, boolean x2Rotate, boolean y2Rotate, boolean z2Rotate, boolean x1Anchor, boolean y1Anchor, boolean z1Anchor, boolean x2Anchor, boolean y2Anchor, boolean z2Anchor, List<Block> blocks, SettingColor sideColor, SettingColor lineColor, boolean visible, boolean active) {
            this.name = name;
            this.cornerPos1 = cornerPos1;
            this.cornerPos2 = cornerPos2;
            this.cornerAnchorPos1 = cornerAnchorPos1;
            this.cornerAnchorPos2 = cornerAnchorPos2;
            this.x1Rotate = x1Rotate;
            this.y1Rotate = y1Rotate;
            this.z1Rotate = z1Rotate;
            this.x2Rotate = x2Rotate;
            this.y2Rotate = y2Rotate;
            this.z2Rotate = z2Rotate;
            this.x1Anchor = x1Anchor;
            this.y1Anchor = y1Anchor;
            this.z1Anchor = z1Anchor;
            this.x2Anchor = x2Anchor;
            this.y2Anchor = y2Anchor;
            this.z2Anchor = z2Anchor;
            this.blocks = blocks;
            this.sideColor = sideColor;
            this.lineColor = lineColor;
            this.visible = visible;
            this.active = active;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("name", this.name);

            tag.putIntArray("cornerPos1", new int[]{this.cornerPos1.getX(), this.cornerPos1.getY(), this.cornerPos1.getZ()});
            tag.putIntArray("cornerPos2", new int[]{this.cornerPos2.getX(), this.cornerPos2.getY(), this.cornerPos2.getZ()});
            tag.putIntArray("cornerAnchorPos1", new int[]{this.cornerAnchorPos1.getX(), this.cornerAnchorPos1.getY(), this.cornerAnchorPos1.getZ()});
            tag.putIntArray("cornerAnchorPos2", new int[]{this.cornerAnchorPos2.getX(), this.cornerAnchorPos2.getY(), this.cornerAnchorPos2.getZ()});

            tag.putBoolean("x1Rotate", this.x1Rotate);
            tag.putBoolean("y1Rotate", this.y1Rotate);
            tag.putBoolean("z1Rotate", this.z1Rotate);
            tag.putBoolean("x2Rotate", this.x2Rotate);
            tag.putBoolean("y2Rotate", this.y2Rotate);
            tag.putBoolean("z2Rotate", this.z2Rotate);

            tag.putBoolean("x1Anchor", this.x1Anchor);
            tag.putBoolean("y1Anchor", this.y1Anchor);
            tag.putBoolean("z1Anchor", this.z1Anchor);
            tag.putBoolean("x2Anchor", this.x2Anchor);
            tag.putBoolean("y2Anchor", this.y2Anchor);
            tag.putBoolean("z2Anchor", this.z2Anchor);

            NbtList blocksList = new NbtList();
            for (Block block : this.blocks) {
                blocksList.add(NbtString.of(String.valueOf(Registries.BLOCK.getId(block))));
            }
            tag.put("blocks", blocksList);
            tag.put("sideColor", this.sideColor.toTag());
            tag.put("lineColor", this.lineColor.toTag());
            tag.putBoolean("visible", this.visible);
            tag.putBoolean("active", this.active);
            return tag;
        }

        @Override
        public PlacerData fromTag(NbtCompound tag) {
            this.name = tag.getString("name");

            int[] cornerPos1 = tag.getIntArray("cornerPos1");
            this.cornerPos1 = new BlockPos(cornerPos1[0], cornerPos1[1], cornerPos1[2]);

            int[] cornerPos2 = tag.getIntArray("cornerPos2");
            this.cornerPos2 = new BlockPos(cornerPos2[0], cornerPos2[1], cornerPos2[2]);

            int[] cornerAnchorPos1 = tag.getIntArray("cornerAnchorPos1");
            this.cornerAnchorPos1 = new BlockPos(cornerAnchorPos1[0], cornerAnchorPos1[1], cornerAnchorPos1[2]);

            int[] cornerAnchorPos2 = tag.getIntArray("cornerAnchorPos2");
            this.cornerAnchorPos2 = new BlockPos(cornerAnchorPos2[0], cornerAnchorPos2[1], cornerAnchorPos2[2]);

            this.x1Rotate = tag.getBoolean("x1Rotate");
            this.y1Rotate = tag.getBoolean("y1Rotate");
            this.z1Rotate = tag.getBoolean("z1Rotate");
            this.x2Rotate = tag.getBoolean("x2Rotate");
            this.y2Rotate = tag.getBoolean("y2Rotate");
            this.z2Rotate = tag.getBoolean("z2Rotate");

            this.x1Anchor = tag.getBoolean("x1Anchor");
            this.y1Anchor = tag.getBoolean("y1Anchor");
            this.z1Anchor = tag.getBoolean("z1Anchor");
            this.x2Anchor = tag.getBoolean("x2Anchor");
            this.y2Anchor = tag.getBoolean("y2Anchor");
            this.z2Anchor = tag.getBoolean("z2Anchor");

            List<Block> blocksList = new ArrayList<>();
            NbtList blocksNbtList = tag.getList("blocks", NbtElement.STRING_TYPE);
            for (int i = 0; i < blocksNbtList.size(); i++) {
                String string = blocksNbtList.getString(i);
                Block block = Registries.BLOCK.get(Identifier.tryParse(string));
                blocksList.add(block);
            }
            this.blocks = blocksList;
            this.sideColor = sideColor.fromTag(tag.getCompound("sideColor"));
            this.lineColor = lineColor.fromTag(tag.getCompound("lineColor"));
            this.visible = tag.getBoolean("visible");
            this.active = tag.getBoolean("active");
            return this;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    public void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection placersSection = list.add(theme.section("Placers")).expandX().widget();
        WHorizontalList placersList = placersSection.add(theme.horizontalList()).expandX().widget();
        WTable table = placersList.add(theme.table()).expandX().widget();

        WLabel nameLabel = table.add(theme.label("Name")).expandX().widget();
        nameLabel.tooltip = "Name of the placer.";

        table.add(theme.label("")).expandX().widget();

        WLabel visibleLabel = table.add(theme.label("Visible")).expandX().widget();
        visibleLabel.tooltip = "Is the placer visible.";

        WLabel activeLabel = table.add(theme.label("Active")).expandX().widget();
        activeLabel.tooltip = "Is the placer active.";

        table.add(theme.label("")).expandX().widget();
        table.add(theme.label("")).expandX().widget();
        table.add(theme.label("")).expandX().widget();
        table.row();

        for (PlacerData placer : placers) {
            int padding = 16;

            WTextBox name = table.add(theme.textBox(placer.name)).expandX().widget();
            name.tooltip = nameLabel.tooltip;
            name.actionOnUnfocused = () -> placer.name = name.get();

            WButton edit = table.add(theme.button("Edit")).padRight(padding).widget();
            edit.tooltip = "Edit the placer.";
            edit.action = () -> mc.setScreen(new PlacerScreen(theme, placer, () -> {
                list.clear();
                fillWidget(theme, list);
            }));

            WCheckbox visible = table.add(theme.checkbox(placer.visible)).padRight(padding).widget();
            visible.tooltip = visibleLabel.tooltip;
            visible.action = () -> placer.visible = visible.checked;

            WCheckbox active = table.add(theme.checkbox(placer.active)).padRight(padding).widget();
            active.tooltip = activeLabel.tooltip;
            active.action = () -> placer.active = active.checked;

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (placers.size() > 1) {
                int index = placers.indexOf(placer);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move placer up.";
                    moveUp.action = () -> {
                        placers.remove(index);
                        placers.add(index - 1, placer);
                        list.clear();
                        fillWidget(theme, list);
                    };
                }

                if (index < placers.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move placer down.";
                    moveDown.action = () -> {
                        placers.remove(index);
                        placers.add(index + 1, placer);
                        list.clear();
                        fillWidget(theme, list);
                    };
                }

                // Goofy workaround for padding
                moveContainer.add(theme.horizontalList()).padRight(padding).widget();
            }

            WButton copy = table.add(theme.button(COPY)).padRight(padding).widget();
            copy.tooltip = "Copy placer.";
            copy.action = () -> {
                list.clear();
                placers.add(placers.indexOf(placer), new PlacerData("Copy of " + placer.name, placer.cornerPos1, placer.cornerPos2, placer.cornerAnchorPos1, placer.cornerAnchorPos2, placer.x1Rotate, placer.y1Rotate, placer.z1Rotate, placer.x2Rotate, placer.y2Rotate, placer.z2Rotate, placer.x1Anchor, placer.y1Anchor, placer.z1Anchor, placer.x2Anchor, placer.y2Anchor, placer.z2Anchor, placer.blocks, placer.sideColor, placer.lineColor, placer.visible, placer.active));
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove placer.";
            remove.action = () -> {
                list.clear();
                placers.remove(placer);
                fillWidget(theme, list);
            };
            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();
        WButton create = controls.add(theme.button("Add new placer")).expandX().widget();
        create.action = () -> {
            PlacerData placer = new PlacerData();
            placer.name = "Placer #" + (placers.size() + 1);
            placers.add(placer);
            list.clear();
            fillWidget(theme, list);
        };

        WSection configSection = list.add(theme.section("Config")).expandX().widget();
        WTable control = configSection.add(theme.table()).expandX().widget();

        WButton save = control.add(theme.button("Save to file")).expandX().widget();
        save.action = () -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterBuffer = stack.mallocPointer(1);
                filterBuffer.put(stack.UTF8("*.nbt"));
                filterBuffer.flip();
                String result = TinyFileDialogs.tinyfd_saveFileDialog("Save to file", "placer_config.nbt", filterBuffer, "NBT File (*.nbt)");
                if (result != null) {
                    try {
                        NbtCompound data = toTag();
                        FileOutputStream outputStream = new FileOutputStream(result);
                        NbtIo.writeCompressed(data, outputStream);
                        outputStream.close();
                    } catch (IOException e) {
                        mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Failed to save config"), Text.of(e.getMessage())));
                        e.printStackTrace();
                    }
                }
            }
        };

        WButton load = control.add(theme.button("Load from file")).expandX().widget();
        load.action = () -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterBuffer = stack.mallocPointer(1);
                filterBuffer.put(stack.UTF8("*.nbt"));
                filterBuffer.flip();
                String result = TinyFileDialogs.tinyfd_openFileDialog("Load from file", null, filterBuffer, "NBT File (*.nbt)", false);
                if (result != null) {
                    try {
                        FileInputStream in = new FileInputStream(result);
                        NbtCompound data = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
                        fromTag(data);
                        list.clear();
                        fillWidget(theme, list);
                    } catch (Exception e) {
                        String text = e.getMessage();
                        if (e instanceof EOFException) text = "Config is empty";
                        mc.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Failed to load config"), Text.of(text)));
                        e.printStackTrace();
                    }
                }
            }
        };
        control.row();
    }

    private Pair<BlockPos, BlockPos> getPlacePos(PlacerData placer) {
        BlockPos finalPos1 = rotateBlockPos(placer.cornerPos1, placer.y1Rotate);

        if (!placer.x1Rotate) finalPos1 = new BlockPos(placer.cornerPos1.getX(), finalPos1.getY(), finalPos1.getZ());
        if (!placer.z1Rotate) finalPos1 = new BlockPos(finalPos1.getX(), finalPos1.getY(), placer.cornerPos1.getZ());

        finalPos1 = finalPos1.add(mc.player.getBlockPos());

        if (placer.x1Anchor) finalPos1 = new BlockPos(placer.cornerPos1.getX(), finalPos1.getY(), finalPos1.getZ());
        if (placer.y1Anchor) finalPos1 = new BlockPos(finalPos1.getX(), placer.cornerPos1.getY(), finalPos1.getZ());
        if (placer.z1Anchor) finalPos1 = new BlockPos(finalPos1.getX(), finalPos1.getY(), placer.cornerPos1.getZ());

        finalPos1 = finalPos1.add(placer.cornerAnchorPos1);

        BlockPos finalPos2 = rotateBlockPos(placer.cornerPos2, placer.y2Rotate);

        if (!placer.x2Rotate) finalPos2 = new BlockPos(placer.cornerPos2.getX(), finalPos2.getY(), finalPos2.getZ());
        if (!placer.z2Rotate) finalPos2 = new BlockPos(finalPos2.getX(), finalPos2.getY(), placer.cornerPos2.getZ());

        finalPos2 = finalPos2.add(mc.player.getBlockPos());

        if (placer.x2Anchor) finalPos2 = new BlockPos(placer.cornerPos2.getX(), finalPos2.getY(), finalPos2.getZ());
        if (placer.y2Anchor) finalPos2 = new BlockPos(finalPos2.getX(), placer.cornerPos2.getY(), finalPos2.getZ());
        if (placer.z2Anchor) finalPos2 = new BlockPos(finalPos2.getX(), finalPos2.getY(), placer.cornerPos2.getZ());

        finalPos2 = finalPos2.add(placer.cornerAnchorPos2);

        return new Pair<>(finalPos1, finalPos2);
    }

    private Stream<BlockPos> getPlaceStream(Pair<BlockPos, BlockPos> finalPos) {
        return BlockPos.stream(finalPos.getLeft(), finalPos.getRight());
    }

    private int timer;
    private boolean work;

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
    }

    public void onTickRainbow() {
        for (PlacerData p : placers) {
            p.lineColor.update();
            p.sideColor.update();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (work) {
            int loopCount = 0;
            placersLoop:
            for (PlacerData p : placers) {
                if (p.active) {

                    // Block pos sorting and other stuff
                    List<BlockPos> blockPosList = new ArrayList<>();
                    Pair<BlockPos, BlockPos> finalPos = getPlacePos(p);
                    BlockPos.stream(finalPos.getLeft(), finalPos.getRight()).filter(blockPos ->
                            (!limitRange.get() || !(blockPos.toCenterPos().distanceTo(mc.player.getEyePos()) > maxRange.get()))
                    ).forEach(blockPos -> blockPosList.add(new BlockPos(blockPos)));
                    blockPosList.sort(Comparator.comparingDouble(blockPos -> blockPos.toCenterPos().distanceTo(mc.player.getEyePos())));

                    for (BlockPos blockPos : blockPosList) {
                        if (useDelay.get() && loopCount >= maxBlocksPerTick.get()) {
                            break placersLoop;
                        }

                        if (!BlockUtils.canPlace(blockPos) || MeteoristUtils.isCollidesEntity(blockPos)) continue;

                        Random random = new Random();
                        List<Block> hotbarBlocks = new ArrayList<>();
                        for (Block block : p.blocks) {
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
        Direction direction = Direction.fromRotation(mc.player.getYaw());
        return switch (direction) {
            case NORTH -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case SOUTH -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case WEST -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            default -> pos;
        };
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (PlacerData p : placers) {
            if (p.visible) {
                if (renderEachBlock.get()) {
                    getPlaceStream(getPlacePos(p)).forEach(blockPos -> event.renderer.box(blockPos, p.sideColor, p.lineColor, ShapeMode.Both, 0));
                } else {
                    Pair<BlockPos, BlockPos> finalPos = getPlacePos(p);
                    event.renderer.box(Box.enclosing(finalPos.getLeft(), finalPos.getRight()), p.sideColor, p.lineColor, ShapeMode.Both, 0);
                }
            }
        }
    }
}

class PlacerScreen extends WindowScreen {
    private final Placer.PlacerData p;
    // reloadPlacers allows to reload placers list of the module, so if the name, visible and active state are changed, we can handle it correctly
    private final Runnable reloadPlacers;

    public PlacerScreen(GuiTheme theme, Placer.PlacerData p, Runnable reloadPlacers) {
        super(theme, p.name);
        this.p = p;
        this.reloadPlacers = reloadPlacers;
    }

    @Override
    public void initWidgets() {
        Settings settings = new Settings();

        SettingGroup sgName = settings.createGroup("Name");
        SettingGroup sgCorner1 = settings.createGroup("Corner #1");
        SettingGroup sgCorner2 = settings.createGroup("Corner #2");
        SettingGroup sgCornerAnchorPos1 = settings.createGroup("Corner Anchor Position #1");
        SettingGroup sgCornerAnchorPos2 = settings.createGroup("Corner Anchor Position #2");
        SettingGroup sgRotation = settings.createGroup("Rotation");
        SettingGroup sgAnchor = settings.createGroup("Anchor");
        SettingGroup sgBlocks = settings.createGroup("Blocks");
        SettingGroup sgColor = settings.createGroup("Color");
        SettingGroup sgControls = settings.createGroup("Controls");

        Placer.PlacerData defaults = new Placer.PlacerData();
        sgName.add(new StringSetting.Builder()
                .name("name")
                .description("The name of the placer.")
                .defaultValue(defaults.name)
                .onChanged(value -> {
                    p.name = value;
                    reloadPlacers.run();
                })
                .onModuleActivated(setting -> setting.set(p.name))
                .build()
        ).set(p.name);

        // Better to replace all IntSetting with Vector3dSetting, but looks like onChanged doesn't work with Vector3dSetting
        sgCorner1.add(new IntSetting.Builder()
                .name("corner-x1")
                .description("The first X coordinate of the placer.")
                .defaultValue(defaults.cornerPos1.getX())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos1 = new BlockPos(value, p.cornerPos1.getY(), p.cornerPos1.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerPos1.getX()))
                .build()
        ).set(p.cornerPos1.getX());
        sgCorner1.add(new IntSetting.Builder()
                .name("corner-y1")
                .description("The first Y coordinate of the placer.")
                .defaultValue(defaults.cornerPos1.getY())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos1 = new BlockPos(p.cornerPos1.getX(), value, p.cornerPos1.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerPos1.getY()))
                .build()
        ).set(p.cornerPos1.getY());
        sgCorner1.add(new IntSetting.Builder()
                .name("corner-z1")
                .description("The first Z coordinate of the placer.")
                .defaultValue(defaults.cornerPos1.getZ())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos1 = new BlockPos(p.cornerPos1.getX(), p.cornerPos1.getY(), value))
                .onModuleActivated(setting -> setting.set(p.cornerPos1.getZ()))
                .build()
        ).set(p.cornerPos1.getZ());

        sgCorner2.add(new IntSetting.Builder()
                .name("corner-x2")
                .description("The second X coordinate of the placer.")
                .defaultValue(defaults.cornerPos2.getX())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos2 = new BlockPos(value, p.cornerPos2.getY(), p.cornerPos2.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerPos2.getX()))
                .build()
        ).set(p.cornerPos2.getX());
        sgCorner2.add(new IntSetting.Builder()
                .name("corner-y2")
                .description("The second Y coordinate of the placer.")
                .defaultValue(defaults.cornerPos2.getY())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos2 = new BlockPos(p.cornerPos2.getX(), value, p.cornerPos2.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerPos2.getY()))
                .build()
        ).set(p.cornerPos2.getY());
        sgCorner2.add(new IntSetting.Builder()
                .name("corner-z2")
                .description("The second Z coordinate of the placer.")
                .defaultValue(defaults.cornerPos2.getZ())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerPos2 = new BlockPos(p.cornerPos2.getX(), p.cornerPos2.getY(), value))
                .onModuleActivated(setting -> setting.set(p.cornerPos2.getZ()))
                .build()
        ).set(p.cornerPos2.getZ());

        sgCornerAnchorPos1.add(new IntSetting.Builder()
                .name("corner-anchor-pos-x1")
                .description("The X coordinate of the first corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos1.getX())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos1 = new BlockPos(value, p.cornerAnchorPos1.getY(), p.cornerAnchorPos1.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos1.getX()))
                .build()
        ).set(p.cornerAnchorPos1.getX());
        sgCornerAnchorPos1.add(new IntSetting.Builder()
                .name("corner-anchor-pos-y1")
                .description("The Y coordinate of the first corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos1.getY())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos1 = new BlockPos(p.cornerAnchorPos1.getX(), value, p.cornerAnchorPos1.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos1.getY()))
                .build()
        ).set(p.cornerAnchorPos1.getY());
        sgCornerAnchorPos1.add(new IntSetting.Builder()
                .name("corner-anchor-pos-z1")
                .description("The Z coordinate of the first corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos1.getZ())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos1 = new BlockPos(p.cornerAnchorPos1.getX(), p.cornerAnchorPos1.getY(), value))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos1.getZ()))
                .build()
        ).set(p.cornerAnchorPos1.getZ());

        sgCornerAnchorPos2.add(new IntSetting.Builder()
                .name("corner-anchor-pos-x2")
                .description("The X coordinate of the second corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos2.getX())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos2 = new BlockPos(value, p.cornerAnchorPos2.getY(), p.cornerAnchorPos2.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos2.getX()))
                .build()
        ).set(p.cornerAnchorPos2.getX());
        sgCornerAnchorPos2.add(new IntSetting.Builder()
                .name("corner-anchor-pos-y2")
                .description("The Y coordinate of the second corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos2.getY())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos2 = new BlockPos(p.cornerAnchorPos2.getX(), value, p.cornerAnchorPos2.getZ()))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos2.getY()))
                .build()
        ).set(p.cornerAnchorPos2.getY());
        sgCornerAnchorPos2.add(new IntSetting.Builder()
                .name("corner-anchor-pos-z2")
                .description("The Z coordinate of the second corner anchor position of the placer.")
                .defaultValue(defaults.cornerAnchorPos2.getZ())
                .sliderRange(-5, 5)
                .onChanged(value -> p.cornerAnchorPos2 = new BlockPos(p.cornerAnchorPos2.getX(), p.cornerAnchorPos2.getY(), value))
                .onModuleActivated(setting -> setting.set(p.cornerAnchorPos2.getZ()))
                .build()
        ).set(p.cornerAnchorPos2.getZ());

        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-x1")
                .description("Is the first X position should be rotated where is the player looking at.")
                .defaultValue(defaults.x1Rotate)
                .onChanged(value -> p.x1Rotate = value)
                .onModuleActivated(setting -> setting.set(p.x1Rotate))
                .build()
        ).set(p.x1Rotate);
        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-y1")
                .description("Is the first Y position should be rotated where is the player looking at.")
                .defaultValue(defaults.y1Rotate)
                .onChanged(value -> p.y1Rotate = value)
                .onModuleActivated(setting -> setting.set(p.y1Rotate))
                .build()
        ).set(p.y1Rotate);
        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-z1")
                .description("Is the first Z position should be rotated where is the player looking at.")
                .defaultValue(defaults.z1Rotate)
                .onChanged(value -> p.z1Rotate = value)
                .onModuleActivated(setting -> setting.set(p.z1Rotate))
                .build()
        ).set(p.z1Rotate);
        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-x2")
                .description("Is the second X position should be rotated where is the player looking at.")
                .defaultValue(defaults.x2Rotate)
                .onChanged(value -> p.x2Rotate = value)
                .onModuleActivated(setting -> setting.set(p.x2Rotate))
                .build()
        ).set(p.x2Rotate);
        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-y2")
                .description("Is the second Y position should be rotated where is the player looking at.")
                .defaultValue(defaults.y2Rotate)
                .onChanged(value -> p.y2Rotate = value)
                .onModuleActivated(setting -> setting.set(p.y2Rotate))
                .build()
        ).set(p.y2Rotate);
        sgRotation.add(new BoolSetting.Builder()
                .name("rotate-z2")
                .description("Is the second Z position should be rotated where is the player looking at.")
                .defaultValue(defaults.z2Rotate)
                .onChanged(value -> p.z2Rotate = value)
                .onModuleActivated(setting -> setting.set(p.z2Rotate))
                .build()
        ).set(p.z2Rotate);

        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-x1")
                .description("Is the first X position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.x1Anchor)
                .onChanged(value -> p.x1Anchor = value)
                .onModuleActivated(setting -> setting.set(p.x1Anchor))
                .build()
        ).set(p.x1Anchor);
        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-y1")
                .description("Is the first Y position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.y1Anchor)
                .onChanged(value -> p.y1Anchor = value)
                .onModuleActivated(setting -> setting.set(p.y1Anchor))
                .build()
        ).set(p.y1Anchor);
        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-z1")
                .description("Is the first Z position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.z1Anchor)
                .onChanged(value -> p.z1Anchor = value)
                .onModuleActivated(setting -> setting.set(p.z1Anchor))
                .build()
        ).set(p.z1Anchor);
        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-x2")
                .description("Is the second X position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.x2Anchor)
                .onChanged(value -> p.x2Anchor = value)
                .onModuleActivated(setting -> setting.set(p.x2Anchor))
                .build()
        ).set(p.x2Anchor);
        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-y2")
                .description("Is the second Y position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.y2Anchor)
                .onChanged(value -> p.y2Anchor = value)
                .onModuleActivated(setting -> setting.set(p.y2Anchor))
                .build()
        ).set(p.y2Anchor);
        sgAnchor.add(new BoolSetting.Builder()
                .name("anchor-z2")
                .description("Is the second Z position should be anchored (fixed) in the world space or be relative to the player.")
                .defaultValue(defaults.z2Anchor)
                .onChanged(value -> p.z2Anchor = value)
                .onModuleActivated(setting -> setting.set(p.z2Anchor))
                .build()
        ).set(p.z2Anchor);

        sgBlocks.add(new BlockListSetting.Builder()
                .name("blocks")
                .description("Blocks to place.")
                .defaultValue(defaults.blocks)
                .onChanged(value -> p.blocks = value)
                .onModuleActivated(setting -> setting.set(p.blocks))
                .build()
        ).set(p.blocks);

        sgColor.add(new ColorSetting.Builder()
                .name("side-color")
                .description("The color of the side of the placer hologram.")
                .defaultValue(defaults.sideColor)
                .onChanged(value -> p.sideColor = value)
                .onModuleActivated(setting -> setting.set(p.sideColor))
                .build()
        ).set(p.sideColor);
        sgColor.add(new ColorSetting.Builder()
                .name("line-color")
                .description("The color of the line of the placer hologram.")
                .defaultValue(defaults.lineColor)
                .onChanged(value -> p.lineColor = value)
                .onModuleActivated(setting -> setting.set(p.lineColor))
                .build()
        ).set(p.lineColor);

        sgControls.add(new BoolSetting.Builder()
                .name("visible")
                .description("Is the placer visible.")
                .defaultValue(defaults.visible)
                .onChanged(value -> {
                    p.visible = value;
                    reloadPlacers.run();
                })
                .onModuleActivated(setting -> setting.set(p.visible))
                .build()
        ).set(p.visible);
        sgControls.add(new BoolSetting.Builder()
                .name("active")
                .description("Is the placer active.")
                .defaultValue(defaults.active)
                .onChanged(value -> {
                    p.active = value;
                    reloadPlacers.run();
                })
                .onModuleActivated(setting -> setting.set(p.active))
                .build()
        ).set(p.active);

        settings.onActivated();
        add(theme.settings(settings)).expandX();
    }
}
