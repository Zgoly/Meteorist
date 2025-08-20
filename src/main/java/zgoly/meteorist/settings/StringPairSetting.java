package zgoly.meteorist.settings;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.COPY;
import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

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
            WContainer container = table.add(theme.horizontalList()).expandX().widget();
            int index = pairs.indexOf(pair);

            WTextBox textBoxKey = container.add(theme.textBox(pair.getLeft(), placeholder.getLeft())).minWidth(150).expandX().widget();
            textBoxKey.actionOnUnfocused = () -> pair.setLeft(textBoxKey.get());

            WTextBox textBoxValue = container.add(theme.textBox(pair.getRight(), placeholder.getRight())).minWidth(150).expandX().widget();
            textBoxValue.actionOnUnfocused = () -> pair.setRight(textBoxValue.get());

            if (pairs.size() > 1) {
                WContainer moveContainer = container.add(theme.horizontalList()).expandX().widget();

                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move pair up.";
                    moveUp.action = () -> {
                        pairs.remove(index);
                        pairs.add(index - 1, pair);
                        fillTable(theme, table, setting);
                    };
                }

                if (index < pairs.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move pair down.";
                    moveDown.action = () -> {
                        pairs.remove(index);
                        pairs.add(index + 1, pair);
                        fillTable(theme, table, setting);
                    };
                }
            }

            WButton copy = container.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate pair.";
            copy.action = () -> {
                pairs.add(index, new Pair<>(pair.getLeft(), pair.getRight()));
                fillTable(theme, table, setting);
            };

            WMinus remove = container.add(theme.minus()).widget();
            remove.tooltip = "Remove pair.";
            remove.action = () -> {
                pairs.remove(pair);
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!pairs.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("Add")).expandX().widget();
        add.tooltip = "Add new pair to the list.";
        add.action = () -> {
            pairs.add(new Pair<>("", ""));
            fillTable(theme, table, setting);
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };

        table.row();
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue);
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
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();

        for (Pair<String, String> pair : get()) {
            NbtList pairTag = new NbtList();
            pairTag.add(NbtString.of(pair.getLeft()));
            pairTag.add(NbtString.of(pair.getRight()));
            valueTag.add(pairTag);
        }

        tag.put("value", valueTag);
        return tag;
    }

    @Override
    protected List<Pair<String, String>> load(NbtCompound tag) {
        NbtList valueTag = tag.getListOrEmpty("value");
        get().clear();

        for (NbtElement nbtElement : valueTag) {
            if (!(nbtElement instanceof NbtList pairList)) continue;

            String left = pairList.get(0).asString().orElse("");
            String right = pairList.get(1).asString().orElse("");

            get().add(new Pair<>(left, right));
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
