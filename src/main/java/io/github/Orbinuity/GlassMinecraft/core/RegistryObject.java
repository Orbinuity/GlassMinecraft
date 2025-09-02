package io.github.Orbinuity.GlassMinecraft.core;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

public final class RegistryObject<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Class<T> type;
    private volatile T value;

    private RegistryObject(ResourceLocation id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    static <T> RegistryObject<T> of(ResourceLocation id, Class<T> type) {
        return new RegistryObject<>(id, type);
    }

    void set(T v) { this.value = v; }

    public ResourceLocation getId() { return id; }

    public boolean isPresent() { return value != null; }

    @Override
    public T get() {
        return Objects.requireNonNull(value, "RegistryObject<" + type.getSimpleName() + "> not ready for " + id);
    }

    @Override
    public String toString() { return "RegistryObject[" + id + "]"; }
}