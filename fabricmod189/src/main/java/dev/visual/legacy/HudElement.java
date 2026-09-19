package dev.visual.legacy;

/**
 * Ein HUD-Baustein: an/aus, frei verschiebbar, liefert seinen Text selbst.
 * Position wird in Prozent der Bildschirmgroesse gespeichert, damit sie bei
 * anderer Aufloesung oder GUI-Skalierung an derselben Stelle bleibt.
 */
public class HudElement {
    public interface TextSource {
        String get();
    }

    public final String id;
    public final String title;
    private final TextSource source;
    private final int standardX;
    private final int standardY;

    /**
     * standardX/Y sind die Startposition in Promille. Vorher hatten ALLE
     * Bausteine denselben Vorgabewert und lagen damit uebereinander —
     * im Spiel war davon nur ein unlesbarer Klumpen zu sehen.
     */
    public HudElement(String id, String title, int standardX, int standardY,
                      TextSource source) {
        this.id = id;
        this.title = title;
        this.standardX = standardX;
        this.standardY = standardY;
        this.source = source;
    }

    public boolean enabled() {
        return VConfig189.getBool(id + ".on", true);
    }

    public void setEnabled(boolean on) {
        VConfig189.setBool(id + ".on", on);
    }

    /** X-Position in Promille der Breite (0–1000). */
    public int xPermille() {
        return VConfig189.getInt(id + ".x", standardX);
    }

    public int yPermille() {
        return VConfig189.getInt(id + ".y", standardY);
    }

    public void setPos(int xPermille, int yPermille) {
        VConfig189.setInt(id + ".x", clamp(xPermille));
        VConfig189.setInt(id + ".y", clamp(yPermille));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 1000 ? 1000 : v;
    }

    private String cached = "";
    private long cachedAt;

    /**
     * Der Text wird hoechstens zehnmal pro Sekunde neu gebildet.
     *
     * Ohne diese Bremse liefe die Berechnung einmal pro Bild — bei ueber
     * 1000 Bildern je Sekunde waeren das ebenso viele String.format- und
     * Ping-Abfragen. Zehnmal je Sekunde ist fuer das Auge ohnehin die
     * Grenze des Lesbaren.
     */
    public String text() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < 100L && !cached.isEmpty()) return cached;
        cachedAt = now;
        try {
            cached = source.get();
        } catch (Exception e) {
            // Ein defekter Baustein darf nie das ganze HUD reissen
            cached = VText189.t(title) + ": —";
        }
        return cached;
    }
}
