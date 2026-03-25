package divinity.event.base.dispatcher;


import divinity.ClientManager;
import divinity.event.base.EventHandler;
import divinity.event.base.EventListener;
import divinity.module.Module;
import divinity.utils.player.rotation.RequireRotationPriority;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;


public final class EventDispatcher<Event> implements IEventDispatcher<Event> {

    private final Map<Type, List<CallSite<Event>>> callSiteMap = new HashMap<>();
    private final Map<Type, List<EventHandler<Event>>> listenerCache = new HashMap<>();

    @Override
    public void register(final Object subscriber) {
        for (final Method method : subscriber.getClass().getDeclaredMethods()) {
            final EventListener annotation = method.getAnnotation(EventListener.class);
            if (annotation != null) {
                final Type eventType = method.getParameterTypes()[0];
                final byte priority = annotation.value();

                // Capture whether the handler requires rotation priority
                boolean requiresPriority = method.isAnnotationPresent(RequireRotationPriority.class);

                // Capture module if the subscriber is a module
                Module module = (subscriber instanceof Module) ? (Module) subscriber : null;

                addCallSite(eventType, new CallSite<>(subscriber, method, priority, requiresPriority, module));
            }
        }
        populateListenerCache();
    }

    private void addCallSite(Type eventType, CallSite<Event> callSite) {
        List<CallSite<Event>> callSites = callSiteMap.computeIfAbsent(eventType, k -> new ArrayList<>());
        callSites.add(callSite);
        callSites.sort(Comparator.comparingInt((CallSite<Event> cs) -> -cs.priority));
    }

    private void populateListenerCache() {
        listenerCache.clear();
        callSiteMap.forEach((type, callSites) -> {
            List<EventHandler<Event>> handlers = new ArrayList<>();
            for (CallSite<Event> callSite : callSites) {
                handlers.add(event -> {
                    // Skip if requires priority but module doesn't have it
                    if (callSite.requiresPriority && (callSite.module == null || !ClientManager.getInstance().getRotationHandler().isActiveModule(callSite.module))) {
                        return;
                    }
                    try {
                        callSite.method.invoke(callSite.owner, event);
                    } catch (Exception e) {
                        e.getSuppressed();
                    }
                });
            }
            listenerCache.put(type, handlers);
        });
    }

    @Override
    public void unregister(final Object subscriber) {
        callSiteMap.values().forEach(callSites -> callSites.removeIf(callSite -> callSite.owner == subscriber));
        populateListenerCache();
    }

    @Override
    public void post(final Event event) {
        List<EventHandler<Event>> listeners = listenerCache.getOrDefault(event.getClass(), Collections.emptyList());
        for (EventHandler<Event> listener : listeners) {
            listener.call(event);
        }
    }

    private static class CallSite<Event> {
        private final Object owner;
        private final Method method;
        private final byte priority;
        private final boolean requiresPriority;
        private final Module module;

        public CallSite(Object owner, Method method, byte priority, boolean requiresPriority, Module module) {
            this.owner = owner;
            this.method = method;
            this.priority = priority;
            this.requiresPriority = requiresPriority;
            this.module = module;
        }
    }
}
