package divinity.gui.click.windows;

import divinity.gui.click.components.Component;
import divinity.gui.click.components.module.*;
import divinity.module.Module;
import divinity.module.property.Property;
import divinity.module.property.impl.*;
import divinity.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModulePropertyWindow extends Window {

    public Module module;
    public MainWindow mainWindow;
    private int scrollY = 0;
    private int totalContentHeight = 0;
    private List<Component> components;

    public ModulePropertyWindow(Module module, MainWindow mainWindow, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.module = module;
        this.mainWindow = mainWindow;
        addComponents(module);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        totalContentHeight = components.stream()
                .filter(Component::isVisible)
                .mapToInt(c -> (int) (c.getHeight() + c.getGap()))
                .sum();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.prepareScissorbox(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        int currentY = -scrollY;
        for (Component component : components) {
            if (component.isVisible()) {
                int scrollbarSpace = 4;
                component.setX((int) getX());
                component.setY((int) getY() + currentY);
                component.setWidth(getWidth() - scrollbarSpace);
                component.drawScreen(mouseX, mouseY);
                currentY += component.getHeight() + component.getGap();
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        int visibleHeight = (int) getHeight();
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        if (maxScroll > 0) {
            int scrollbarWidth = 2;
            int scrollbarX = (int) (getX() + getWidth() - scrollbarWidth);
            float scrollProgress = (float) scrollY / maxScroll;

            RenderUtils.drawRect(scrollbarX, getY(), scrollbarX + scrollbarWidth, getY() + visibleHeight, new Color(45, 45, 45).getRGB());
            int thumbHeight = Math.max(30, (int) (visibleHeight * ((float) visibleHeight / totalContentHeight)));
            int thumbY = (int) (getY() + scrollProgress * (visibleHeight - thumbHeight));
            RenderUtils.drawRect(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, new Color(100, 100, 100).getRGB());
        }
        super.drawScreen(mouseX, mouseY);
    }


    public void onScroll(int mouseWheel) {
        if (totalContentHeight > getHeight()) {
            scrollY = MathHelper.clamp_int(scrollY + (mouseWheel > 0 ? -20 : 20), 0, Math.max(0, totalContentHeight - (int) getHeight()));
        }
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isMouseOverWindow()) {
            float minY = getY();
            float maxY = getY() + getHeight();

            for (Component component : components) {
                if (component.isVisible()) {
                    float componentTop = component.getY();
                    float componentBottom = component.getY() + component.getHeight();

                    if (componentBottom >= minY && componentTop <= maxY) {
                        component.mouseClicked(mouseX, mouseY, mouseButton);
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isMouseOverWindow()) {
            float minY = getY();
            float maxY = getY() + getHeight();

            for (Component component : components) {
                if (component.isVisible()) {
                    float componentTop = component.getY();
                    float componentBottom = component.getY() + component.getHeight();

                    if (componentBottom >= minY && componentTop <= maxY) {
                        component.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                    }
                }
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (isMouseOverWindow()) {
            float minY = getY();
            float maxY = getY() + getHeight();

            for (Component component : components) {
                if (component.isVisible()) {
                    float componentTop = component.getY();
                    float componentBottom = component.getY() + component.getHeight();

                    if (componentBottom >= minY && componentTop <= maxY) {
                        component.mouseReleased(mouseX, mouseY, state);
                    }
                }
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        components.forEach(c -> c.keyTyped(typedChar, keyCode));
        super.keyTyped(typedChar, keyCode);
    }

    private void addComponents(Module module) {
        this.components = new ArrayList<>();
        int offset = 0;
        for (Property property : module.getProperties()) {
            if (property instanceof BooleanProperty) {
                components.add(new CheckboxComponent((BooleanProperty) property, getX(), getY() + offset, getWidth(), 16, 2));
                offset += 18;
            } else if (property instanceof ModeProperty) {
                components.add(new TextSpinnerComponent((ModeProperty) property, getX(), getY() + offset, getWidth(), 16, 2));
                offset += 18;
            } else if (property instanceof MultiSelectProperty) {
                components.add(new ChecklistComponent((MultiSelectProperty) property, mainWindow, getX(), getY() + offset, getWidth(), 16, 2));
                offset += 18;
            } else if (property instanceof NumberProperty) {
                components.add(new NumberSliderComponent((NumberProperty) property, getX(), getY() + offset, getWidth(), 18, 2));
                offset += 20;
            } else if (property instanceof ColorProperty) {
                components.add(new ColorPickerComponent((ColorProperty) property, mainWindow, getX(), getY() + offset, getWidth(), 16, 2));
                offset += 18;
            }
        }
    }

    public boolean isMouseOverWindow() {
        Minecraft mc = Minecraft.getMinecraft();

        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();

        int scaledMouseX = mouseX * mc.currentScreen.width / mc.displayWidth;
        int scaledMouseY = mc.currentScreen.height - (mouseY * mc.currentScreen.height / mc.displayHeight);

        return scaledMouseX >= getX() && scaledMouseX <= getX() + getWidth() &&
                scaledMouseY >= getY() && scaledMouseY <= getY() + getHeight();
    }
}