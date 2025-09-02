package io.github.Orbinuity.GlassMinecraft.core;

public interface IEventBus {
    <E> void addListener(Class<E> eventType, EventListener<E> listener);
    void post(Object event);

    @FunctionalInterface
    interface EventListener<E> {
        void onEvent(E e);
    }
}