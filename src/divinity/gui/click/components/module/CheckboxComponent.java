package divinity.gui.click.components.module;


import divinity.ClientManager;
import divinity.gui.click.components.Component;
import divinity.module.property.impl.BooleanProperty;
import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.shader.Shader;

import java.awt.*;

public class CheckboxComponent extends Component {

    private final BooleanProperty property;
    private float stateAnimation = 4;

    public CheckboxComponent(BooleanProperty property, float x, float y, float width, float height, int gap) {
        super(x, y, width, height, gap);
        this.property = property;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        stateAnimation = AnimationUtils.getAnimationState(stateAnimation, property.getValue() ? 7 : 0, 50f);
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(property.getName(), getX() + 1, getY() + 5.5f, -1);
        ShaderUtils.drawRoundRect(getX() + getWidth() - 18, getY() + 4, getX() + getWidth() - 2, getY() + getHeight() - 4, 0.5f, new Color(15, 15, 20, 200).getRGB());
        ShaderUtils.drawRoundRect(getX() + getWidth() - 17 + stateAnimation, getY() + 5, getX() + getWidth() - 10 + stateAnimation, getY() + getHeight() - 5, 0.5f, property.getValue() ? ClientManager.getInstance().getMainColor().getRGB() : new Color(21, 20, 27).getRGB());
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            property.toggle();
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
