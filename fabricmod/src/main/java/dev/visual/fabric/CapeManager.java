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
    private static long lastModified = -1;
    private static long lastCheck = 0;

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
        Path file = capeFile();
        try {
            if (file == null || !Files.isRegularFile(file)) {
                bilder = null;
                lastModified = -1;
                return;
            }
            long stamp = Files.getLastModifiedTime(file).toMillis();
            if (stamp == lastModified && bilder != null) return;
            try (InputStream in = Files.newInputStream(file)) {
                zerlegen(client, NativeImage.read(in));
                lastModified = stamp;
            }
        } catch (Throwable t) {
            bilder = null;
        }
    }

    /**
     * Ein Cape-Bild ist doppelt so breit wie hoch. Ist das geladene Bild
     * höher, stecken mehrere Einzelbilder übereinander darin.
     */
    private static void zerlegen(MinecraftClient client, NativeImage voll) {
        int b = voll.getWidth();
        int hoeheJeBild = b / 2;
        int anzahl = hoeheJeBild > 0 ? voll.getHeight() / hoeheJeBild : 0;

        if (anzahl <= 1) {
            Identifier id = Identifier.of("visualsfabric", "cape");
            Compat.registerTexture(client, id, voll);
            bilder = new Identifier[]{id};
            return;
        }
        if (anzahl > MAX_BILDER) anzahl = MAX_BILDER;

        Identifier[] neu = new Identifier[anzahl];
        for (int i = 0; i < anzahl; i++) {
            NativeImage teil = new NativeImage(b, hoeheJeBild, false);
            // NativeImage bietet kein Zuschneiden — zeilenweise kopieren.
            for (int y = 0; y < hoeheJeBild; y++) {
                for (int x = 0; x < b; x++) {
                    teil.setColorArgb(x, y, voll.getColorArgb(x, i * hoeheJeBild + y));
                }
            }
            Identifier id = Identifier.of("visualsfabric", "cape_" + i);
            Compat.registerTexture(client, id, teil);
            neu[i] = id;
        }
        voll.close();
        bilder = neu;
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
        if (d == null) return;
        try {
            Path ziel = d.resolve("cosmetics").resolve("cape.png");
            Path marker = d.resolve("cosmetics").resolve("cape.name");
            Files.createDirectories(ziel.getParent());
            if (name == null) {
                Files.deleteIfExists(ziel);
                Files.deleteIfExists(marker);
            } else {
                Files.copy(d.resolve("capes").resolve(name + ".png"), ziel,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(marker, name);
            }
            lastModified = -1;  // beim naechsten get neu einlesen
            lastCheck = 0;
        } catch (Throwable t) {
            // Schlaegt das Kopieren fehl, bleibt das bisherige Cape angelegt
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
