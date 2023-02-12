//By Zgoly

package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.BlockSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.List;

public class Placer extends Module {
    List<Object> list = new ArrayList<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after sending a command in ticks (20 ticks = 1 sec).")
            .defaultValue(20)
            .range(1, 1200)
            .sliderRange(1, 40)
            .build()
    );

    // It would be nice to get help for saving / loading because I don't know how to do it; other things should be easier
    /*
    TODO: add auto save / load for table using `NbtCompound toTag()` and `Module fromTag(NbtCompound tag)`
        - add functionality
        - add placing holograms, when player rotated in right direction, placing holograms should also rotate
    */

    public void createRow(GuiTheme theme, WTable table) {
        int min = -128;
        int max = 128;
        WIntEdit x1 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WIntEdit y1 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WIntEdit z1 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WIntEdit x2 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WIntEdit y2 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WIntEdit z2 = table.add(theme.intEdit(0, min, max, true)).expandX().widget();
        WItem item = table.add(theme.item(Items.AIR.getDefaultStack())).expandX().widget();
        WButton blockSelect = table.add(theme.button("Select Block")).expandX().widget();
        blockSelect.action = () -> mc.setScreen(new BlockSettingScreen(theme, new BlockSetting(
                "", "", Blocks.AIR, block -> item.set(block.asItem().getDefaultStack()), null, null, null
        )));
        WButton sideColor = table.add(theme.button("Side Color")).expandX().widget();
        WButton lineColor = table.add(theme.button("Line Color")).expandX().widget();
        WMinus minus = table.add(theme.minus()).widget();
        minus.action = () -> {
            if (!table.getRow(1).isEmpty()) {
                table.removeRow(1);
            }
        };
        table.row();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WSection blocksSection = list.add(theme.section("Blocks")).expandX().widget();
        WTable table = blocksSection.add(theme.table()).expandX().widget();
        WPlus plus = table.add(theme.plus()).centerX().widget();
        plus.action = () -> createRow(theme, table);
        table.row();
        return list;
    }


    public Placer() {
        super(Meteorist.CATEGORY, "placer", "Places blocks in range.");
    }
}