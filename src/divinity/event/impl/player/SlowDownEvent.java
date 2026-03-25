package divinity.event.impl.player;

import divinity.event.base.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SlowDownEvent extends Event {

    public float forward;
    public float strafe;
    private boolean forceSlowdown;

    public SlowDownEvent(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }
}