package divinity.gui.click.windows.config;


import divinity.ClientManager;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;

import java.awt.*;

public class ConfigEntry extends Element<String, ConfigFrame> {


    public ConfigEntry(String object, ConfigFrame parent, int x, int y, int width, int height) {
        super(object, parent, x, y, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean hover = isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), hover ? ClientManager.getInstance().getMainColor().brighter().getRGB() : getParent().currentConfig == this ? ClientManager.getInstance().getMainColor().getRGB() : new Color(0, 0, 0, 0).getRGB());
        String displayName = getObject();
        if (displayName.endsWith(".json") && displayName.length() > 5) {
            displayName = displayName.substring(0, displayName.length() - 5);
        }
        Fonts.INTER_MEDIUM.get(14).drawStringWithShadow(displayName, getX() + 2, getY() + 3, -1);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            if (mouseButton == 0) {
                getParent().currentConfig = this;
            } else if (mouseButton == 2 || mouseButton == 1) {
                // load
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
