package divinity.gui.click.windows.config;

import divinity.utils.MouseUtils;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Frame<O> {

    private O object;
    private int x, y, width, height, lastX, lastY;
    private boolean dragging;

    public Frame(O object, int x, int y, int width, int height) {
        this.object = object;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update(int x, int y) {
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (isDragging()) {
            setX(mouseX + getLastX());
            setY(mouseY + getLastY());
        }
        update(getX(), getY());
    }


    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + 16)) {
            setLastX(getX() - mouseX);
            setLastY(getY() - mouseY);
            setDragging(true);
        }
    }


    public void mouseReleased(int mouseX, int mouseY, int state) {
        setDragging(false);
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }

    public void handleMouseInput() {

    }

    public void keyTyped(char typedChar, int keyCode) {
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return MouseUtils.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + getHeight());
    }

}
