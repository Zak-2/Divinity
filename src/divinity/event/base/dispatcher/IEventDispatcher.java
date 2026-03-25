package divinity.event.base.dispatcher;

public interface IEventDispatcher<Event> {

    void register(final Object subscriber);

    void unregister(final Object subscriber);

    void post(final Event event);

}