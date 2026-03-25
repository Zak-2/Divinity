package divinity.gui.click.components.module;

import divinity.ClientManager;
import divinity.gui.click.components.Component;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.util.MathHelper;

import java.awt.*;
import java.util.Objects;

public class NumberSliderComponent extends Component {

    private final NumberProperty<Number> property;
    private boolean dragging;
    private float slideAnimation;

    public NumberSliderComponent(NumberProperty<Number> property, float x, float y, float width, float height, int gap) {
        super(x, y, width, height, gap);
        this.property = property;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Number valueObj = property.getValue();
        String currentValue;

        if (valueObj instanceof Integer || valueObj instanceof Long) {
            currentValue = String.valueOf(valueObj.intValue());
        } else {
            currentValue = String.format("%.2f", valueObj.floatValue());
        }

        float min = property.getMin().floatValue();
        float max = property.getMax().floatValue();
        float value = property.getValue().floatValue();
        float length = (value - min) / (max - min) * (getWidth() - 1);
        slideAnimation = AnimationUtils.getAnimationState(slideAnimation, length, 200f);

        ShaderUtils.drawRoundRect(getX(), getY() + getHeight() - 4, getX() + getWidth(), getY() + getHeight(), 0.5f, new Color(35, 35, 35).getRGB());
        ShaderUtils.drawRoundRect(getX() + .5f, getY() + getHeight() - 3.5f, getX() + .5f + ((int) slideAnimation), getY() + getHeight() - .5f,0.5f, ClientManager.getInstance().getMainColor().getRGB());
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(property.getName(), getX() + 1, getY() + 5.5f, -1);
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(currentValue, getX() + getWidth() - Fonts.INTER_MEDIUM.get(14).getStringWidth(currentValue) - 2, getY() + 5.5f, -1);
        if (dragging) {
            float newValue = (mouseX - getX()) / getWidth() * (max - min) + min;
            property.setValue(MathHelper.clamp_float(newValue, min, max));
        }

        if (Objects.equals(property.getValue(), property.getMax()) || Objects.equals(property.getValue(), property.getMin())) {
            dragging = false;
        }
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            dragging = true;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
