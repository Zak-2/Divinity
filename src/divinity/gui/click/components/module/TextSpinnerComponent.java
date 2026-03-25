package divinity.gui.click.components.module;


import divinity.ClientManager;
import divinity.gui.click.components.Component;
import divinity.module.property.impl.ModeProperty;
import divinity.utils.AnimationUtils;
import divinity.utils.font.Fonts;

import java.awt.*;
import java.util.stream.IntStream;

public class TextSpinnerComponent extends Component {

    private final ModeProperty property;
    private float textAnimation = 0;

    public TextSpinnerComponent(ModeProperty property, float x, float y, float width, float height, int gap) {
        super(x, y, width, height, gap);
        this.property = property;
    }

    public static int getNumber(String name, String[] array) {
        if (array == null || name == null) return -1;
        return IntStream.range(0, array.length)
                .filter(i -> name.equals(array[i]))
                .map(i -> i + 1).findFirst().orElse(-1);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        int all = property.getModes().length;
        String count = getNumber(property.getValue(), property.getModes()) + "/" + all;
        String val = property.getValue();

        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(property.getName(), getX() + 1, getY() + 5.5f, -1);

        float valWidth = Fonts.INTER_MEDIUM.get(15).getStringWidth(val);
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(val, getX() + getWidth() - valWidth - 2, getY() + 5.5f, ClientManager.getInstance().getMainColor().getRGB());

        float countWidth = Fonts.INTER_MEDIUM.get(12).getStringWidth(count);
        Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(count, getX() + getWidth() - valWidth - countWidth - 6, getY() + 6.5f, new Color(120, 120, 130).getRGB());

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                property.next();
            } else if (mouseButton == 1) {
                property.previous();
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
