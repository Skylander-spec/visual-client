package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt die Vorschaubilder der Modrinth-Projekte und hält sie als Texturen.
 *
 * Bilder kommen über das Netz, Texturen dürfen nur im Render-Thread
 * angelegt werden — deshalb lädt {@link #anfordern} nebenher und reicht das
 * fertige Bild an den Client zurück. Solange nichts da ist, liefert
 * {@link #textur} {@code null}; der Aufrufer zeichnet dann seine eigene
 * Ersatzkachel, statt auf das Bild zu warten.
 *
 * Modrinth liefert die Symbole in verschiedenen Formaten aus. NativeImage
 * liest nur PNG, alles andere wird gar nicht erst geholt.
 */
public final class ProjektBild {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    /** Projekt-Kennung -> Textur. Fehlt der Eintrag, läuft der Versuch noch. */
    private static final Map<String, Identifier> FERTIG = new ConcurrentHashMap<>();
    /** Projekte, für die schon ein Versuch läuft oder gescheitert ist. */
    private static final Map<String, Boolean> VERSUCHT = new ConcurrentHashMap<>();

    private ProjektBild() {
    }

    /** Textur eines Projekts oder {@code null}, wenn (noch) keine da ist. */
    public static Identifier textur(String projektId) {
        return FERTIG.get(projektId);
    }

    /**
     * Anstoßen, dass ein Bild geladen wird. Mehrfachaufrufe sind gewollt —
     * der Bildschirm ruft das je Bildaufbau, geladen wird trotzdem nur
     * einmal.
     */
    public static void anfordern(String projektId, String bildUrl) {
        if (projektId == null || bildUrl == null || bildUrl.isEmpty()) return;
        if (!bildUrl.toLowerCase().endsWith(".png")) return;
        if (VERSUCHT.putIfAbsent(projektId, Boolean.TRUE) != null) return;

        CompletableFuture.runAsync(() -> {
            try {
                byte[] daten = HTTP.send(
                        HttpRequest.newBuilder(URI.create(bildUrl))
                                .header("User-Agent", "visual-client-ingame/1.0").build(),
                        HttpResponse.BodyHandlers.ofByteArray()).body();
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    try {
                        NativeImage bild = NativeImage.read(daten);
                        Identifier id = Identifier.of("visualsfabric",
                                "projekt/" + projektId.toLowerCase());
                        Compat.registerTexture(mc, id, bild);
                        FERTIG.put(projektId, id);
                    } catch (Throwable t) {
                        // Kein lesbares PNG — es bleibt bei der Ersatzkachel
                    }
                });
            } catch (Throwable t) {
                // Netzfehler: derselbe Umgang, nur ohne Bild
            }
        });
    }
}
