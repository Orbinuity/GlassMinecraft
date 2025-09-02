package io.github.Orbinuity.GlassMinecraft.core;

import net.minecraft.core.Registry;

public final class RegistryEvent {
    private RegistryEvent() {}

    public static final class Register<T> {
        private final Registry<T> registry;
        public Register(Registry<T> registry) { this.registry = registry; }
        public Registry<T> registry() { return registry; }
    }

    public static final class FreezeAll {}
}