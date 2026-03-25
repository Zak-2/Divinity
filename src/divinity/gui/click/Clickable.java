package divinity.gui.click;

import divinity.ClientManager;
import divinity.Core;
import divinity.gui.click.windows.MainWindow;
import divinity.module.impl.render.element.GuiEditElement;
import divinity.module.impl.render.element.core.DraggableElement;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import hogoshi.Animation;
import hogoshi.util.Easings;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class Clickable extends GuiScreen implements Core {

    private final String[] buttons = new String[]{"Menu", "Widgets"};
    public UiSection section;
    private MainWindow mainWindow;
    private boolean initialized = false;

    private final Animation guiAnim = new Animation();
    private boolean closing;

    private final float guiWidth = 450;
    private final float guiHeight = 320;

    public Clickable() {
        this.section = UiSection.Menu;
    }

    @Override
    public void initialize() {
        mainWindow = new MainWindow(
                (width / 2.0f) - (guiWidth / 2.0f),
                (height / 2.0f) - (guiHeight / 2.0f),
                guiWidth,
                guiHeight
        );
        initialized = true;
    }

    @Override
    public void initGui() {
        if (!initialized) initialize();
        reposition(width, height);

        closing = false;
        guiAnim.setValue(0);
        guiAnim.animate(1, 0.45, Easings.QUINT_OUT, false);

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        guiAnim.update();
        double t = guiAnim.getValue();

        if (closing && !guiAnim.isAlive() && t <= 0.01) {
            mc.displayGuiScreen(null);
            return;
        }

        RenderUtils.drawRect(0, 0, width, height, new Color(5, 5, 10, (int) (120 * t)).getRGB());

        double scale = 0.9 + 0.1 * t;
        double yOff = (1.0 - t) * 10.0;

        GL11.glPushMatrix();
        GL11.glTranslated(width / 2.0, height / 2.0, 0);
        GL11.glScaled(scale, scale, 1);
        GL11.glTranslated(-width / 2.0, -height / 2.0 + yOff, 0);

        drawNavigation(mouseX, mouseY, (float) t);

        switch (section) {
            case Menu:
                mainWindow.drawScreen(mouseX, mouseY);
                break;
            case Widgets:
                GuiEditElement.draggableElements.forEach(draggableElement ->
                        draggableElement.drawScreen(mouseX, mouseY, partialTicks));
                break;
        }

        GL11.glPopMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawNavigation(int mouseX, int mouseY, float alphaMult) {
        float navWidth = buttons.length * 70;
        float navX = (width / 2.0f) - (navWidth / 2.0f);
        float navY = 10;

        ShaderUtils.drawRoundRect(navX, navY, navX + navWidth, navY + 18, 5, new Color(15, 15, 20, (int) (180 * alphaMult)).getRGB());

        float xOffset = navX;
        for (String button : buttons) {
            boolean isCurrent = button.equalsIgnoreCase(section.name());
            boolean hover = MouseUtils.isHovered(xOffset, navY, xOffset + 70, navY + 18);

            if (isCurrent) {
                Color main = ClientManager.getInstance().getMainColor();
                ShaderUtils.drawRoundRect(xOffset + 2, navY + 2, xOffset + 68, navY + 16, 4,
                        new Color(main.getRed(), main.getGreen(), main.getBlue(), (int) (200 * alphaMult)).getRGB());
            }

            int textColor = isCurrent ? -1 : (hover ? -1 : new Color(150, 150, 160, (int) (255 * alphaMult)).getRGB());
            Fonts.INTER_MEDIUM.get(14).drawCenteredString(button, xOffset + 35, navY + 6.5f, textColor);

            xOffset += 70;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing || guiAnim.getValue() < 0.8) return;

        float navWidth = buttons.length * 70;
        float navX = (width / 2.0f) - (navWidth / 2.0f);
        float navY = 10;

        float xOffset = navX;
        for (String button : buttons) {
            if (MouseUtils.isHovered(xOffset, navY, xOffset + 70, navY + 18)) {
                if (button.equals("Menu")) section = UiSection.Menu;
                if (button.equals("Widgets")) section = UiSection.Widgets;
                return;
            }
            xOffset += 70;
        }

        switch (section) {
            case Menu:
                mainWindow.mouseClicked(mouseX, mouseY, mouseButton);
                break;
            case Widgets:
                GuiEditElement.draggableElements.forEach(draggableElement ->
                        draggableElement.mouseClicked(mouseX, mouseY, mouseButton));
                break;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (closing) return;

        switch (section) {
            case Menu:
                mainWindow.mouseReleased(mouseX, mouseY, state);
                break;
            case Widgets:
                GuiEditElement.draggableElements.forEach(draggableElement ->
                        draggableElement.mouseReleased(mouseX, mouseY, state));
                break;
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (closing) return;

        switch (section) {
            case Menu:
                mainWindow.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                break;
            case Widgets:
                break;
            case CONFIGS:
                break;
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (closing) return;

        switch (section) {
            case Menu:
                mainWindow.handleMouseInput();
                break;
            case Widgets:
                GuiEditElement.draggableElements.forEach(DraggableElement::handleMouseInput);
                break;
        }

        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (!closing) {
                closing = true;
                guiAnim.animate(0, 0.3, Easings.QUINT_IN, false);
            }
            return;
        }

        if (closing) return;

        switch (section) {
            case Menu:
                mainWindow.keyTyped(typedChar, keyCode);
                break;
            case Widgets:
                GuiEditElement.draggableElements.forEach(draggableElement ->
                        draggableElement.keyTyped(typedChar, keyCode));
                break;
        }

        super.keyTyped(typedChar, keyCode);
    }

    public void reposition(int w, int h) {
        if (initialized) {
            mainWindow.setX((w / 2.0f) - (guiWidth / 2.0f));
            mainWindow.setY((h / 2.0f) - (guiHeight / 2.0f));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void shutdown() {
    }
}