package divinity.event.impl.minecraft;

import divinity.event.base.Event;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class UpdateEvent extends Event {

    public float yaw;
    public float pitch;
    public boolean post;

    public UpdateEvent(float currentYaw, float currentPitch) {
        this.yaw = currentYaw;
        this.pitch = currentPitch;
    }

    public boolean isPre() {
        return !this.post;
    }
}
