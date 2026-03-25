package divinity.event.base;

import divinity.event.base.types.EventType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

@Getter
@Setter
public class Event {
    private final EventType type;
    public Minecraft mc = Minecraft.getMinecraft();
    private boolean cancelled;

    public Event() {
        this.type = EventType.PRE;
        cancelled = false;
    }

    public Event(final EventType type) {
        this.type = EventType.PRE;
        cancelled = false;
    }
}