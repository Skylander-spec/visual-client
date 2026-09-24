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
    private static volatile String geladenerName = null;
    private static volatile long geladenStand = -1;
    private static long letztePruefung = 0;
    private static volatile boolean loading;

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
        if (loading) return;
        String name = VConfig.get().miniMeSkin;
        if (name == null || name.isEmpty()) {
            clear();
            geladenerName = null;
            geladenStand = -1;
            return;
        }
        loading = true;
        CosmeticIO.WORKER.execute(() -> {
            try {
                Path file = datei(name);
                long stamp = file == null ? -1 : Files.getLastModifiedTime(file).toMillis();
                if (name.equals(geladenerName) && stamp == geladenStand) return;
                NativeImage image = file == null ? null : CosmeticIO.read(file);
                MinecraftClient.getInstance().execute(() -> {
                    if (!name.equals(VConfig.get().miniMeSkin)) {
                        if (image != null) image.close();
                        return;
                    }
                    clear();
                    if (image != null) {
                        Identifier id = Identifier.of("visualsfabric", "minime/selected");
                        try {
                            Compat.registerTexture(MinecraftClient.getInstance(), id, image);
                            textur = id;
                        } catch (Exception error) { image.close(); }
                    }
                    geladenerName = name;
                    geladenStand = stamp;
                });
            } catch (Exception ignored) {
                // Keep the last valid image during external file updates.
            } finally { loading = false; }
        });
    }

    private static volatile String importMessage = "";
    private static final java.util.concurrent.atomic.AtomicBoolean importing = new java.util.concurrent.atomic.AtomicBoolean();
    public static String importStatus() { return importMessage; }
    public static void importFile() {
        if (!importing.compareAndSet(false, true)) return;
        importMessage = dev.visual.fabric.ui.VText.t("pet.choosefile");
        Thread picker = new Thread(() -> {
            try {
                String chosen = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                        "Mini-Me Skin (64 x 64 PNG)", null, (org.lwjgl.PointerBuffer) null, "PNG", false);
                if (chosen == null) { importMessage = ""; return; }
                Path source = Path.of(chosen);
                if (Files.size(source) > 1024 * 1024) throw new java.io.IOException("PNG is too large (1 MB maximum)");
                byte[] bytes = Files.readAllBytes(source);
                try (var input = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                    var readers = javax.imageio.ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw new java.io.IOException("Expected a PNG skin");
                    var reader = readers.next();
                    try {
                        reader.setInput(input);
                        if (!reader.getFormatName().equalsIgnoreCase("png") || reader.getWidth(0) != 64 || reader.getHeight(0) != 64)
                            throw new java.io.IOException("Skin must be a 64 x 64 PNG");
                        reader.read(0).flush();
                    } finally { reader.dispose(); }
                }
                Path folder = CapeManager.datenOrdner().resolve("skins");
                Files.createDirectories(folder);
                String base = source.getFileName().toString().replaceFirst("(?i)\\.png$", "")
                        .replaceAll("[^a-zA-Z0-9 _-]", "_");
                if (base.isBlank()) base = "Mini Skin";
                String name = base;
                int suffix = 1;
                while (Files.exists(folder.resolve(name + ".png"))) name = base + " " + suffix++;
                Files.write(folder.resolve(name + ".png"), bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
                String selected = name;
                MinecraftClient.getInstance().execute(() -> {
                    VConfig.get().miniMeSkin = selected; VConfig.get().miniMeArt = 0; VConfig.get().miniMe = true;
                    VConfig.save(); letztePruefung = 0;
                    importMessage = dev.visual.fabric.ui.VText.t("pet.imported", selected);
                });
            } catch (Exception error) { importMessage = error.getMessage(); }
            finally { importing.set(false); }
        }, "Sky skin file picker");
        picker.setDaemon(true); picker.start();
    }

    private static void clear() {
        if (textur != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(textur);
        textur = null;
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
        if (d == null || name == null || name.isEmpty() || name.contains("/") || name.contains("\\")) return null;
        Path p = d.resolve("skins").resolve(name + ".png");
        return Files.isRegularFile(p) ? p : null;
    }
}