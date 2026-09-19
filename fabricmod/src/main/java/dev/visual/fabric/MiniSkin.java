package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Eigener Skin für den Mini-Me.
 *
 * Gewählt wird eine PNG aus dem Skin-Ordner des Launchers; der Name steht in
 * der Config. Geladen wird sie einmal und danach nur neu, wenn sich Name oder
 * Änderungszeit unterscheiden — das Zeichnen fragt jeden Frame hier an, da
 * darf nichts Teures passieren.
 *
 * Ohne Auswahl liefert {@link #textur} null; dann trägt der Kleine den Skin
 * des Spielers, so wie bisher.
 */
public final class MiniSkin {
    private static Identifier textur = null;
    private static String geladenerName = null;
    private static long geladenStand = -1;
    private static long letztePruefung = 0;

    private MiniSkin() {
    }

    /** Textur des gewählten Skins oder null. Höchstens jede Sekunde geprüft. */
    public static Identifier textur() {
        long jetzt = System.currentTimeMillis();
        if (jetzt - letztePruefung > 1000) {
            letztePruefung = jetzt;
            pruefen();
        }
        return textur;
    }

    private static void pruefen() {
        String name = VConfig.get().miniMeSkin;
        if (name == null || name.isEmpty()) {
            textur = null;
            geladenerName = null;
            geladenStand = -1;
            return;
        }
        Path datei = datei(name);
        if (datei == null) {
            textur = null;
            return;
        }
        try {
            long stand = Files.getLastModifiedTime(datei).toMillis();
            if (name.equals(geladenerName) && stand == geladenStand && textur != null) return;
            try (InputStream in = Files.newInputStream(datei)) {
                Identifier id = Identifier.of("visualsfabric",
                        "minime/" + Integer.toHexString(name.hashCode()));
                Compat.registerTexture(MinecraftClient.getInstance(), id, NativeImage.read(in));
                textur = id;
                geladenerName = name;
                geladenStand = stand;
            }
        } catch (Throwable t) {
            textur = null;
        }
    }

    /**
     * Alle Skins aus dem Ordner des Launchers, der leere Eintrag voran.
     * Leer heißt: der Kleine trägt den Skin des Spielers.
     */
    public static java.util.List<String> namen() {
        java.util.List<String> liste = new java.util.ArrayList<>();
        liste.add("");
        Path d = CapeManager.datenOrdner();
        if (d == null) return liste;
        try (java.util.stream.Stream<Path> s = Files.list(d.resolve("skins"))) {
            s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .forEach(p -> {
                        String n = p.getFileName().toString();
                        liste.add(n.substring(0, n.length() - 4));
                    });
        } catch (Throwable t) {
            // Kein Ordner — dann bleibt es beim eigenen Skin
        }
        return liste;
    }

    /** Pfad zur gewählten Skin-Datei, oder null. */
    public static Path datei(String name) {
        Path d = CapeManager.datenOrdner();
        if (d == null || name == null || name.isEmpty()) return null;
        Path p = d.resolve("skins").resolve(name + ".png");
        return Files.isRegularFile(p) ? p : null;
    }
}