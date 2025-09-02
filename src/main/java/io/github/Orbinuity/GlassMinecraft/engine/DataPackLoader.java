package io.github.Orbinuity.GlassMinecraft.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DataPackLoader {
    public static void importModDataPacks(Path dataPacksFolder) {
        copyFolderTo(Bootstrap.dataPacksTempFolder.toPath(), dataPacksFolder);
    }

    private static void copyFolderTo(Path source, Path target) {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source folder does not exist: " + source);
        }

        try {
            Files.walk(source).forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path targetPath = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + path, e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk source folder: " + source, e);
        }
    }
}
