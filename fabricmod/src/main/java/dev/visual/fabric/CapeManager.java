package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lädt das im Launcher gewählte Cape aus {@code .visualclient/cosmetics/cape.png}.
 *
 * Die Auflösung ist bewusst frei: das Vanilla-Layout ist proportional, ein
 * 512×256-Bild wird also im selben Raster gezeichnet — nur eben scharf.
 *
 * BEWEGTE CAPES: ist das Bild höher als das 2:1-Verhältnis eines einzelnen
 * Bildes, gilt es als Streifen mehrerer übereinandergelegter Einzelbilder.
 * Jedes wird als eigene Textur registriert und der Reihe nach gezeigt.
 * NoRisk lässt sich animierte Capes bezahlen — hier kosten sie nichts.
 */
public final class CapeManager {
    /** Millisekunden je Einzelbild. 100 ms = 10 Bilder je Sekunde. */
    private static final int MS_PRO_BILD = 100;
    /** Schutzgrenze, damit ein versehentlich riesiges Bild nicht den Speicher flutet. */
    private static final int MAX_BILDER = 64;

    private static Identifier[] bilder = null;
    private static volatile long lastModified = -1;
    private static volatile long lastCheck = 0;
    private static volatile boolean loading;
    private static final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();

    /**
     * Vorschau: der Cosmetics-Bildschirm zeichnet denselben Spieler mehrfach,
     * jedes Mal mit einem anderen Umhang. Solange eine Vorschau gesetzt ist,
     * liefert {@link #get} diese statt des angelegten Capes. Gesetzt und
     * gelöscht wird das im selben Zeichendurchlauf, also nie über ein Bild
     * hinaus sichtbar.
     */
    private static Identifier vorschau = null;
    private static boolean vorschauAktiv = false;

    public static void vorschauSetzen(Identifier cape) {
        vorschau = cape;
        vorschauAktiv = true;
    }

    public static void vorschauLoeschen() {
        vorschau = null;
        vorschauAktiv = false;
    }

    private CapeManager() {
    }

    /** Aktuelles Cape oder null. Prüft höchstens jede Sekunde auf Änderungen. */
    public static Identifier get(MinecraftClient client) {
        if (vorschauAktiv) return vorschau;
        long now = System.currentTimeMillis();
        if (now - lastCheck > 1000) {
            lastCheck = now;
            refresh(client);
        }
        if (bilder == null || bilder.length == 0) return null;
        if (bilder.length == 1) return bilder[0];
        int i = (int) ((now / MS_PRO_BILD) % bilder.length);
        return bilder[i];
    }

    /** Anzahl der Einzelbilder — 1 bei einem ruhenden Cape, 0 wenn keins da ist. */
    public static int bildAnzahl() {
        return bilder == null ? 0 : bilder.length;
    }

    private static void refresh(MinecraftClient client) {
        if (loading) return;
        loading = true;
        long expected = revision.get();
        CosmeticIO.WORKER.execute(() -> {
            NativeImage[] decoded = null;
            try {
                Path file = capeFile();
                long stamp = file == null ? -1 : Files.getLastModifiedTime(file).toMillis();
                if (stamp == lastModified) return;
                decoded = file == null ? new NativeImage[0] : zerlegen(CosmeticIO.read(file));
                NativeImage[] ready = decoded;
                decoded = null;
                client.execute(() -> {
                    if (expected != revision.get()) {
                        for (NativeImage frame : ready) frame.close();
                        return;
                    }
                    clear(client);
                    Identifier[] textures = new Identifier[ready.length];
                    int registered = 0;
                    try {
                        for (; registered < ready.length; registered++) {
                            textures[registered] = Identifier.of("visualsfabric", "cape_" + registered);
                            Compat.registerTexture(client, textures[registered], ready[registered]);
                        }
                        bilder = textures;
                        lastModified = stamp;
                    } catch (Exception error) {
                        for (int i = 0; i < registered; i++) client.getTextureManager().destroyTexture(textures[i]);
                        for (int i = registered; i < ready.length; i++) ready[i].close();
                    }
                });
            } catch (Exception ignored) {
                // Keep the last valid texture if an external writer is still replacing a file.
            } finally {
                if (decoded != null) for (NativeImage frame : decoded) frame.close();
                loading = false;
            }
        });
    }

    private static void clear(MinecraftClient client) {
        if (bilder != null) for (Identifier id : bilder) client.getTextureManager().destroyTexture(id);
        bilder = null;
    }

    /**
     * Ein Cape-Bild ist doppelt so breit wie hoch. Ist das geladene Bild
     * höher, stecken mehrere Einzelbilder übereinander darin.
     */
    private static NativeImage[] zerlegen(NativeImage full) {
        int width = full.getWidth();
        int height = width / 2;
        int count = height > 0 ? full.getHeight() / height : 0;
        if (count <= 1) return new NativeImage[]{full};
        count = Math.min(count, MAX_BILDER);
        NativeImage[] frames = new NativeImage[count];
        try (full) {
            for (int i = 0; i < count; i++) {
                frames[i] = new NativeImage(width, height, false);
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                    frames[i].setColorArgb(x, y, full.getColorArgb(x, i * height + y));
            }
            return frames;
        } catch (Throwable error) {
            for (NativeImage frame : frames) if (frame != null) frame.close();
            throw error;
        }
    }

    /** Ordner des Launchers, oder null, wenn er nicht zu finden ist. */
    public static Path datenOrdner() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            Path p = Paths.get(appData, ".visualclient");
            if (Files.isDirectory(p)) return p;
        }
        Path relativ = Paths.get("..", "..");
        return Files.isDirectory(relativ.resolve("capes")) ? relativ : null;
    }

    /** Name des angelegten Capes, wie der Launcher ihn vermerkt. */
    public static String angelegt() {
        Path d = datenOrdner();
        if (d == null) return null;
        try {
            Path marker = d.resolve("cosmetics").resolve("cape.name");
            return Files.isRegularFile(marker) ? Files.readString(marker).trim() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Ein Cape anlegen — genau wie der Launcher: die Datei nach
     * cosmetics/cape.png kopieren und den Namen daneben vermerken. Dadurch
     * zeigen Launcher und Spiel immer dasselbe an.
     */
    public static void anlegen(String name) {
        Path d = datenOrdner();
        if (d == null) throw new IllegalStateException("Cosmetics directory unavailable");
        try {
            Path ziel = d.resolve("cosmetics").resolve("cape.png");
            Path marker = d.resolve("cosmetics").resolve("cape.name");
            Files.createDirectories(ziel.getParent());
            if (name == null || name.isEmpty()) {
                Files.deleteIfExists(ziel);
                Files.deleteIfExists(marker);
            } else {
                Path temporary = Files.createTempFile(ziel.getParent(), "cape-", ".png");
                try {
                    try (InputStream in = CapeLibrary.open(name)) {
                        if (in == null) throw new java.io.IOException("Cape unavailable: " + name);
                        Files.copy(in, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    try (NativeImage checked = CosmeticIO.read(temporary)) { /* validate before replacing */ }
                    CosmeticIO.replace(temporary, ziel);
                } finally { Files.deleteIfExists(temporary); }
                Files.writeString(marker, name);
            }
            revision.incrementAndGet();
            lastModified = Long.MIN_VALUE; // force reload, including removal
            lastCheck = 0;
        } catch (Throwable t) {
            throw new IllegalStateException("Cape could not be equipped", t);
        }
    }

    /**
     * Der Launcher legt das Cape global unter %APPDATA%/.visualclient/cosmetics
     * ab; das Spiel läuft im Instanzordner darunter, deshalb beide Wege.
     */
    private static Path capeFile() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            Path p = Paths.get(appData, ".visualclient", "cosmetics", "cape.png");
            if (Files.isRegularFile(p)) return p;
        }
        Path relative = Paths.get("..", "..", "cosmetics", "cape.png");
        return Files.isRegularFile(relative) ? relative : null;
    }
}
