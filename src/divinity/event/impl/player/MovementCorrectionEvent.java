package divinity.event.impl.player;

import divinity.event.base.Event;

public class MovementCorrectionEvent extends Event {

    private float yaw;

    public MovementCorrectionEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
