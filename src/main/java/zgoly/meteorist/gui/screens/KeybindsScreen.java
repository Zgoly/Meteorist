// Idea by @rip_2xc3
package zgoly.meteorist.gui.screens;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.system.MemoryStack;
import zgoly.meteorist.utils.KeybindUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class KeybindsScreen extends WindowTabScreen {
    public final Settings settings = new Settings();

    private final SettingGroup sgSettings = settings.createGroup("Settings");

    private final Setting<Boolean> showMeteorBinds = sgSettings.add(new BoolSetting.Builder()
            .name("show-meteor-binds")
            .description("Shows Meteor module keybinds.")
            .defaultValue(true)
            .onChanged(_ -> dirty = true)
            .build()
    );

    private final Setting<Boolean> showMcBinds = sgSettings.add(new BoolSetting.Builder()
            .name("show-minecraft-binds")
            .description("Shows vanilla Minecraft keybinds.")
            .defaultValue(true)
            .onChanged(_ -> dirty = true)
            .build()
    );

    private InputConstants.Key selectedKey;
    private KeybindUtils.Bind binding;
    private Module meteorBinding;
    private boolean dirty;

    private WVerticalList keyboardList;
    private WSection meteorSection;
    private WVerticalList meteorList;
    private WSection mcSection;
    private WVerticalList mcList;
    private WVerticalList settingsContainer;

    // Keyboard layout, measured in magic key units (1 unit = one standard keycap width)
    private static final double BLOCK_GAP = 0.5;

    private static final int FN_KEYS_PER_GROUP = 4;
    private static final double FN_FIRST_COLUMN = 2;
    private static final double FN_GROUP_WIDTH = FN_KEYS_PER_GROUP + BLOCK_GAP;

    private static final double MAIN_BLOCK_WIDTH = 15;  // ` -- Backspace
    private static final double NAV_BLOCK_WIDTH = 3;    // Insert/Home/PgUp -- Del/End/PgDn and arrows
    private static final double NUMPAD_BLOCK_WIDTH = 4; // NumLock -- numpad operators

    private static final double NAV_COLUMN = MAIN_BLOCK_WIDTH + BLOCK_GAP;                // first nav column
    private static final double NUMPAD_COLUMN = NAV_COLUMN + NAV_BLOCK_WIDTH + BLOCK_GAP; // first numpad column
    private static final double KEYBOARD_WIDTH = NUMPAD_COLUMN + NUMPAD_BLOCK_WIDTH;      // total width in units

    private static final double MODIFIER_WIDTH = 1.25;
    private static final double SPACE_WIDTH = 6.25;

    private static final double MOUSE_LEFT_COLUMN = 0;
    private static final double MOUSE_LEFT_WIDTH = 1.5;
    private static final double MOUSE_MIDDLE_COLUMN = MOUSE_LEFT_COLUMN + MOUSE_LEFT_WIDTH;
    private static final double MOUSE_MIDDLE_WIDTH = 1;
    private static final double MOUSE_RIGHT_COLUMN = MOUSE_MIDDLE_COLUMN + MOUSE_MIDDLE_WIDTH;
    private static final double MOUSE_RIGHT_WIDTH = 1.5;

    private static final double MOUSE_SIDE_WIDTH = 1;
    private static final double MOUSE_SIDE_SHIFT = 0.5; // how far buttons 4/5 stick out to the left

    private static final int ROW_NUMBERS = 1;
    private static final int MOUSE_ROWS = 3;

    private static final double KEY_GAP_PX = 4;
    private static final double KEY_MAX_PX = 40;
    private static final double KEY_MIN_PX = 20;
    private static final double ROW_GAP_RATIO = 0.5;

    public KeybindsScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Override
    public void initWidgets() {
        settingsContainer = add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(settings)).expandX();

        keyboardList = add(theme.verticalList()).centerX().widget();
        keyboardList.spacing = KEY_GAP_PX;

        meteorSection = add(theme.section("Meteor", true)).expandX().widget();
        meteorList = meteorSection.add(theme.verticalList()).expandX().widget();

        mcSection = add(theme.section("Minecraft", true)).expandX().widget();
        mcList = mcSection.add(theme.verticalList()).expandX().widget();

        rebuild();
    }

    @Override
    public void tick() {
        super.tick();

        settings.tick(settingsContainer, theme);

        if (meteorBinding != null && !Modules.get().isBinding()) {
            meteorBinding = null;
            dirty = true;
        }

        if (dirty) {
            dirty = false;
            rebuild();
        }
    }

    @EventHandler
    private void onBindChanged(ModuleBindChangedEvent ignored) {
        meteorBinding = null;
        dirty = true;
    }

    private void selectKey(InputConstants.Key key) {
        selectedKey = selectedKey != null && selectedKey.equals(key) ? null : key;
        dirty = true;
    }

    private void toggleMeteorBinding(Module module) {
        boolean same = meteorBinding == module;

        Modules.get().setModuleToBind(same ? null : module);
        meteorBinding = same ? null : module;
        if (!same) {
            binding = null;
            selectedKey = null;
        }
        dirty = true;
    }

    private void startBinding(KeybindUtils.Bind bind) {
        boolean same = bind.equals(binding);
        binding = same ? null : bind;
        if (!same) {
            selectedKey = null;
            meteorBinding = null;
            Modules.get().setModuleToBind(null);
        }
        dirty = true;
    }

    private void applyBinding(InputConstants.Key key) {
        if (binding == null) return;

        KeybindUtils.setMcKey(binding, key);
        binding = null;
        dirty = true;
    }

    private void cancelBinding() {
        if (binding == null) return;

        binding = null;
        dirty = true;
    }

    private void unbindCurrent() {
        if (binding == null) return;

        KeybindUtils.unbindMc(binding);
        binding = null;
        dirty = true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (meteorBinding != null) return true;

        if (binding == null) return super.keyPressed(event);

        if (event.key() == InputConstants.KEY_ESCAPE) cancelBinding();
        else if (event.key() == InputConstants.KEY_DELETE) unbindCurrent();
        return true;
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        if (meteorBinding != null) return true;

        if (binding == null) return super.keyReleased(event);

        applyBinding(InputConstants.getKey(event));
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return binding == null && meteorBinding == null && super.shouldCloseOnEsc();
    }

    @Override
    public void onClosed() {
        binding = null;
        meteorBinding = null;
        Modules.get().setModuleToBind(null);
        super.onClosed();
    }

    private void rebuild() {
        Map<InputConstants.Key, List<KeybindUtils.Bind>> bound = KeybindUtils.getBoundKeys(showMeteorBinds.get(), showMcBinds.get());
        List<KeybindUtils.Bind> binds = KeybindUtils.getBinds(showMeteorBinds.get(), showMcBinds.get(), true);

        meteorSection.visible = showMeteorBinds.get();
        mcSection.visible = showMcBinds.get();

        buildKeyboard(bound);
        buildList(meteorList, true, binds);
        buildList(mcList, false, binds);
    }

    private void buildList(WVerticalList list, boolean meteor, List<KeybindUtils.Bind> binds) {
        list.clear();

        int shown = 0;
        for (KeybindUtils.Bind bind : binds) {
            if (bind.meteor() != meteor) continue;
            InputConstants.Key key = KeybindUtils.keyOf(bind);
            if (selectedKey != null && !selectedKey.equals(key)) continue;
            shown++;

            WHorizontalList row = list.add(theme.horizontalList()).expandX().widget();

            WLabel title = bind.color() != null
                    ? row.add(theme.label(bind.title()).color(bind.color())).widget()
                    : row.add(theme.label(bind.title())).widget();
            title.tooltip = bind.meteor() ? bind.module().description : "Minecraft control";
            row.add(theme.label("  -  " + bind.keyName()).color(theme.textSecondaryColor()));
            row.add(theme.label("")).expandX();

            if (bind.meteor()) {
                WButton settings = row.add(theme.button("Settings")).padLeft(2).widget();
                settings.action = () -> mc.gui.setScreen(new ModuleScreen(theme, bind.module()));
            }

            boolean meteorBind = bind.meteor();
            boolean bindingThis = meteorBind ? meteorBinding == bind.module() : bind.equals(binding);
            WButton rebind = row.add(theme.button(bindingThis ? "…" : "Bind")).padLeft(2).widget();

            rebind.tooltip = getHint(meteorBind, bindingThis);
            rebind.action = meteorBind ? () -> toggleMeteorBinding(bind.module()) : () -> startBinding(bind);
        }

        if (shown == 0) {
            list.add(theme.label(selectedKey != null ? "Nothing bound to the selected key." : "No occupied keybinds.")
                    .color(theme.textSecondaryColor()));
        }
    }

    private @NonNull String getHint(boolean meteorBind, boolean bindingThis) {
        String hint;
        if (meteorBind) {
            hint = bindingThis
                    ? "Meteor is waiting for a key.\nPress and release it (hold Shift/Ctrl/Alt for combos).\nEsc clears the bind. Click to cancel."
                    : "Rebind with Meteor's binder.\nPress and release a key (hold Shift/Ctrl/Alt for combos).";
        } else {
            hint = (bindingThis
                    ? "Release a key to bind it.\nDelete unbinds, Esc cancels."
                    : "Rebind. Click, then release a key.")
                    + "\nMinecraft binds don't support modifiers.";
        }
        return hint;
    }

    private record KeySpec(InputConstants.Key key, double x, double w, boolean tall) {}

    private void buildKeyboard(Map<InputConstants.Key, List<KeybindUtils.Bind>> bound) {
        keyboardList.clear();

        double gap = theme.scale(KEY_GAP_PX);
        double unit = Math.min(theme.scale(KEY_MAX_PX), (getWindowWidth() - (KEYBOARD_WIDTH - 1) * gap) / KEYBOARD_WIDTH);
        unit = Math.max(unit, theme.scale(KEY_MIN_PX));
        double step = unit + gap;
        double rowGap = unit * ROW_GAP_RATIO;
        double scale = theme.scale(1);
        double keyHeight = theme.pad() * 2 + theme.textHeight();

        List<List<KeySpec>> rows = buildSpecs(step, unit, gap);
        for (int i = 0; i < rows.size(); i++) {
            List<KeySpec> specs = rows.get(i);
            if (specs.isEmpty()) continue;

            boolean mouseRow = i >= rows.size() - MOUSE_ROWS;
            Cell<WHorizontalList> rowCell = keyboardList.add(theme.horizontalList());
            if (i == ROW_NUMBERS || (mouseRow && i == rows.size() - MOUSE_ROWS)) rowCell.padTop(rowGap / scale);
            if (mouseRow) rowCell.centerX();
            WHorizontalList row = rowCell.widget();
            row.spacing = KEY_GAP_PX;

            double cursor = 0;
            for (int j = 0; j < specs.size(); j++) {
                KeySpec spec = specs.get(j);
                double target = spec.x();
                double width = widthOf(spec.w(), unit, gap);

                Cell<WWidget> cell = row.add(keyWidget(spec.key(), width, spec.tall() ? 2 * keyHeight + gap : 0, bound));
                double padLeft = target - cursor - (j > 0 ? gap : 0);
                if (padLeft > 0) cell.padLeft(padLeft / scale);
                if (spec.tall()) cell.top().padBottom(-(keyHeight + gap) / scale);
                cell.minWidth(width / scale);
                cursor = target + width;
            }
        }
    }

    private WWidget keyWidget(InputConstants.Key key, double width, double height, Map<InputConstants.Key, List<KeybindUtils.Bind>> bound) {
        if (key == null) return new WWidget() {};

        String name = key.getDisplayName().getString();
        List<KeybindUtils.Bind> binds = bound.get(key);
        boolean off = binds == null || binds.isEmpty();
        boolean selected = selectedKey != null && selectedKey.equals(key);

        Runnable action = binding != null ? () -> applyBinding(key)
                : meteorBinding != null || off ? null
                : () -> selectKey(key);
        return new KeyWidget(fitLabel(name, width - theme.pad() * 2), key, off, selected, height, off ? "[" + name + "]" : tooltip(name, binds), action);
    }

    private static class KeyWidget extends WWidget {
        private static final double CAP_HOVER = 0.04;
        private static final double CAP_SELECTED = 0.22;
        private static final double CAP_BOUND = 0.11;
        private static final double CAP_FREE = 0.04;

        private final String label;
        private final InputConstants.Key key;
        private final boolean off;
        private final boolean selected;
        private final double tallHeight;
        private final Runnable action;

        private final Color cap = new Color(255, 255, 255);
        private final Color text = new Color();

        private boolean guiPressed;

        private KeyWidget(String label, InputConstants.Key key, boolean off, boolean selected, double tallHeight, String tooltip, Runnable action) {
            this.label = label;
            this.key = key;
            this.off = off;
            this.selected = selected;
            this.tallHeight = tallHeight;
            this.action = action;
            this.tooltip = tooltip;
            instantTooltips = true;
        }

        @Override
        protected void onCalculateSize() {
            double pad = theme.pad();
            width = pad * 2 + theme.textWidth(label);
            height = tallHeight > 0 ? tallHeight : pad * 2 + theme.textHeight();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            boolean flashing = guiPressed || isPhysicallyPressed(key);

            double opacity = flashing ? 0
                    : selected ? CAP_SELECTED
                    : off ? CAP_FREE
                    : CAP_BOUND;
            if (mouseOver) opacity += CAP_HOVER;

            Color background = cap.a(alpha(opacity));
            if (rounded(theme)) Rounded.background(this, theme.pad(), background);
            else renderer.quad(x, y, width, height, background);

            text.set(theme.textColor()).a(255);
            renderer.text(label, x + (width - theme.textWidth(label)) / 2, y + (height - theme.textHeight()) / 2, text, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (mouseOver) guiPressed = true;
            return super.mouseClicked(click, doubled);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent click) {
            guiPressed = false;
            if (mouseOver && action != null) action.run();
            return false;
        }

        private static int alpha(double value) {
            return (int) Math.round(Math.clamp(value, 0, 1) * 255);
        }

        private static boolean isPhysicallyPressed(InputConstants.Key key) {
            if (key.getType() == InputConstants.Type.MOUSE) {
                int button = key.getValue();
                if (button < InputConstants.MOUSE_BUTTON_LEFT || button > InputConstants.MOUSE_BUTTON_5) return false;

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    int mask = 1 << button - 1;
                    return (SDLMouse.SDL_GetMouseState(stack.mallocFloat(1), stack.mallocFloat(1)) & mask) != 0;
                }
            }
            return InputConstants.isKeyDown(key.getValue());
        }

        private static boolean rounded(GuiTheme theme) {
            return theme != null && theme.getClass().getName().startsWith("me.pindour.catppuccin");
        }
    }

    private static final class Rounded {
        private static void background(WWidget widget, double radius, Color color) {
            me.pindour.catppuccin.api.render.shape.RoundedRect.get()
                    .bounds(widget)
                    .radius(radius)
                    .color(color)
                    .render();
        }
    }

    private String tooltip(String name, List<KeybindUtils.Bind> binds) {
        List<KeybindUtils.Bind> sorted = new ArrayList<>(binds);
        sorted.sort((a, b) -> Boolean.compare(b.meteor(), a.meteor()));

        StringBuilder meteor = new StringBuilder();
        StringBuilder mc = new StringBuilder();
        for (KeybindUtils.Bind bind : sorted) {
            StringBuilder sb = bind.meteor() ? meteor : mc;
            if (!sb.isEmpty()) sb.append('\n');

            sb.append(bind.title());
            if (bind.meteor()) {
                if (bind.module().keybind.hasMods()) sb.append(" (").append(KeybindUtils.describe(bind.module().keybind)).append(')');
            } else {
                sb.append(" (Minecraft)");
            }
        }

        List<String> sections = new ArrayList<>();
        sections.add("[" + name + "]");
        if (!meteor.isEmpty()) sections.add(meteor.toString());
        if (!mc.isEmpty()) sections.add(mc.toString());
        return String.join("\n\n", sections);
    }

    private String fitLabel(String text, double maxWidth) {
        if (theme.textWidth(text) <= maxWidth) return text;

        String shortened = shorten(text);
        if (shortened.isEmpty()) return truncate(text, maxWidth);

        return theme.textWidth(shortened) <= maxWidth ? shortened : truncate(shortened, maxWidth);
    }

    private String shorten(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLowerCase(c) && !Character.isWhitespace(c)) builder.append(c);
        }
        return builder.toString();
    }

    private String truncate(String text, double maxWidth) {
        if (theme.textWidth(text) <= maxWidth) return text;
        while (text.length() > 1 && theme.textWidth(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    private double widthOf(double units, double unit, double gap) {
        return unit * units + gap * (units - 1);
    }

    private void key(List<KeySpec> row, int keyCode, double x, double w) {
        key(row, keyCode, x, w, false);
    }

    private void key(List<KeySpec> row, int keyCode, double x, double w, boolean tall) {
        row.add(new KeySpec(InputConstants.Type.KEYBOARD.getOrCreate(keyCode), x, w, tall));
    }

    private void mouseKey(List<KeySpec> row, int button, double x, double w) {
        row.add(new KeySpec(InputConstants.Type.MOUSE.getOrCreate(button), x, w, false));
    }

    private void spacer(List<KeySpec> row, double x, double w) {
        row.add(new KeySpec(null, x, w, false));
    }

    // Method of trial and error
    private List<List<KeySpec>> buildSpecs(double step, double unit, double gap) {
        List<List<KeySpec>> rows = new ArrayList<>();

        List<KeySpec> row0 = new ArrayList<>();
        key(row0, InputConstants.KEY_ESCAPE, 0, 1);
        for (int group = 0; group < 3; group++) {
            for (int i = 0; i < FN_KEYS_PER_GROUP; i++) {
                int keyCode = InputConstants.KEY_F1 + group * FN_KEYS_PER_GROUP + i;
                key(row0, keyCode, step * (FN_FIRST_COLUMN + group * FN_GROUP_WIDTH + i), 1);
            }
        }
        key(row0, InputConstants.KEY_PRINTSCREEN, step * NAV_COLUMN, 1);
        key(row0, InputConstants.KEY_SCROLLLOCK, step * (NAV_COLUMN + 1), 1);
        key(row0, InputConstants.KEY_PAUSE, step * (NAV_COLUMN + 2), 1);
        rows.add(row0);

        List<KeySpec> row1 = new ArrayList<>();
        int[] keys1 = {InputConstants.KEY_GRAVE, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9, InputConstants.KEY_0, InputConstants.KEY_MINUS, InputConstants.KEY_EQUALS};
        for (int i = 0; i < keys1.length; i++) key(row1, keys1[i], step * i, 1);
        key(row1, InputConstants.KEY_BACKSPACE, step * keys1.length, 2);
        key(row1, InputConstants.KEY_INSERT, step * NAV_COLUMN, 1);
        key(row1, InputConstants.KEY_HOME, step * (NAV_COLUMN + 1), 1);
        key(row1, InputConstants.KEY_PAGEUP, step * (NAV_COLUMN + 2), 1);
        key(row1, InputConstants.KEY_NUMLOCK, step * NUMPAD_COLUMN, 1);
        key(row1, SDLScancode.SDL_SCANCODE_KP_DIVIDE, step * (NUMPAD_COLUMN + 1), 1);
        key(row1, InputConstants.KEY_MULTIPLY, step * (NUMPAD_COLUMN + 2), 1);
        key(row1, SDLScancode.SDL_SCANCODE_KP_MINUS, step * (NUMPAD_COLUMN + 3), 1);
        rows.add(row1);

        List<KeySpec> row2 = new ArrayList<>();
        key(row2, InputConstants.KEY_TAB, 0, 1.5);
        int[] keys2 = {InputConstants.KEY_Q, InputConstants.KEY_W, InputConstants.KEY_E, InputConstants.KEY_R, InputConstants.KEY_T, InputConstants.KEY_Y, InputConstants.KEY_U, InputConstants.KEY_I, InputConstants.KEY_O, InputConstants.KEY_P, InputConstants.KEY_LBRACKET, InputConstants.KEY_RBRACKET};
        double tabEnd = widthOf(1.5, unit, gap) + gap;
        for (int i = 0; i < keys2.length; i++) key(row2, keys2[i], tabEnd + step * i, 1);
        key(row2, InputConstants.KEY_BACKSLASH, tabEnd + step * keys2.length, 1.5);
        key(row2, InputConstants.KEY_DELETE, step * NAV_COLUMN, 1);
        key(row2, InputConstants.KEY_END, step * (NAV_COLUMN + 1), 1);
        key(row2, InputConstants.KEY_PAGEDOWN, step * (NAV_COLUMN + 2), 1);
        key(row2, InputConstants.KEY_NUMPAD7, step * NUMPAD_COLUMN, 1);
        key(row2, InputConstants.KEY_NUMPAD8, step * (NUMPAD_COLUMN + 1), 1);
        key(row2, InputConstants.KEY_NUMPAD9, step * (NUMPAD_COLUMN + 2), 1);
        key(row2, InputConstants.KEY_ADD, step * (NUMPAD_COLUMN + 3), 1, true);
        rows.add(row2);

        List<KeySpec> row3 = new ArrayList<>();
        key(row3, InputConstants.KEY_CAPSLOCK, 0, 1.75);
        int[] keys3 = {InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_G, InputConstants.KEY_H, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_SEMICOLON, InputConstants.KEY_APOSTROPHE};
        double capsEnd = widthOf(1.75, unit, gap) + gap;
        for (int i = 0; i < keys3.length; i++) key(row3, keys3[i], capsEnd + step * i, 1);
        key(row3, InputConstants.KEY_RETURN, capsEnd + step * keys3.length, 2.25);
        key(row3, InputConstants.KEY_NUMPAD4, step * NUMPAD_COLUMN, 1);
        key(row3, InputConstants.KEY_NUMPAD5, step * (NUMPAD_COLUMN + 1), 1);
        key(row3, InputConstants.KEY_NUMPAD6, step * (NUMPAD_COLUMN + 2), 1);
        rows.add(row3);

        List<KeySpec> row4 = new ArrayList<>();
        key(row4, InputConstants.KEY_LSHIFT, 0, 2.25);
        int[] keys4 = {InputConstants.KEY_Z, InputConstants.KEY_X, InputConstants.KEY_C, InputConstants.KEY_V, InputConstants.KEY_B, InputConstants.KEY_N, InputConstants.KEY_M, InputConstants.KEY_COMMA, InputConstants.KEY_PERIOD, InputConstants.KEY_SLASH};
        double lShiftEnd = widthOf(2.25, unit, gap) + gap;
        for (int i = 0; i < keys4.length; i++) key(row4, keys4[i], lShiftEnd + step * i, 1);
        key(row4, InputConstants.KEY_RSHIFT, lShiftEnd + step * keys4.length, 2.75);
        key(row4, InputConstants.KEY_UP, step * (NAV_COLUMN + 1), 1);
        key(row4, InputConstants.KEY_NUMPAD1, step * NUMPAD_COLUMN, 1);
        key(row4, InputConstants.KEY_NUMPAD2, step * (NUMPAD_COLUMN + 1), 1);
        key(row4, InputConstants.KEY_NUMPAD3, step * (NUMPAD_COLUMN + 2), 1);
        key(row4, InputConstants.KEY_NUMPADENTER, step * (NUMPAD_COLUMN + 3), 1, true);
        rows.add(row4);

        List<KeySpec> row5 = new ArrayList<>();
        double x = 0;
        key(row5, InputConstants.KEY_LCONTROL, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_LGUI, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_LALT, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_SPACE, x, SPACE_WIDTH);
        x += widthOf(SPACE_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_RALT, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_RGUI, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, SDLScancode.SDL_SCANCODE_APPLICATION, x, MODIFIER_WIDTH);
        x += widthOf(MODIFIER_WIDTH, unit, gap) + gap;
        key(row5, InputConstants.KEY_RCONTROL, x, MODIFIER_WIDTH);
        key(row5, InputConstants.KEY_LEFT, step * NAV_COLUMN, 1);
        key(row5, InputConstants.KEY_DOWN, step * (NAV_COLUMN + 1), 1);
        key(row5, InputConstants.KEY_RIGHT, step * (NAV_COLUMN + 2), 1);
        key(row5, InputConstants.KEY_NUMPAD0, step * NUMPAD_COLUMN, 2);
        key(row5, SDLScancode.SDL_SCANCODE_KP_PERIOD, step * (NUMPAD_COLUMN + 2), 1);
        rows.add(row5);

        List<KeySpec> mouse1 = new ArrayList<>();
        mouseKey(mouse1, InputConstants.MOUSE_BUTTON_LEFT, step * MOUSE_LEFT_COLUMN, MOUSE_LEFT_WIDTH);
        mouseKey(mouse1, InputConstants.MOUSE_BUTTON_MIDDLE, step * MOUSE_MIDDLE_COLUMN, MOUSE_MIDDLE_WIDTH);
        mouseKey(mouse1, InputConstants.MOUSE_BUTTON_RIGHT, step * MOUSE_RIGHT_COLUMN, MOUSE_RIGHT_WIDTH);
        rows.add(mouse1);

        rows.add(mouseSideRow(step, InputConstants.MOUSE_BUTTON_5));
        rows.add(mouseSideRow(step, InputConstants.MOUSE_BUTTON_4));

        return rows;
    }

    private List<KeySpec> mouseSideRow(double step, int button) {
        List<KeySpec> row = new ArrayList<>();
        mouseKey(row, button, step * MOUSE_LEFT_COLUMN, MOUSE_SIDE_WIDTH);
        double spacerWidth = MOUSE_RIGHT_COLUMN + MOUSE_RIGHT_WIDTH - MOUSE_SIDE_WIDTH + 2 * MOUSE_SIDE_SHIFT;
        spacer(row, step * MOUSE_SIDE_WIDTH, spacerWidth);
        return row;
    }
}
