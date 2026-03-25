package divinity.gui.click.components.module;


import divinity.gui.click.components.Component;
import divinity.gui.click.windows.MainWindow;
import divinity.module.property.impl.ColorProperty;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;

import java.awt.*;

public class ColorPickerComponent extends Component {

    public final ColorProperty property;
    public final MainWindow mainWindow;
    public boolean expanded = false;

    public ColorPickerComponent(ColorProperty property, MainWindow mainWindow, float x, float y, float width, float height, int gap) {
        super(x, y, width, height, gap);
        this.property = property;
        this.mainWindow = mainWindow;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(property.getName(), getX() + 1, getY() + 5.5f, -1);

        ShaderUtils.drawRoundRect(getX() + getWidth() - 17, getY() + 2.5f, getX() + getWidth() - 1, getY() + 12.5f, 1f, new Color(30, 30, 30).getRGB());
        ShaderUtils.drawRoundRect(getX() + getWidth() - 16, getY() + 3.5f, getX() + getWidth() - 2, getY() + 11.5f, 1f,property.getValue().getRGB());

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            if (mouseX >= getX() + getWidth() - 30 && mouseX <= getX() + getWidth() - 20) {
                // Toggle expanded state
                expanded = !expanded;
                if (expanded) {
                    mainWindow.pickerContainer = new ColorPickerContainer(this, getX() + getWidth() - 220, getY() + getHeight() - 3, 200, 200);
                } else {
                    mainWindow.pickerContainer = null;
                }
            } else if (mouseX >= getX() + getWidth() - 17 && mouseX <= getX() + getWidth() - 1) {
                // Open color picker on color preview click
                mainWindow.pickerContainer = new ColorPickerContainer(this, getX() + getWidth() - 220, getY() + getHeight() - 3, 200, 200);
                expanded = true;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }
}