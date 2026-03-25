package divinity.event.impl.minecraft;

import divinity.event.base.Event;

public class TimerSpeedEvent extends Event {

    public static double lastTimerSpeed;
    private double timerSpeed;

    public TimerSpeedEvent(double timerSpeed) {
        this.timerSpeed = timerSpeed;
    }

    public double getTimerSpeed() {
        return lastTimerSpeed = timerSpeed;
    }

    public void setTimerSpeed(double timerSpeed) {
        this.timerSpeed = timerSpeed;
    }
}
