package zgoly.meteorist.settings;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static zgoly.meteorist.Meteorist.COPY;

public class StringPairSetting extends Setting<List<Pair<String, String>>> {
    private final Pair<String, String> placeholder;

    public StringPairSetting(String name, String description, List<Pair<String, String>> defaultValue, Pair<String, String> placeholder, Consumer<List<Pair<String, String>>> onChanged, Consumer<Setting<List<Pair<String, String>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.placeholder = placeholder;
    }

    public static void register() {
        DefaultSettingsWidgetFactory.registerCustomFactory(StringPairSetting.class, (theme) -> (table, setting) -> {
            WTable wtable = table.add(theme.table()).expandX().widget();
            StringPairSetting.fillTable(theme, wtable, (StringPairSetting) setting);
        });
    }

    public static void fillTable(GuiTheme theme, WTable table, StringPairSetting setting) {
        table.clear();

        List<Pair<String, String>> pairs = setting.get();
        Pair<String, String> placeholder = setting.placeholder;

        for (Pair<String, String> pair : pairs) {
            WTextBox textBoxK = table.add(theme.textBox(pair.getLeft(), placeholder.getLeft())).minWidth(150).expandX().widget();
            textBoxK.actionOnUnfocused = () -> pair.setLeft(textBoxK.get());

            WTextBox textBoxV = table.add(theme.textBox(pair.getRight(), placeholder.getRight())).minWidth(150).expandX().widget();
            textBoxV.actionOnUnfocused = () -> pair.setRight(textBoxV.get());

            WButton copy = table.add(theme.button(COPY)).widget();
            copy.action = () -> {
                pairs.add(pairs.indexOf(pair), new Pair<>(pair.getLeft(), pair.getRight()));
                fillTable(theme, table, setting);
            };

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                pairs.remove(pair);
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!pairs.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            pairs.add(new Pair<>("", ""));
            fillTable(theme, table, setting);
        };

        table.row();
    }

    @Override
    protected List<Pair<String, String>> parseImpl(String str) {
        String[] values = str.split(",");

        List<Pair<String, String>> pairs = new ArrayList<>(values.length / 2);

        try {
            for (int i = 0; i < values.length; i += 2) {
                pairs.add(new Pair<>(values[i], values[i + 1]));
            }
        } catch (Exception ignored) {
        }

        return pairs;
    }

    @Override
    protected boolean isValueValid(List<Pair<String, String>> value) {
        return true;
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Pair<String, String> pair : get()) {
            NbtCompound pairTag = new NbtCompound();
            pairTag.putString("left", pair.getLeft());
            pairTag.putString("right", pair.getRight());
            valueTag.add(pairTag);
        }
        tag.put("pairs", valueTag);

        return tag;
    }

    @Override
    protected List<Pair<String, String>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("pairs", NbtList.COMPOUND_TYPE);
        for (int i = 0; i < valueTag.size(); i++) {
            NbtCompound pairTag = valueTag.getCompound(i);
            get().add(new Pair<>(pairTag.getString("left"), pairTag.getString("right")));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Pair<String, String>>, StringPairSetting> {
        private Pair<String, String> placeholder;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(List<Pair<String, String>> pairs) {
            this.defaultValue = pairs;
            return this;
        }

        public Builder placeholder(Pair<String, String> pair) {
            this.placeholder = pair;
            return this;
        }

        @Override
        public StringPairSetting build() {
            return new StringPairSetting(name, description, defaultValue, placeholder, onChanged, onModuleActivated, visible);
        }
    }
}
