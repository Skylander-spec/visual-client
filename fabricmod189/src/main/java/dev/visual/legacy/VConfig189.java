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

    public static void save() {
        FileOutputStream out = null;
        try {
            File dir = FILE.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            out = new FileOutputStream(FILE);
            P.store(out, "Visual Client 1.8.9");
        } catch (Exception ignored) {
            // Nicht schreibbar: Einstellungen gelten nur fuer diese Sitzung
        } finally {
            close(out);
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
