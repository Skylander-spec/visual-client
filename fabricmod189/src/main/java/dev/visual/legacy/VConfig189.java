package dev.visual.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Einstellungen als schlichte Properties-Datei in config/visual-189.properties.
 * java.util.Properties reicht hier vollstaendig aus — eine JSON-Bibliothek
 * waere zusaetzliches Gewicht ohne Gewinn.
 */
public final class VConfig189 {
    private static final File FILE = new File("config/visual-189.properties");
    private static final Properties P = new Properties();
    private static final java.util.concurrent.ScheduledExecutorService WRITER =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Sky legacy settings");
                thread.setDaemon(true);
                return thread;
            });
    private static java.util.concurrent.ScheduledFuture<?> pending;
    private static volatile Properties latest;
    private static final Object WRITE_LOCK = new Object();
    static { Runtime.getRuntime().addShutdownHook(new Thread(VConfig189::writeLatest, "Sky legacy settings flush")); }

    private VConfig189() {
    }

    public static void load() {
        if (!FILE.exists()) return;
        FileInputStream in = null;
        try {
            in = new FileInputStream(FILE);
            P.load(in);
        } catch (Exception ignored) {
            // Beschaedigte Datei: mit Standardwerten weiterlaufen
        } finally {
            close(in);
        }
    }

    public static synchronized void save() {
        Properties snapshot = new Properties();
        snapshot.putAll(P);
        latest = snapshot;
        if (pending != null) pending.cancel(false);
        pending = WRITER.schedule(VConfig189::writeLatest, 150, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static void writeLatest() {
        synchronized (WRITE_LOCK) {
            Properties snapshot = latest;
            if (snapshot == null) return;
            java.nio.file.Path temporary = null;
            try {
                java.nio.file.Path target = FILE.toPath().toAbsolutePath();
                java.nio.file.Files.createDirectories(target.getParent());
                temporary = java.nio.file.Files.createTempFile(target.getParent(), "visual-settings-", ".tmp");
                try (java.io.OutputStream output = java.nio.file.Files.newOutputStream(temporary)) {
                    snapshot.store(output, "Visual Client 1.8.9");
                }
                try {
                    java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (java.io.IOException error) {
                System.err.println("[Sky] Settings could not be saved: " + error.getClass().getSimpleName());
            } finally {
                if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); } catch (java.io.IOException ignored) { }
            }
        }
    }

    private static void close(java.io.Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    public static boolean getBool(String key, boolean fallback) {
        String v = P.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }

    public static void setBool(String key, boolean value) {
        P.setProperty(key, Boolean.toString(value));
    }

    public static int getInt(String key, int fallback) {
        try {
            String v = P.getProperty(key);
            return v == null ? fallback : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static void setInt(String key, int value) {
        P.setProperty(key, Integer.toString(value));
    }
}
