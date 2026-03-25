package divinity.utils.math;

public class TimerUtil {

    private long lastMS;

    public TimerUtil() {
        reset();
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean hasTimeElapsed(long millis) {
        return getElapsedTime() >= millis;
    }
}
