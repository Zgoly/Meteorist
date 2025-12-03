package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.misc.DebugLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SignSearch extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<SearchSide> searchSide = sgGeneral.add(new EnumSetting.Builder<SearchSide>()
            .name("search-side")
            .description("Which side(s) of the sign to search.")
            .defaultValue(SearchSide.Both)
            .build()
    );
    private final Setting<Boolean> showResults = sgGeneral.add(new BoolSetting.Builder()
            .name("show-results")
            .description("Show found signs in GUI.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> updateResultsEveryTick = sgGeneral.add(new BoolSetting.Builder()
            .name("update-every-tick")
            .description("Re-run the search every tick (useful for dynamic sign changes). May impact performance.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> renderSigns = sgRender.add(new BoolSetting.Builder()
            .name("render-signs")
            .description("Render found signs in world.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Color of the side of the sign render box.")
            .defaultValue(new SettingColor(255, 255, 0, 50))
            .visible(renderSigns::get)
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of the line of the sign render box.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .visible(renderSigns::get)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How to render the sign boxes.")
            .defaultValue(ShapeMode.Both)
            .visible(renderSigns::get)
            .build()
    );

    private final List<SignBlockEntity> foundSigns = new ArrayList<>();
    private String searchBarText = "";

    private GuiTheme currentTheme;
    private WVerticalList currentResultsList;

    private final DebugLogger debugLogger;

    public SignSearch() {
        super(Meteorist.CATEGORY, "sign-search", "Searches sign text using regex and highlights matching signs.");

        debugLogger = new DebugLogger(this, settings);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        currentTheme = theme;
        WVerticalList list = theme.verticalList();

        WTextBox searchBar = list.add(theme.textBox(searchBarText, "Enter your search query. Regex is supported.")).expandX().widget();
        currentResultsList = theme.verticalList();
        list.add(currentResultsList).expandX();

        searchBar.action = () -> {
            searchBarText = searchBar.get();
            refreshGui();
        };

        refreshGui();
        return list;
    }

    private void refreshGui() {
        if (currentTheme == null || currentResultsList == null) return;

        currentResultsList.clear();
        performSearch();

        WTable table = currentResultsList.add(currentTheme.table()).expandX().widget();
        table.horizontalSpacing = 12;

        if (showResults.get()) {
            if (!foundSigns.isEmpty()) {
                table.add(currentTheme.label("Item")).expandX();
                table.add(currentTheme.label("Name")).expandX();
                table.add(currentTheme.label("Pos")).expandX();
                table.add(currentTheme.label("Front Text")).expandX();
                table.add(currentTheme.label("Back Text")).expandX();
                table.add(currentTheme.label("Goto")).expandX();
                table.add(currentTheme.label("Look at")).expandX();
            }

            for (SignBlockEntity signBlockEntity : foundSigns) {
                table.row();

                BlockPos pos = signBlockEntity.getBlockPos();
                Item signItem = mc.level.getBlockState(pos).getBlock().asItem();
                table.add(currentTheme.item(signItem.getDefaultInstance())).expandX();
                table.add(currentTheme.label(signItem.getName().getString())).expandX();
                table.add(currentTheme.label(String.format(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()))).expandX();

                WVerticalList frontTextList = table.add(currentTheme.verticalList()).expandX().widget();
                for (Component message : signBlockEntity.getFrontText().getMessages(false)) {
                    frontTextList.add(currentTheme.label(message.getString())).expandX();
                }

                WVerticalList backTextList = table.add(currentTheme.verticalList()).expandX().widget();
                for (Component message : signBlockEntity.getBackText().getMessages(false)) {
                    backTextList.add(currentTheme.label(message.getString())).expandX();
                }

                WButton gotoBtn = table.add(currentTheme.button("Goto")).expandX().widget();
                gotoBtn.action = () -> PathManagers.get().moveTo(pos);

                WButton lookAtBtn = table.add(currentTheme.button("Look at")).expandX().widget();
                lookAtBtn.action = () -> {
                    if (mc.player != null) {
                        mc.player.setYRot((float) Rotations.getYaw(pos));
                        mc.player.setXRot((float) Rotations.getPitch(pos));
                    }
                };
            }
        }
    }

    private void performSearch() {
        foundSigns.clear();

        if (mc.level == null || searchBarText.isEmpty()) return;

        Pattern pattern;
        try {
            pattern = Pattern.compile(searchBarText, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            debugLogger.error(exception.getMessage());
            return;
        }

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof SignBlockEntity sign)) continue;

            boolean matches = false;

            switch (searchSide.get()) {
                case Front -> matches = matchesText(pattern, sign.getFrontText());
                case Back -> matches = matchesText(pattern, sign.getBackText());
                case Both ->
                        matches = matchesText(pattern, sign.getFrontText()) || matchesText(pattern, sign.getBackText());
            }

            if (matches) foundSigns.add(sign);
        }
    }

    private boolean matchesText(Pattern pattern, SignText signText) {
        if (signText == null) return false;
        for (int i = 0; i < 4; i++) {
            String line = signText.getMessage(i, false).getString();
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (updateResultsEveryTick.get()) {
            refreshGui();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderSigns.get() || !isActive()) return;

        for (SignBlockEntity signBlockEntity : foundSigns) {
            event.renderer.box(signBlockEntity.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    public enum SearchSide {
        Front,
        Back,
        Both
    }
}