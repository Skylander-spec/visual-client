package dev.visual.fabric;

import net.minecraft.client.texture.NativeImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** File access and PNG decoding must never block the render thread. */
final class CosmeticIO {
    static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Sky cosmetic images");
        thread.setDaemon(true);
        return thread;
    });

    static NativeImage read(Path path) throws IOException {
        if (Files.size(path) > 8_000_000) throw new IOException("Cosmetic file too large");
        byte[] bytes;
        try (var in = Files.newInputStream(path)) {
            bytes = in.readNBytes(8_000_001);
        }
        if (bytes.length > 8_000_000 || bytes.length < 24) throw new IOException("Invalid cosmetic PNG");
        ByteBuffer header = ByteBuffer.wrap(bytes);
        int width = header.getInt(16), height = header.getInt(20);
        if (header.getLong(0) != 0x89504e470d0a1a0aL || width < 1 || height < 1
                || width > 2048 || height > 8192 || (long) width * height > 4_194_304)
            throw new IOException("Cosmetic dimensions too large");
        return NativeImage.read(new java.io.ByteArrayInputStream(bytes));
    }

    static void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
