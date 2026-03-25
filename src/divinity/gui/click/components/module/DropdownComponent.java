package divinity.gui.click.components.module;

import divinity.ClientManager;
import divinity.gui.click.components.Component;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import divinity.utils.math.TimerUtil;

import java.awt.*;

public class DropdownComponent extends Component {

    private final ChecklistComponent component;
    private final TimerUtil timerUtils;

    public DropdownComponent(ChecklistComponent component, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.component = component;
        timerUtils = new TimerUtil();
    }

    public ChecklistComponent getChecklist() {
        return component;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float totalHeight = component.property.getValue().length * 10;

        int borderColor = new Color(35, 35, 45).getRGB();
        int bgColor = new Color(15, 15, 20, 200).getRGB();

        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + totalHeight, 3, borderColor);
        ShaderUtils.drawRoundRect(getX() + 0.5f, getY() + 0.5f, getX() + getWidth() - 0.5f, getY() + totalHeight - 0.5f, 3, bgColor);

        int offset = 0;
        for (String s : component.property.getValue()) {
            float itemY = getY() + offset;
            boolean isHovered = hovered(mouseX, mouseY, getX(), itemY, getX() + getWidth(), itemY + 10);
            boolean isSelected = component.property.isSelected(s);

            if (isHovered) {
                Color accent = ClientManager.getInstance().getMainColor();
                int hoverColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40).getRGB();
                ShaderUtils.drawRoundRect(getX() + 1, itemY + 1, getX() + getWidth() - 1, itemY + 9, 2, hoverColor);
            }

            int textColor = isSelected ? ClientManager.getInstance().getMainColor().getRGB() :
                    (isHovered ? -1 : new Color(160, 160, 170).getRGB());

            Fonts.INTER_MEDIUM.get(13).drawStringWithShadow(s, getX() + 4, itemY + 4, textColor);
            offset += 10;
        }

        setHeight(offset);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY, getX(), getY() - 10, getX() + getWidth(), getY() + (component.property.getValue().length * 10))) {
            int offset = 0;
            for (String s : component.property.getValue()) {
                if (hovered(mouseX, mouseY, getX(), getY() + offset, getX() + getWidth(), getY() + offset + 10)) {
                    component.property.toggle(s);
                }
                offset += 10;
            }
            if (hovered(mouseX, mouseY, getX(), getY() - 10, getX() + getWidth(), getY()) && timerUtils.hasTimeElapsed(250)) {
                component.mainWindow.dropdownComponent = null;
            }
        } else {
            component.mainWindow.dropdownComponent = null;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}