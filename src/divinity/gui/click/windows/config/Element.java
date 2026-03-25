package divinity.gui.click.windows.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Element<O, P> {

    private O object;
    private P parent;
    private int x, y, width, height;

    public Element(O object, P parent, int x, int y, int width, int height) {
        this.object = object;
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update(int x, int y) {
        setX(x);
        setY(y);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }


    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }


    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }

    public void keyTyped(char typedChar, int keyCode) {
    }

    public void handleMouseInput() {

    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public boolean isVisible() {
        return true;
    }

}
