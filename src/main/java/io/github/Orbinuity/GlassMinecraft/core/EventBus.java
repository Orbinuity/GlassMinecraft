package io.github.Orbinuity.GlassMinecraft.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EventBus implements IEventBus {
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    public <E> void addListener(Class<E> eventType, EventListener<E> listener) {
        listeners.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(listener);
    }

    @Override
    public void post(Object event) {
        Class<?> type = event.getClass();
        listeners.forEach((registeredType, list) -> {
            if (registeredType.isAssignableFrom(type)) {
                for (EventListener<?> l : List.copyOf(list)) {
                    @SuppressWarnings("unchecked")
                    EventListener<Object> cast = (EventListener<Object>) l;
                    cast.onEvent(event);
                }
            }
        });
    }
}