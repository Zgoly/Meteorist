package zgoly.meteorist.utils;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlotClickSyntaxHighlighting implements WTextBox.Renderer {
    private record Section(String text, Color color) {}

    @Override
    public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
        List<Section> sections = new ArrayList<>();
        int lastNumber = 0;
        boolean wasMinus = false;

        Matcher matcher = Pattern.compile("(-|\\d+|[^\\d-]+)").matcher(text);

        while (matcher.find()) {
            String part = matcher.group();

            if (part.chars().allMatch(Character::isDigit)) {
                lastNumber = sections.size();
                sections.add(new Section(part, wasMinus ? Color.CYAN : Color.GREEN));
                wasMinus = false;
            } else if (part.contains("-")) {
                if (lastNumber < sections.size()) sections.set(lastNumber, new Section(sections.get(lastNumber).text, Color.CYAN));
                sections.add(new Section(part, Color.CYAN));
                wasMinus = true;
            } else {
                sections.add(new Section(part, Color.GRAY));
            }
        }

        for (Section section : sections) {
            renderer.text(section.text, x, y, section.color, false);
            x += renderer.theme.textWidth(section.text);
        }
    }
}
