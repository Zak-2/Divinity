package hogoshi;

import hogoshi.util.Easing;

public final class Tween {

    private float v0;

    public Tween(float v0) {
        this.v0 = v0;
    }

    public float tween(float target, float dt, float v2, Easing easing) {
        float theta = target - this.v0;
        float inc = dt / v2;

        if (Math.abs(theta) > inc) {
            this.v0 += inc * Math.signum(theta);
        } else {
            this.v0 = target;
        }

        this.v0 = Math.max(0.0f, Math.min(1.0f, this.v0));

        return (float) easing.ease(this.v0);
    }
}