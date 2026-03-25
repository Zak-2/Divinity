package divinity.gui.click.components.module;


import divinity.gui.click.components.Component;
import divinity.gui.click.windows.MainWindow;
import divinity.module.property.impl.MultiSelectProperty;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;

import java.awt.*;
import java.util.List;

public class ChecklistComponent extends Component {

    public MultiSelectProperty property;
    public MainWindow mainWindow;

    public ChecklistComponent(MultiSelectProperty property, MainWindow mainWindow, float x, float y, float width, float height, int gap) {
        super(x, y, width, height, gap);
        this.property = property;
        this.mainWindow = mainWindow;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(property.getName(), getX() + 1, getY() + 5.5f, -1);

        float boxWidth = 80;
        float boxX = getX() + getWidth() - boxWidth - 2;
        float boxY = getY() + 3;
        float boxHeight = getHeight() - 6;

        boolean isOpen = mainWindow.dropdownComponent != null && mainWindow.dropdownComponent.getChecklist() == this;

        int borderColor = new Color(35, 35, 45).getRGB();
        int bgColor = new Color(15, 15, 20, 200).getRGB();

        ShaderUtils.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 3, borderColor);
        ShaderUtils.drawRoundRect(boxX + 0.5f, boxY + 0.5f, boxX + boxWidth - 0.5f, boxY + boxHeight - 0.5f, 3, bgColor);

        String displayText = buildDisplayText(property.getSelected(), Fonts.INTER_MEDIUM.get(13), (int)boxWidth - 10);
        Fonts.INTER_MEDIUM.get(13).drawStringWithShadow(displayText, boxX + 4, getY() + 7, new Color(200, 200, 200).getRGB());
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float boxWidth = 80;
        float boxX = getX() + getWidth() - boxWidth - 2;

        if (hovered(mouseX, mouseY)) {
            if (mainWindow.dropdownComponent != null && mainWindow.dropdownComponent.getChecklist() == this) {
                mainWindow.dropdownComponent = null;
            } else {
                mainWindow.dropdownComponent = new DropdownComponent(this, boxX, getY() + getHeight() - 3, boxWidth, 30);
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private String buildDisplayText(List<String> selected, FontRenderer font, int maxWidth) {
        if (selected.isEmpty()) return "...";
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;
        int ellipsisWidth = font.getStringWidth("...");
        for (int i = 0; i < selected.size(); i++) {
            String item = selected.get(i);
            String prefix = (i == 0) ? "" : ", ";
            String candidate = prefix + item;
            int candidateWidth = font.getStringWidth(candidate);
            boolean isLast = i == selected.size() - 1;
            if (currentWidth + candidateWidth + (isLast ? 0 : ellipsisWidth) <= maxWidth) {
                result.append(candidate);
                currentWidth += candidateWidth;
            } else {
                int availableWidth = maxWidth - currentWidth - (isLast ? 0 : ellipsisWidth);
                if (availableWidth <= 0) {
                    if (result.length() == 0) return "...";
                    result.append("...");
                    break;
                }
                String partial = getFittingSubstring(prefix + item, availableWidth, font);
                result.append(partial);
                if (!isLast) result.append("...");
                break;
            }
        }
        return result.toString();
    }

    private String getFittingSubstring(String text, int maxWidth, FontRenderer font) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        int low = 0, high = text.length();
        String best = "";
        while (low <= high) {
            int mid = (low + high) / 2;
            String candidate = text.substring(0, mid);
            if (font.getStringWidth(candidate) < maxWidth) {
                best = candidate;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
