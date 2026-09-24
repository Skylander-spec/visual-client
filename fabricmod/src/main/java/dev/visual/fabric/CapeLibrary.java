package dev.visual.fabric;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** File work stays off the render thread. Built-ins ship with the mod. */
public final class CapeLibrary {
    public record Entry(String name, byte[] png) {}
    private static final String[] BUILT_INS = {
            "Aurora", "Amethyst", "Sternenlicht", "Goldregen", "Frost", "Glut",
            "Monster Energy Weiss", "Kirschbluete", "Panda Liebe", "Mondkatze",
            "Wolkenzucker", "Mint Herzen", "Pfirsich", "Lavendel", "Sternenreise"
    };
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Visual cosmetics IO");
        thread.setDaemon(true);
        return thread;
    });
    private static CompletableFuture<List<Entry>> cached;
    private static long loadedAt;

    private CapeLibrary() {}

    public static synchronized CompletableFuture<List<Entry>> load() {
        if (cached == null || System.nanoTime() - loadedAt > 5_000_000_000L) {
            loadedAt = System.nanoTime();
            cached = CompletableFuture.supplyAsync(CapeLibrary::read, IO);
        }
        return cached;
    }

    private static List<Entry> read() {
        List<Entry> result = new ArrayList<>();
        for (String name : BUILT_INS) {
            try (InputStream in = open("Visual " + name)) {
                if (in != null) result.add(new Entry("Visual " + name, preview(in)));
            } catch (Exception error) {
                System.err.println("[Visual] Cape unavailable: " + name + ": " + error.getMessage());
            }
        }
        Path data = CapeManager.datenOrdner();
        if (data != null && Files.isDirectory(data.resolve("capes"))) {
            try (var paths = Files.list(data.resolve("capes"))) {
                paths.filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                        .sorted().limit(256).forEach(p -> {
                            try {
                                if (Files.size(p) > 8 * 1024 * 1024) return;
                                String file = p.getFileName().toString();
                                String name = file.substring(0, file.length() - 4);
                                if (result.stream().noneMatch(e -> e.name().equals(name)))
                                    try (InputStream in = Files.newInputStream(p)) {
                                        result.add(new Entry(name, preview(in)));
                                    }
                            } catch (Exception ignored) { }
                        });
            } catch (Exception error) {
                System.err.println("[Visual] Cape folder: " + error.getMessage());
            }
        }
        return List.copyOf(result);
    }

    /** Thumbnails contain only a small first frame, never a full animation strip. */
    private static byte[] preview(InputStream source) throws java.io.IOException {
        try (var stream = javax.imageio.ImageIO.createImageInputStream(source)) {
            var readers = javax.imageio.ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new java.io.IOException("Invalid cape image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 2 || height < 1 || width > 2048 || height > 8192 || (long) width * height > 4_194_304)
                    throw new java.io.IOException("Cape dimensions too large");
                var image = reader.read(0);
                var thumbnail = new java.awt.image.BufferedImage(128, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = thumbnail.createGraphics();
                try { graphics.drawImage(image, 0, 0, 128, 64, 0, 0, width, Math.min(height, width / 2), null); }
                finally { graphics.dispose(); image.flush(); }
                var bytes = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(thumbnail, "png", bytes);
                thumbnail.flush();
                return bytes.toByteArray();
            } finally { reader.dispose(); }
        }
    }

    public static InputStream open(String name) throws java.io.IOException {
        for (String builtin : BUILT_INS) {
            if (name.equals("Visual " + builtin))
                return CapeLibrary.class.getResourceAsStream("/assets/visualsfabric/capes/" + builtin + ".png");
        }
        if (name.contains("/") || name.contains("\\") || name.equals(".."))
            throw new java.io.IOException("Invalid cape name");
        Path data = CapeManager.datenOrdner();
        if (data == null) throw new java.io.IOException("Cosmetics folder unavailable");
        return Files.newInputStream(data.resolve("capes").resolve(name + ".png"));
    }

    public static CompletableFuture<Void> equip(String name) {
        return CompletableFuture.runAsync(() -> CapeManager.anlegen(name), IO);
    }
}
