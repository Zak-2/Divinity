package divinity.event.impl.input;

import divinity.event.base.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MouseClickEvent extends Event {

    private int mouseButton;
    private boolean pressed;

    public MouseClickEvent(int mouseButton, boolean pressed) {
        this.mouseButton = mouseButton;
        this.pressed = pressed;
    }
}