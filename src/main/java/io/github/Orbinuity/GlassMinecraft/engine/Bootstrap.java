package io.github.Orbinuity.GlassMinecraft.engine;

import io.github.Orbinuity.GlassMinecraft.annotations.Mod;
import io.github.Orbinuity.GlassMinecraft.core.*;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class Bootstrap {
    public static File modsDir;
    public static File resourcePacksDir;
    public static File gameDir;
    public static File glassTempFolder;
    public static File dataPacksTempFolder;
    public static final List<String> modIds = new ArrayList<>();

    public Bootstrap(File modsDir, File resourcePacksDir, File gameDir) {
        this.modsDir = modsDir;
        this.resourcePacksDir = resourcePacksDir;
        this.gameDir = gameDir;
        this.glassTempFolder = new File(gameDir, "glassTemp");
        this.dataPacksTempFolder = new File(glassTempFolder, "datapacks");
    }

    public static void bootStrap() {
        System.out.println("[Engine] Booting mini-Forge (Minecraft-backed registries)...");

        // 1) Build the global bus
        IEventBus bus = new EventBus();

        // 2) Use Minecraft's registries
        Registry<?> BLOCKS = BuiltInRegistries.BLOCK; // Registries.BLOCK is the Minecraft block registry
        Registry<?> ITEMS  = BuiltInRegistries.ITEM;  // Registries.ITEM is the Minecraft item registry

        // 3) Ensure mods dir exists and load mods
        loadAnnotatedMods(bus);

        // 4) Fire registry events (Forge fires per-registry events)
        System.out.println("[Engine] Posting Register<Block>...");
        bus.post(new RegistryEvent.Register<>(BuiltInRegistries.BLOCK)); // event carries Minecraft registry
        System.out.println("[Engine] Posting Register<Item>...");
        bus.post(new RegistryEvent.Register<>(BuiltInRegistries.ITEM));

        // 5) Freeze step (we still post FreezeAll for listeners)
        bus.post(new RegistryEvent.FreezeAll());

        System.out.println("[Engine] Done.");
    }

    private static void loadAnnotatedMods(IEventBus bus) {
        if (!modsDir.exists()) {
            if (modsDir.mkdirs()) {
                System.out.println("[Engine] Created mods folder: " + modsDir.getAbsolutePath());
            } else {
                System.err.println("[Engine] Failed to create mods folder! No mods will be loaded.");
                return;
            }
        }

        java.io.File[] jarFiles = modsDir.listFiles(f -> f.getName().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("[Engine] No mod jars found in " + modsDir.getAbsolutePath());
            return;
        }

        for (var jarFile : jarFiles) {
            try {
                var url = jarFile.toURI().toURL();
                var loader = new java.net.URLClassLoader(new java.net.URL[]{url}, Bootstrap.class.getClassLoader());

                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                    jar.stream().forEach(entry -> {
                        if (entry.getName().endsWith(".class")) {
                            String className = entry.getName()
                                    .replace('/', '.')
                                    .replaceAll("\\.class$", "");
                            try {
                                Class<?> clazz = loader.loadClass(className);
                                if (clazz.isAnnotationPresent(Mod.class) && !Modifier.isAbstract(clazz.getModifiers())) {
                                    Mod ann = clazz.getAnnotation(Mod.class);
                                    String modId = ann.value();
                                    System.out.println("[Engine] Loaded @Mod from JAR: " + className);

                                    loadModAssets(modId, jarFile);
                                    loadDataPacks(modId, jarFile);

                                    var ctor = clazz.getDeclaredConstructor(IEventBus.class);
                                    ctor.setAccessible(true);
                                    ctor.newInstance(bus);
                                }
                            } catch (Throwable t) {
                                System.err.println("[Engine] Failed to load class: " + className);
                                t.printStackTrace();
                            }
                        }
                    });
                }

            } catch (Throwable t) {
                System.err.println("[Engine] Failed to load JAR: " + jarFile.getName());
                t.printStackTrace();
            }
        }
    }

    private static void loadModAssets(String modId, File jarFile) {
        File modPackDir = new File(resourcePacksDir, modId);

        // Delete old pack
        if (modPackDir.exists()) {
            try {
                Files.walk(modPackDir.toPath())
                        .sorted((p1, p2) -> p2.compareTo(p1))
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException e) { throw new UncheckedIOException(e); }
                        });
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // create pack root and assets folder
        if (!modPackDir.mkdirs()) {
            System.err.println("Failed to create pack dir " + modPackDir);
            return;
        }

        // create pack.mcmeta
        File mcmetaFile = new File(modPackDir, "pack.mcmeta");
        String mcmeta = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 15,\n" + // Minecraft 1.20.x
                "    \"description\": \"Assets for " + modId + "\"\n" +
                "  }\n" +
                "}";
        try {
            Files.writeString(mcmetaFile.toPath(), mcmeta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy assets from JAR
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            jar.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().startsWith("assets/" + modId + "/"))
                    .forEach(entry -> {
                        String relativePath = entry.getName().substring(("assets/" + modId + "/").length());
                        File outFile = new File(modPackDir, "assets/" + modId + "/" + relativePath);
                        outFile.getParentFile().mkdirs();
                        try (InputStream in = jar.getInputStream(entry);
                             OutputStream out = new FileOutputStream(outFile)) {
                            in.transferTo(out);
                        } catch (IOException e) {
                            System.err.println("Failed to copy asset: " + entry.getName());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadDataPacks(String modId, File jarFile) {
        File modPackDir = new File(dataPacksTempFolder, modId);

        // Delete old pack
        if (modPackDir.exists()) {
            try {
                Files.walk(modPackDir.toPath())
                        .sorted((p1, p2) -> p2.compareTo(p1))
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException e) { throw new UncheckedIOException(e); }
                        });
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // create pack root and assets folder
        if (!modPackDir.mkdirs()) {
            System.err.println("Failed to create pack dir " + modPackDir);
            return;
        }

        // create pack.mcmeta
        File mcmetaFile = new File(modPackDir, "pack.mcmeta");
        String mcmeta = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 15,\n" + // Minecraft 1.20.x
                "    \"description\": \"Data for " + modId + "\"\n" +
                "  }\n" +
                "}";
        try {
            Files.writeString(mcmetaFile.toPath(), mcmeta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy assets from JAR
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            jar.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().startsWith("data/" + modId + "/"))
                    .forEach(entry -> {
                        String relativePath = entry.getName().substring(("data/" + modId + "/").length());
                        File outFile = new File(modPackDir, "data/" + modId + "/" + relativePath);
                        outFile.getParentFile().mkdirs();
                        try (InputStream in = jar.getInputStream(entry);
                             OutputStream out = new FileOutputStream(outFile)) {
                            in.transferTo(out);
                        } catch (IOException e) {
                            System.err.println("Failed to copy asset: " + entry.getName());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}