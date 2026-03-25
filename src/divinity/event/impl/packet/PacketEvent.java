package divinity.event.impl.packet;

import divinity.event.base.Event;
import divinity.event.base.types.EventType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

@Getter
@Setter
public class PacketEvent extends Event {
    private final EventType eventType;
    private Packet<?> packet;

    public PacketEvent(EventType eventType, Packet<?> packet) {
        this.packet = packet;
        this.eventType = eventType;
    }

    public boolean isSending() {
        return eventType.equals(EventType.SEND);
    }

    public boolean isReceiving() {
        return eventType.equals(EventType.RECEIVE);
    }
}