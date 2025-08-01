package zgoly.meteorist.settings;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.COPY;
import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.EDIT;
import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class ItemsSetting extends Setting<List<Item>> {
    private final Predicate<Item> filter;

    public ItemsSetting(String name, String description, List<Item> defaultValue, Consumer<List<Item>> onChanged, Consumer<Setting<List<Item>>> onModuleActivated, IVisible visible, Predicate<Item> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.filter = filter;
    }

    @Override
    protected List<Item> parseImpl(String str) {
        return new ArrayList<>();
    }

    @Override
    protected boolean isValueValid(List<Item> value) {
        return true;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList listTag = new NbtList();
        for (Item item : get()) {
            NbtCompound itemTag = new NbtCompound();
            Identifier id = Registries.ITEM.getId(item);
            itemTag.putString("item", id.toString());
            listTag.add(itemTag);
        }
        tag.put("value", listTag);
        return tag;
    }

    @Override
    public List<Item> load(NbtCompound tag) {
        NbtList listTag = tag.getListOrEmpty("value");
        get().clear();
        for (NbtElement element : listTag) {
            if (!(element instanceof NbtCompound itemTag)) continue;
            String idStr = itemTag.getString("item", "");
            Item item = Registries.ITEM.get(Identifier.of(idStr));
            if (item != Items.AIR) get().add(item);
        }
        return get();
    }

    public static void register() {
        DefaultSettingsWidgetFactory.registerCustomFactory(ItemsSetting.class, (theme) -> (table, setting) -> {
            WTable wtable = table.add(theme.table()).expandX().widget();
            fillTable(theme, wtable, (ItemsSetting) setting);
        });
    }

    // Had to shitcode little bit to use ItemSettingScreen
    public static void fillTable(GuiTheme theme, WTable table, ItemsSetting setting) {
        table.clear();

        List<Item> items = setting.get();

        for (int i = 0; i < items.size(); i++) {
            int index = i;
            Item item = items.get(index);

            WContainer container = table.add(theme.horizontalList()).expandX().widget();

            WWidget icon = container.add(theme.item(item.getDefaultStack())).widget();
            icon.tooltip = item.getName().getString();

            WButton edit = container.add(theme.button(EDIT)).widget();
            edit.tooltip = "Edit the item.";
            edit.action = () -> {
                ItemSetting tempSetting = new ItemSetting.Builder().defaultValue(item).filter(setting.filter).build();

                mc.setScreen(new ItemSettingScreen(theme, tempSetting) {
                    @Override
                    public void onClosed() {
                        Item result = tempSetting.get();
                        items.set(index, result);
                        fillTable(theme, table, setting);
                    }
                });
            };

            if (items.size() > 1) {
                WContainer moveContainer = container.add(theme.horizontalList()).expandX().widget();

                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move item up.";
                    moveUp.action = () -> {
                        items.remove(index);
                        items.add(index - 1, item);
                        fillTable(theme, table, setting);
                    };
                }

                if (index < items.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move item down.";
                    moveDown.action = () -> {
                        items.remove(index);
                        items.add(index + 1, item);
                        fillTable(theme, table, setting);
                    };
                }
            }

            WButton copy = container.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate item.";
            copy.action = () -> {
                items.add(index, item);
                fillTable(theme, table, setting);
            };

            WMinus remove = container.add(theme.minus()).widget();
            remove.tooltip = "Remove item.";
            remove.action = () -> {
                items.remove(index);
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!items.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("Add")).expandX().widget();
        add.tooltip = "Add new item to the list.";
        add.action = () -> {
            ItemSetting tempSetting = new ItemSetting.Builder().filter(setting.filter).build();

            mc.setScreen(new ItemSettingScreen(theme, tempSetting) {
                @Override
                public void onClosed() {
                    Item result = tempSetting.get();
                    if (result != null) {
                        tempSetting.set(null);
                        items.add(result);
                        fillTable(theme, table, setting);
                    }
                }
            });
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    public static class Builder extends SettingBuilder<Builder, List<Item>, ItemsSetting> {
        private Predicate<Item> filter;

        public Builder() {
            super(new ArrayList<>());
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public ItemsSetting build() {
            return new ItemsSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
