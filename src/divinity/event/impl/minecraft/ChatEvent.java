package divinity.event.impl.minecraft;

import divinity.event.base.Event;
import lombok.Getter;

@Getter
public class ChatEvent extends Event {
    private final String message;

    public ChatEvent(String message) {
        this.message = message;
    }
}