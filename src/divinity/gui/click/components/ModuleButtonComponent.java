package divinity.gui.click.components;

import divinity.ClientManager;
import divinity.gui.click.windows.MainWindow;
import divinity.module.Module;
import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class ModuleButtonComponent extends Component {

    private final MainWindow mainWindow;
    public Module module;
    private float hoverAnimation = 4;
    private float stateAnimation = 4;
    private float alphaAnimation = 0;
    private float selectorAnimation = 0;
    private boolean binding;

    public ModuleButtonComponent(Module module, MainWindow mainWindow, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.module = module;
        this.mainWindow = mainWindow;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        String modName = binding ? "Press a key..." : module.getName();
        boolean hover = hovered(mouseX, mouseY);
        float position = hover ? 4 : 0;
        hoverAnimation = AnimationUtils.getAnimationState(hoverAnimation, position, 20f);
        stateAnimation = AnimationUtils.getAnimationState(stateAnimation, module.isState() ? 7 : 0, 50f);
        alphaAnimation = AnimationUtils.getAnimationState(alphaAnimation, hover ? mainWindow.selectedModule == this ? 0 : 255 : 0, 800);
        selectorAnimation = AnimationUtils.getAnimationState(selectorAnimation, mainWindow.selectedModule == this ? 255 : 0, 800);
       // RenderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 10, new Color(16, 16, 21, (int) selectorAnimation).getRGB());
        //  RenderUtils.drawRoundRect(getX() + .5f, getY() + .5f, getX() + getWidth() - .5f, getY() + getHeight() - .5f, 10, new Color(11, 10, 15, (int) selectorAnimation).getRGB());
        if (mainWindow.selectedModule == this) {
            RenderUtils.drawHorizontalLine(getX() + Fonts.INTER_MEDIUM.get(15).getStringWidth(modName) + hoverAnimation + 6, getX() + getWidth() - 22, getY() + (getHeight() / 2), .5f, new Color(ClientManager.getInstance().getMainColor().getRed(), ClientManager.getInstance().getMainColor().getGreen(), ClientManager.getInstance().getMainColor().getBlue(), (int) selectorAnimation).getRGB());
        }
        ShaderUtils.drawRoundRect(getX() + getWidth() - 18, getY() + 4, getX() + getWidth() - 2, getY() + getHeight() - 4, 0.5f,new Color(15, 15, 20, 200).getRGB());
        ShaderUtils.drawRoundRect(getX() + getWidth() - 17 + stateAnimation, getY() + 5, getX() + getWidth() - 10 + stateAnimation, getY() + getHeight() - 5, 0.5f,module.isState() ? ClientManager.getInstance().getMainColor().getRGB() : new Color(21, 20, 27).getRGB());
        RenderUtils.drawGradientRectHorizontal(getX() + Fonts.INTER_MEDIUM.get(15).getStringWidth(modName) + 10,
                getY() + (getHeight() / 2), getX() + getWidth() - 22, getY() + (getHeight() / 2) + .5f,
                new Color(9, 9, 14, (int) alphaAnimation).getRGB(),
                new Color(ClientManager.getInstance().getMainColor().getRed(),
                        ClientManager.getInstance().getMainColor().getGreen(), ClientManager.getInstance().getMainColor().getBlue(), (int) alphaAnimation).getRGB());
        Fonts.INTER_MEDIUM.get(15).drawStringWithShadow(modName, getX() + 2 + hoverAnimation, getY() + 5.5f, -1);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 1) {
                mainWindow.selectedModule = this;
            } else if (mouseButton == 2) {
                binding = !binding;
            }
        } else {
            if (binding) {
                binding = false;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == Keyboard.KEY_DELETE) {
                module.setKeyBind(0);
            } else {
                module.setKeyBind(keyCode);
            }
            binding = false;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
