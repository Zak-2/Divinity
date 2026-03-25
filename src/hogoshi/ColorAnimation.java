package hogoshi;

import java.awt.*;

public final class ColorAnimation {
    private final Animation redPart = new Animation();
    private final Animation greenPart = new Animation();
    private final Animation bluePart = new Animation();
    private final Animation alphaPart = new Animation();

    public void update() {
        this.redPart.update();
        this.greenPart.update();
        this.bluePart.update();
        this.alphaPart.update();
    }

    public boolean isAlive() {
        return this.redPart.isAlive() && this.greenPart.isAlive() && this.bluePart.isAlive();
    }

    public void animate(final Color color, final float duration) {
        this.animate(color, duration, false);
    }

    public void animate(final Color color, final float duration, final boolean safe) {
        update();
        this.redPart.animate(color.getRed(), duration, safe);
        this.greenPart.animate(color.getGreen(), duration, safe);
        this.bluePart.animate(color.getBlue(), duration, safe);
        this.alphaPart.animate(color.getAlpha(), duration, safe);
    }

    public Color getColor() {
        return new Color((int) this.redPart.getValue(), (int) this.greenPart.getValue(), (int) this.bluePart.getValue(), (int) this.alphaPart.getValue());
    }
}
