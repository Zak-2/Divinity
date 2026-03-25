package divinity.gui.click.windows.config;


import divinity.ClientManager;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;

import java.awt.*;

public class TabButton extends Element<String, ConfigFrame> {


    public TabButton(String object, ConfigFrame parent, int x, int y, int width, int height) {
        super(object, parent, x, y, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawBorderedRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 1, new Color(11, 10, 15).getRGB(), getParent().getCurrentTab() == this ? ClientManager.getInstance().getMainColor().getRGB() : new Color(16, 16, 21).getRGB());
        Fonts.INTER_MEDIUM.get(14).drawCenteredStringWithShadow(getObject(), getX() + getWidth() / 2, getY() + 5, /*getParent().getCurrentTab() == this ? new Color(121, 105, 229).getRGB() : */-1);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (getParent().getCurrentTab() != this) {
                getParent().scrollOffset = 0;
                getParent().setCurrentTab(this);
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
