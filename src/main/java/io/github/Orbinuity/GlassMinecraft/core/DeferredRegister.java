package io.github.Orbinuity.GlassMinecraft.core;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

public final class DeferredRegister<T> {
    private final Registry<T> registry;
    private final String modId;
    private final List<Entry<T>> pending = new ArrayList<>();
    private boolean bound = false;

    private record Entry<T>(String name, Supplier<? extends T> supplier, RegistryObject<T> handle) {}

    private DeferredRegister(Registry<T> registry, String modId) {
        this.registry = registry;
        this.modId = modId;
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String modId) {
        return new DeferredRegister<>(registry, modId);
    }

    public RegistryObject<T> register(String path, Supplier<? extends T> supplier) {
        if (bound) throw new IllegalStateException("Already bound to an EventBus");
        ResourceLocation id = new ResourceLocation(modId, path);
        RegistryObject<T> ro = RegistryObject.of(id, (Class<T>) Object.class);
        pending.add(new Entry<>(path, supplier, ro));
        return ro;
    }

    public void register(IEventBus bus) {
        if (bound) return;
        bound = true;
        bus.addListener(RegistryEvent.Register.class, evt -> {
            if (evt.registry() == this.registry) {
                for (Entry<T> e : pending) {
                    T value = e.supplier().get();
                    Registry.register(this.registry, new ResourceLocation(modId, e.name()), value);
                    e.handle().set(value);
                }
            }
        });
    }
}