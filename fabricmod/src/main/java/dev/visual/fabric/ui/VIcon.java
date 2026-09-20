package dev.visual.fabric.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Symbole, gezeichnet statt gesetzt.
 *
 * Vorher standen in den Knoepfen Zeichen wie Zahnrad, Raute oder Kreuz.
 * Minecrafts Schrift kennt die nicht, und Poppins bringt sie auch nicht
 * mit - im Spiel stand deshalb in jedem Knopf dasselbe
 * Platzhalter-Kaestchen. Das ist der Unterschied, der einen Client
 * billig aussehen laesst.
 *
 * OneClient zeichnet seine Symbole als Vektoren. Wir machen dasselbe mit
 * Rechtecken: bei dieser Groesse (8-10 Pixel) sieht man den Unterschied
 * nicht, und es kann nichts fehlen, weil weder Schrift noch Textur
 * beteiligt ist.
 *
 * Alle Symbole zeichnen in ein Quadrat der Kantenlaenge {@code g} ab
 * {@code (x, y)} und bleiben darin.
 */
public final class VIcon {

    private VIcon() {
    }

    /** Ein Strich der Dicke 1, waagerecht. */
    private static void h(DrawContext ctx, int x, int y, int w, int farbe) {
        ctx.fill(x, y, x + w, y + 1, farbe);
    }

    /** Ein Strich der Dicke 1, senkrecht. */
    private static void v(DrawContext ctx, int x, int y, int h, int farbe) {
        ctx.fill(x, y, x + 1, y + h, farbe);
    }

    /** Ein Kasten, nur Umriss. */
    private static void kasten(DrawContext ctx, int x, int y, int w, int h, int farbe) {
        h(ctx, x, y, w, farbe);
        h(ctx, x, y + h - 1, w, farbe);
        v(ctx, x, y, h, farbe);
        v(ctx, x + w - 1, y, h, farbe);
    }

    /** Einzelspieler: Kopf und Schultern. */
    public static void person(DrawContext ctx, int x, int y, int g, int farbe) {
        int k = Math.max(2, g / 3);
        ctx.fill(x + (g - k) / 2, y + 1, x + (g + k) / 2, y + 1 + k, farbe);
        ctx.fill(x + 1, y + g - k, x + g - 1, y + g, farbe);
    }

    /** Mehrspieler: zwei Personen, die hintere versetzt. */
    public static void personen(DrawContext ctx, int x, int y, int g, int farbe) {
        person(ctx, x, y, g - 2, farbe);
        person(ctx, x + 3, y + 1, g - 3, farbe);
    }

    /** Realms: Kugel mit Laengen- und Breitenkreis. */
    public static void kugel(DrawContext ctx, int x, int y, int g, int farbe) {
        kasten(ctx, x + 1, y + 1, g - 2, g - 2, farbe);
        h(ctx, x + 1, y + g / 2, g - 2, farbe);
        v(ctx, x + g / 2, y + 1, g - 2, farbe);
    }

    /** Optionen: Zahnrad, angedeutet durch Kern und vier Zaehne. */
    public static void zahnrad(DrawContext ctx, int x, int y, int g, int farbe) {
        int m = g / 2;
        int k = Math.max(2, g / 3);
        ctx.fill(x + m - k / 2, y + m - k / 2, x + m + k / 2 + 1, y + m + k / 2 + 1, farbe);
        h(ctx, x + m, y, 1, farbe);
        h(ctx, x + m, y + g - 1, 1, farbe);
        v(ctx, x, y + m, 1, farbe);
        v(ctx, x + g - 1, y + m, 1, farbe);
    }

    /** Module: drei Schieberegler mit versetzten Griffen. */
    public static void regler(DrawContext ctx, int x, int y, int g, int farbe) {
        int s = Math.max(2, g / 4);
        for (int i = 0; i < 3; i++) {
            int zy = y + 1 + i * s;
            h(ctx, x, zy, g, farbe);
            int gx = x + (i == 1 ? g - 4 : 2);
            ctx.fill(gx, zy - 1, gx + 2, zy + 2, farbe);
        }
    }

    /** Cosmetics: Raute. */
    public static void raute(DrawContext ctx, int x, int y, int g, int farbe) {
        int m = g / 2;
        for (int i = 0; i <= m; i++) {
            int b = 1 + 2 * i;
            ctx.fill(x + m - i, y + i, x + m - i + b, y + i + 1, farbe);
            ctx.fill(x + m - i, y + g - 1 - i, x + m - i + b, y + g - i, farbe);
        }
    }

    /** Beenden: Kreuz aus zwei Diagonalen. */
    public static void kreuz(DrawContext ctx, int x, int y, int g, int farbe) {
        for (int i = 0; i < g; i++) {
            ctx.fill(x + i, y + i, x + i + 1, y + i + 1, farbe);
            ctx.fill(x + g - 1 - i, y + i, x + g - i, y + i + 1, farbe);
        }
    }

    /**
     * Die Bildmarke: ein Vierstrahl-Stern.
     *
     * Vorher waren es vier Quadrate um die Mitte - das sah wie ein Plus
     * aus Bausteinen aus. Ein Stern hat eingezogene Flanken, und genau
     * das macht den Unterschied: die Breite je Zeile faellt quadratisch
     * statt linear, deshalb {@code (1 - t) * (1 - t)}. Bei linearem
     * Abfall waere es eine Raute.
     *
     * Gezeichnet wird zeilenweise von der Mitte nach aussen, damit der
     * Stern unabhaengig von der Groesse sauber symmetrisch bleibt.
     */
    public static void stern(DrawContext ctx, int cx, int cy, int r, int farbe) {
        for (int dy = -r; dy <= r; dy++) {
            float t = Math.abs(dy) / (float) r;
            int halb = Math.round(r * (1 - t) * (1 - t));
            if (halb <= 0) {
                continue;
            }
            ctx.fill(cx - halb, cy + dy, cx + halb + 1, cy + dy + 1, farbe);
        }
    }

    /**
     * Nach Namen zeichnen, damit die Knopfliste einen kurzen Schluessel
     * tragen kann statt eines Zeichens.
     *
     * Unbekannte Schluessel zeichnen nichts - lieber eine leere Flaeche
     * als wieder ein Platzhalter-Kaestchen.
     */
    public static void zeichne(DrawContext ctx, String name, int x, int y, int g, int farbe) {
        if (name == null) {
            return;
        }
        switch (name) {
            case "person" -> person(ctx, x, y, g, farbe);
            case "personen" -> personen(ctx, x, y, g, farbe);
            case "kugel" -> kugel(ctx, x, y, g, farbe);
            case "zahnrad" -> zahnrad(ctx, x, y, g, farbe);
            case "regler" -> regler(ctx, x, y, g, farbe);
            case "raute" -> raute(ctx, x, y, g, farbe);
            case "kreuz" -> kreuz(ctx, x, y, g, farbe);
            default -> {
            }
        }
    }
}
