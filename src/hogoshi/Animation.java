package hogoshi;

import hogoshi.util.Easing;
import hogoshi.util.Easings;

/**
 * Main class
 */
public class Animation {

    /**
     * System.currentTimeMillis() from last animation start
     */
    private long start;
    /**
     * Last/Current animate method duration
     */
    private double duration;
    /**
     * Value from which animation is started
     */
    private double fromValue;
    /**
     * Value to which animation goes
     */
    private double toValue;
    /**
     * Returns current animation value (better usage: getValue())
     */
    private double value;

    /**
     * Animation type
     */
    private Easing easing;

    /**
     * Outputs animation things
     */
    private boolean debug;

    /**
     * Main method, use to animate value to something.
     *
     * @param valueTo  toValue, value to which animation will go
     * @param duration duration, with which animation will animate
     */
    public Animation animate(double valueTo, double duration) {
        return animate(valueTo, duration, Easings.NONE, false);
    }

    /**
     * Main method, use to animate value to something.
     *
     * @param valueTo  toValue, value to which animation will go
     * @param duration duration, with which animation will animate
     * @param easing   animation type, like formula for animation
     */
    public Animation animate(double valueTo, double duration, Easing easing) {
        return animate(valueTo, duration, easing, false);
    }

    /**
     * Main method, use to animate value to something.
     *
     * @param valueTo  toValue, value to which animation will go
     * @param duration duration, with which animation will animate
     * @param bezier   custom easing instance, like formula of easing
     */

    /**
     * Main method, use to animate value to something.
     *
     * @param valueTo  toValue, value to which animation will go
     * @param duration duration, with which animation will animate
     * @param safe     means will it update when animation isAlive or with the same targetValue
     */
    public Animation animate(double valueTo, double duration, boolean safe) {
        return animate(valueTo, duration, Easings.NONE, safe);
    }

    /**
     * Main method, use to animate value to something
     *
     * @param valueTo  toValue, value to which animation will go
     * @param duration duration, with which animation will animate
     * @param easing   animation type, like formula for animation
     * @param safe     means will it update when animation isAlive or with the same targetValue
     */
    public Animation animate(double valueTo, double duration, Easing easing, boolean safe) {
        if (check(safe, valueTo)) {
            if (isDebug()) System.out.println("Animate cancelled due to target val equals from val");
            return this;
        }
        setEasing(easing);
        setDuration(duration * 1000);
        setStart(System.currentTimeMillis());
        setFromValue(getValue());
        setToValue(valueTo);
        return this;
    }


    /**
     * Important method, use to update value. If u won't update animation, animation won't work.
     *
     * @return returns if animation isAlive()
     */
    public boolean update() {
        boolean alive = isAlive();

        if (alive) {
            setValue(interpolate(getFromValue(), getToValue(), getEasing().ease(calculatePart())));
        } else {
            setStart(0);
            setValue(getToValue());
        }

        return alive;
    }

    /**
     * Use if u want check if animation is animating
     *
     * @return returns if animation is animating
     */
    public boolean isAlive() {
        return !isDone();
    }

    /**
     * Use if u want check if animation is not animating
     *
     * @return returns if animation is animating
     */
    public boolean isDone() {
        return calculatePart() >= 1.0;
    }

    /**
     * Use if u want to get the current part of animation (from 0.0 to 1.0, like 0% and 100%)
     *
     * @return returns animation part
     */
    public double calculatePart() {
        return (double) (System.currentTimeMillis() - getStart()) / getDuration();
    }

    public boolean check(boolean safe, double valueTo) {
        return safe && isAlive() && (valueTo == getFromValue() || valueTo == getToValue() || valueTo == getValue());
    }

    /**
     * Basic interpolation formula
     */
    public double interpolate(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getFromValue() {
        return fromValue;
    }

    public void setFromValue(double fromValue) {
        this.fromValue = fromValue;
    }

    public double getToValue() {
        return toValue;
    }

    public void setToValue(double toValue) {
        this.toValue = toValue;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
