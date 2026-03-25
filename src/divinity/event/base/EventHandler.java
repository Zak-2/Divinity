package divinity.event.base;

@FunctionalInterface
public interface EventHandler<Event> {
    void call(Event event);
}