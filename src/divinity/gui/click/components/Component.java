package divinity.gui.click.components;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Component {

    private float x, y, width, height, gap;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Component(float x, float y, float width, float height, int gap) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.gap = gap;
    }

    public void drawScreen(int mouseX, int mouseY) {

    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

    }

    public void mouseReleased(int mouseX, int mouseY, int state) {

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

    public boolean isVisible() {
        return true;
    }

}
