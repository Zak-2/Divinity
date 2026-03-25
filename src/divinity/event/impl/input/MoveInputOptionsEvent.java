package divinity.event.impl.input;

import divinity.event.base.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MoveInputOptionsEvent extends Event {
    // the traditional boolean flags
    private boolean jumping, sneaking;
    private boolean fixMovement, roundInput;

    // new: actual movement amounts
    private float forward, strafe;

    public MoveInputOptionsEvent(float forward,
                                 float strafe,
                                 boolean jumping,
                                 boolean sneaking,
                                 boolean fixMovement,
                                 boolean roundInput) {
        this.forward = forward;
        this.strafe = strafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.fixMovement = fixMovement;
        this.roundInput = roundInput;
    }
}
