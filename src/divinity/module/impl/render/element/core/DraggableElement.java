package divinity.module.impl.render.element.core;

import divinity.module.impl.render.element.GuiEditElement;
import divinity.module.property.Property;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DraggableElement {
    public static final Minecraft mc = Minecraft.getMinecraft();
    private final List<Property> properties;
    private String name;
    private int x, y, width, height, lastX, lastY;
    private boolean dragging;
    private boolean enabled;

    private float percentX;

    public DraggableElement(int x, int y, int width, int height) {
        this.name = "";
        this.y = y;
        this.width = width;
        this.height = height;
        this.properties = new ArrayList<>();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setPercentX((float) x / sr.getScaledWidth());
        updateAbsolutePosition();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        updateAbsolutePosition();
        if (getX() < 0) setX(0);
        if (getY() < 0) setY(0);
        if (getX() + getWidth() > sr.getScaledWidth()) setX(sr.getScaledWidth() - getWidth());
        if (getY() + getHeight() > sr.getScaledHeight()) setY(sr.getScaledHeight() - getHeight());
        if (isDragging()) {
            setX(mouseX + getLastX());
            setY(mouseY + getLastY());
            updatePercentage();
        }
        if (Minecraft.getMinecraft().currentScreen instanceof GuiEditElement) {
            RenderUtils.drawRectOutline(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 1, -1);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + 16)) {
            if (mouseButton == 0) {
                setLastX(getX() - mouseX);
                setLastY(getY() - mouseY);
                setDragging(true);
            } else if (mouseButton == 2) {
                setEnabled(!isEnabled());
            }
        }
    }

    public void handleMouseInput() {

    }

    public void keyTyped(char typedChar, int keyCode) {

    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    private void updatePercentage() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setPercentX((float) getX() / sr.getScaledWidth());
    }

    private void updateAbsolutePosition() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setX((int) (getPercentX() * sr.getScaledWidth()));
    }
}
