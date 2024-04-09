package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.screens.settings.BlockListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ColorSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.Utils;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Placer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");

    private final Setting<Boolean> rotateHead = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate-head")
            .description("Rotate head when placing a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotatePlacement = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate-placement")
            .description("Rotate placement in the direction you are looking.")
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

    private static GuiTexture ARROW_UP;
    private static GuiTexture ARROW_DOWN;
    private static GuiTexture COPY;

    public Placer() {
        super(Meteorist.CATEGORY, "placer", "Places blocks in range.");

        // Rainbow
        RainbowColors.register(this::onTickRainbow);

        // Icons
        ARROW_UP = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/arrow_up.png"));
        ARROW_DOWN = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/arrow_down.png"));
        COPY = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/copy.png"));
    }

    /*
    BIG thanks for Maxsupermanhd's Villager Roller. Finally figured out how that work
    https://github.com/maxsupermanhd/meteor-villager-roller
    */

    List<placer> placers = new ArrayList<>();

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList l = new NbtList();
        for (placer e : placers) {
            l.add(e.toTag());
        }
        tag.put("placer", l);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);
        NbtList l = tag.getList("placer", NbtElement.COMPOUND_TYPE);
        placers.clear();
        for (NbtElement e : l) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                continue;
            }
            placers.add(new placer().fromTag((NbtCompound) e));
        }
        return super.fromTag(tag);
    }

    public static class placer implements ISerializable<placer> {
        public int x1;
        public int y1;
        public int z1;
        public int x2;
        public int y2;
        public int z2;
        public List<Block> blocks;
        public SettingColor sideColor;
        public SettingColor lineColor;
        public boolean visible;
        public boolean enabled;

        public placer(int _x1, int _y1, int _z1, int _x2, int _y2, int _z2, List<Block> _blocks, SettingColor _sideColor, SettingColor _lineColor, boolean _visible, boolean _enabled) {
            this.x1 = _x1;
            this.y1 = _y1;
            this.z1 = _z1;
            this.x2 = _x2;
            this.y2 = _y2;
            this.z2 = _z2;
            this.blocks = _blocks;
            this.sideColor = _sideColor;
            this.lineColor = _lineColor;
            this.visible = _visible;
            this.enabled = _enabled;
        }

        public placer() {
            this.x1 = 0;
            this.y1 = 0;
            this.z1 = 0;
            this.x2 = 0;
            this.y2 = 0;
            this.z2 = 0;
            this.blocks = new ArrayList<>(Collections.singletonList(Blocks.COBBLESTONE));
            this.sideColor = new Color(0, 255, 0, 50).toSetting();
            this.lineColor = new Color(0, 255, 0, 255).toSetting();
            this.visible = true;
            this.enabled = false;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putInt("x1", this.x1);
            tag.putInt("y1", this.y1);
            tag.putInt("z1", this.z1);
            tag.putInt("x2", this.x2);
            tag.putInt("y2", this.y2);
            tag.putInt("z2", this.z2);
            NbtList blocksList = new NbtList();
            for (Block block : this.blocks) {
                blocksList.add(NbtString.of(String.valueOf(Registries.BLOCK.getId(block))));
            }
            tag.put("blocks", blocksList);
            tag.put("sideColor", this.sideColor.toTag());
            tag.put("lineColor", this.lineColor.toTag());
            tag.putBoolean("display", this.visible);
            tag.putBoolean("enabled", this.enabled);
            return tag;
        }

        @Override
        public placer fromTag(NbtCompound tag) {
            this.x1 = tag.getInt("x1");
            this.y1 = tag.getInt("y1");
            this.z1 = tag.getInt("z1");
            this.x2 = tag.getInt("x2");
            this.y2 = tag.getInt("y2");
            this.z2 = tag.getInt("z2");
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
            this.visible = tag.getBoolean("display");
            this.enabled = tag.getBoolean("enabled");
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
        int min = -128;
        int max = 128;

        WLabel blocksLabel = theme.label("Blocks");
        blocksLabel.tooltip = "Blocks to place.";

        WLabel sideLabel = theme.label("Side");
        sideLabel.tooltip = "Color of the side.";

        WLabel lineLabel = theme.label("Line");
        lineLabel.tooltip = "Color of the line.";

        WLabel visLabel = theme.label("Vis.");
        visLabel.tooltip = "Visible.";

        WLabel enLabel = theme.label("En.");
        enLabel.tooltip = "Enabled.";

        for (int i = 0; i < placers.size(); i++) {
            if (i == 0) {
                table.add(theme.label("x1"));
                table.add(theme.label("y1"));
                table.add(theme.label("z1"));
                table.add(theme.label("x2"));
                table.add(theme.label("y2"));
                table.add(theme.label("z2"));
                table.add(blocksLabel);
                table.add(sideLabel);
                table.add(lineLabel);
                table.add(visLabel);
                table.add(enLabel);
                table.row();
            }

            placer p = placers.get(i);

            int padding = 16;
            WIntEdit x1 = table.add(theme.intEdit(p.x1, min, max, true)).padRight(padding).widget();
            x1.small = true;
            x1.action = () -> {
                p.x2 = Math.min(p.x2, x1.get());
                p.x1 = x1.get();
                list.clear();
                fillWidget(theme, list);
            };

            WIntEdit y1 = table.add(theme.intEdit(p.y1, min, max, true)).padRight(padding).widget();
            y1.small = true;
            y1.action = () -> {
                p.y2 = Math.min(p.y2, y1.get());
                p.y1 = y1.get();
                list.clear();
                fillWidget(theme, list);
            };

            WIntEdit z1 = table.add(theme.intEdit(p.z1, min, max, true)).padRight(padding).widget();
            z1.small = true;
            z1.action = () -> {
                p.z2 = Math.min(p.z2, z1.get());
                p.z1 = z1.get();
                list.clear();
                fillWidget(theme, list);
            };

            WIntEdit x2 = table.add(theme.intEdit(p.x2, min, max, true)).padRight(padding).widget();
            x2.small = true;
            x2.action = () -> {
                p.x1 = Math.max(p.x1, x2.get());
                p.x2 = x2.get();
                list.clear();
                fillWidget(theme, list);
            };

            WIntEdit y2 = table.add(theme.intEdit(p.y2, min, max, true)).padRight(padding).widget();
            y2.small = true;
            y2.action = () -> {
                p.y1 = Math.max(p.y1, y2.get());
                p.y2 = y2.get();
                list.clear();
                fillWidget(theme, list);
            };

            WIntEdit z2 = table.add(theme.intEdit(p.z2, min, max, true)).padRight(padding).widget();
            z2.small = true;
            z2.action = () -> {
                p.z1 = Math.max(p.z1, z2.get());
                p.z2 = z2.get();
                list.clear();
                fillWidget(theme, list);
            };

            WContainer blocksContainer = table.add(theme.horizontalList()).expandX().widget();
            WContainer blocksContainerInner = blocksContainer.add(theme.horizontalList()).expandX().widget();
            for (Block block : p.blocks) {
                WItem item = blocksContainerInner.add(theme.item(block.asItem().getDefaultStack())).widget();
                item.tooltip = blocksLabel.tooltip;
            }
            WButton blocksSelect = blocksContainer.add(theme.button(GuiRenderer.EDIT)).padRight(padding).widget();
            blocksSelect.tooltip = blocksLabel.tooltip;
            blocksSelect.action = () -> mc.setScreen(new BlockListSettingScreen(theme, new BlockListSetting("", "", p.blocks, blocks -> {
                p.blocks = blocks;
                list.clear();
                fillWidget(theme, list);
            }, null, null, null)));

            WContainer sideColorContainer = table.add(theme.horizontalList()).widget();
            sideColorContainer.add(theme.quad(p.sideColor)).widget();
            WButton sideColorSelect = sideColorContainer.add(theme.button(GuiRenderer.EDIT)).padRight(padding).widget();
            sideColorSelect.tooltip = sideLabel.tooltip;
            sideColorSelect.action = () -> mc.setScreen(new ColorSettingScreen(theme, new ColorSetting("", "", p.sideColor, color -> {
                p.sideColor = color;
                list.clear();
                fillWidget(theme, list);
            }, null, null)));

            WContainer lineColorContainer = table.add(theme.horizontalList()).widget();
            lineColorContainer.add(theme.quad(p.lineColor)).widget();
            WButton lineColorSelect = lineColorContainer.add(theme.button(GuiRenderer.EDIT)).padRight(padding).widget();
            lineColorSelect.tooltip = lineLabel.tooltip;
            lineColorSelect.action = () -> mc.setScreen(new ColorSettingScreen(theme, new ColorSetting("", "", p.lineColor, color -> {
                p.lineColor = color;
                list.clear();
                fillWidget(theme, list);
            }, null, null)));

            WCheckbox visible = table.add(theme.checkbox(p.visible)).padRight(padding).widget();
            visible.tooltip = visLabel.tooltip;
            visible.action = () -> p.visible = visible.checked;

            WCheckbox enabled = table.add(theme.checkbox(p.enabled)).padRight(padding).widget();
            enabled.tooltip = enLabel.tooltip;
            enabled.action = () -> p.enabled = enabled.checked;

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (placers.size() > 1) {
                int index = placers.indexOf(p);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move placer up.";
                    moveUp.action = () -> {
                        placers.remove(index);
                        placers.add(index - 1, p);
                        list.clear();
                        fillWidget(theme, list);
                    };
                }

                if (index < placers.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move placer down.";
                    moveDown.action = () -> {
                        placers.remove(index);
                        placers.add(index + 1, p);
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
                placers.add(placers.indexOf(p), new placer(p.x1, p.y1, p.z1, p.x2, p.y2, p.z2, p.blocks, p.sideColor, p.lineColor, p.visible, p.enabled));
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove placer.";
            remove.action = () -> {
                list.clear();
                placers.remove(p);
                fillWidget(theme, list);
            };
            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();
        WButton create = controls.add(theme.button("Add new placer")).expandX().widget();
        create.action = () -> {
            placers.add(new placer());
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
                        Path path = new File(result).toPath();
                        NbtTagSizeTracker tagSizeTracker = new NbtTagSizeTracker(65536, 512); // 64KiB
                        NbtCompound data = NbtIo.readCompressed(path, tagSizeTracker);
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

    private List<BlockPos> getPlacePos(BlockPos pos1, BlockPos pos2, Entity entity, boolean render) {
        Direction dir = rotatePlacement.get() ? Direction.fromRotation(entity.getYaw()) : Direction.EAST;
        BlockPos pos = entity.getBlockPos();
        BlockPos rel1 = pos;
        BlockPos rel2 = pos;
        if (render) {
            rel1 = BlockPos.ofFloored(pos.toCenterPos().offset(dir, 0.5).offset(Direction.UP, 0.5).offset(dir.rotateYClockwise(), 0.5));
            rel2 = BlockPos.ofFloored(pos.toCenterPos().offset(dir.getOpposite(), 0.5).offset(Direction.DOWN, 0.5).offset(dir.rotateYClockwise().getOpposite(), 0.5));
        }
        List<BlockPos> result = new ArrayList<>();
        result.add(rel1.offset(dir, pos1.getX()).offset(Direction.UP, pos1.getY()).offset(dir.rotateYClockwise(), pos1.getZ()));
        result.add(rel2.offset(dir, pos2.getX()).offset(Direction.UP, pos2.getY()).offset(dir.rotateYClockwise(), pos2.getZ()));
        return result;
    }

    private int timer;
    private boolean work;

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
    }

    public void onTickRainbow() {
        for (placer p : placers) {
            p.lineColor.update();
            p.sideColor.update();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (work) {
            int loopCount = 0;
            placersLoop:
            for (placer p : placers) {
                if (p.enabled) {
                    List<BlockPos> positions = getPlacePos(new BlockPos(p.x1, p.y1, p.z1), new BlockPos(p.x2, p.y2, p.z2), mc.player, false);
                    Iterable<BlockPos> BlockPoses = BlockPos.iterate(positions.get(0), positions.get(1));

                    for (BlockPos blockPos : BlockPoses) {
                        if (useDelay.get() && loopCount >= maxBlocksPerTick.get()) {
                            break placersLoop;
                        }
                        if (!BlockUtils.canPlace(blockPos) || Utils.isCollidesEntity(blockPos)) continue;

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

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (placer p : placers) {
            if (p.visible) {
                List<BlockPos> positions = getPlacePos(new BlockPos(p.x1, p.y1, p.z1), new BlockPos(p.x2, p.y2, p.z2), mc.player, true);
                BlockPos r1 = positions.get(0);
                BlockPos r2 = positions.get(1);
                event.renderer.box(r1.getX(), r1.getY(), r1.getZ(), r2.getX(), r2.getY(), r2.getZ(), p.sideColor, p.lineColor, ShapeMode.Both, 0);
            }
        }
    }
}