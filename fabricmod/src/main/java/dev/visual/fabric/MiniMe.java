package dev.visual.fabric;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Mini-Me: eine verkleinerte Ausgabe des eigenen Spielers, die auf dem Kopf
 * oder einer Schulter mitreitet.
 *
 * Gezeichnet wird nicht etwa ein nachgebautes Modell, sondern derselbe
 * Renderer noch einmal — mit verschobener und verkleinerter Matrix. Dadurch
 * bekommt der Kleine automatisch Skin, Cape, Rüstung, Gegenstand in der Hand
 * und dieselbe Pose wie das Original, ohne dass hier irgendetwas davon
 * nachgepflegt werden müsste. Der Preis ist die Rekursion, gegen die
 * {@link #beginnen()} sichert.
 *
 * Nur der eigene Spieler bekommt ihn: es ist eine lokale Kosmetik, andere
 * sehen sie ohnehin nicht. Welcher Render-Zustand der eigene ist, merkt sich
 * {@link #merken} beim Befüllen — Zustände sind je Wesen wiederverwendet,
 * die Kennung bleibt also gültig.
 */
public final class MiniMe {
    /** Höhe des Spielermodells in Blöcken — Bezug für alle Sitzplätze. */
    private static final float GROESSE = 1.8f;

    private static boolean zeichnet = false;
    private static Object eigenerZustand = null;

    private MiniMe() {
    }

    /** Beim Befüllen des Render-Zustands aufrufen. */
    public static void merken(Object zustand, boolean selbst) {
        if (selbst) eigenerZustand = zustand;
    }

    /** Gilt dieser Zustand als der eigene Spieler? */
    public static boolean istEigener(Object zustand) {
        return zustand != null && zustand == eigenerZustand;
    }

    /**
     * Darf jetzt ein Mini gezeichnet werden? Liefert nur beim äußeren
     * Durchlauf {@code true}; der Mini selbst bekommt keinen zweiten.
     */
    public static boolean beginnen(Object zustand) {
        if (zeichnet) return false;
        if (!VConfig.get().miniMe) return false;
        if (!istEigener(zustand)) return false;
        zeichnet = true;
        return true;
    }

    public static void beenden() {
        zeichnet = false;
    }

    /**
     * Setzt die Matrix auf den Sitzplatz. Aufgerufen wird sie am Ende des
     * Renderns, also am Fußpunkt des Spielers ohne dessen Drehung — deshalb
     * hier erst in die Blickrichtung drehen, dann seitlich versetzen und
     * wieder zurückdrehen, damit der Kleine seine eigene Drehung selbst
     * anwenden kann.
     */
    public static void platzieren(MatrixStack matrizen, float koerperDrehung) {
        VConfig c = VConfig.get();
        float faktor = Math.max(0.1f, Math.min(0.9f, c.miniMeSize / 100f));

        matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-koerperDrehung));
        switch (c.miniMePos) {
            // Auf dem Kopf: mittig, direkt über dem Scheitel
            case 0 -> matrizen.translate(0.0f, GROESSE, 0.0f);
            // Linke Schulter: knapp unter Halshöhe, um die halbe Schulterbreite versetzt
            case 1 -> matrizen.translate(0.26f, GROESSE - 0.45f, 0.0f);
            // Rechte Schulter
            default -> matrizen.translate(-0.26f, GROESSE - 0.45f, 0.0f);
        }
        matrizen.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(koerperDrehung));
        matrizen.scale(faktor, faktor, faktor);
    }
}
