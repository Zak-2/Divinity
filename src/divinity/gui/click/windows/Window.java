package divinity.gui.click.windows;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Window {

    private boolean draggable, dragging;
    private float x, y, width, height, lastX, lastY;

    public Window(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.draggable = false;
    }

    public void drawScreen(int mouseX, int mouseY) {
        if (dragging) {
            setX(mouseX + getLastX());
            setY(mouseY + getLastY());
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + 46) && draggable) {
            setLastX(getX() - mouseX);
            setLastY(getY() - mouseY);
            dragging = true;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
        }
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }

    public void handleMouseInput() {

    }

    public void keyTyped(char typedChar, int keyCode) {

    }

    public boolean hovered(int mouseX, int mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public boolean hovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX > x && mouseX < width && mouseY > y && mouseY < height;
    }
}
