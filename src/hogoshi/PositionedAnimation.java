package hogoshi;

import hogoshi.util.Easing;

public final class PositionedAnimation {

    private final Animation xPosAnimation = new Animation();
    private final Animation yPosAnimation = new Animation();

    public void animate(final float posX, final float posY, final double time, final Easing easing) {
        this.xPosAnimation.animate(posX, time, easing, true);
        this.yPosAnimation.animate(posY, time, easing, true);
        this.xPosAnimation.update();
        this.yPosAnimation.update();
    }

    public Animation getXPosAnimation() {
        return xPosAnimation;
    }

    public Animation getYPosAnimation() {
        return yPosAnimation;
    }
}