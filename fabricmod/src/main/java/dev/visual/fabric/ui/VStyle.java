package dev.visual.fabric.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Gestaltung der In-Game-Oberfläche — Vorbild ist Polyfrosts OneClient.
 *
 * Die Werte (Farben, Radien, Abstände) stammen aus dessen offener
 * Gestaltungsdefinition. Übernommen sind nur diese Zahlen, kein Quelltext:
 * OneLauncher steht unter GPL-3.0, und kopierter Code würde diese Lizenz auf
 * den Mod übertragen. Die Umsetzung hier ist eigenständig geschrieben.
 *
 * Kennzeichen des Stils: blaustichiges Dunkel statt neutralem Grau, ein
 * kräftiges Blau als Marke, Text nicht reinweiß sondern leicht blau, und
 * Ränder als sehr schwaches Weiß statt sichtbarer Linien.
 */
public final class VStyle {
    // ── Flächen ──────────────────────────────────────────────────────────
    /** Abdunklung hinter einem Panel (page_overlay). */
    public static final int BG = 0xC411171C;
    /** Grundfläche der Seite. */
    public static final int PAGE = 0xFF11171C;
    /** Angehobene Fläche, z. B. Seitenleiste (page_elevated). */
    public static final int PANEL = 0xFF151C22;
    /** Bauteil-Hintergrund (component_bg). */
    public static final int CARD = 0xFF1A2229;
    public static final int CARD_HOVER = 0xFF1D242B;
    public static final int CARD_PRESSED = 0xFF222C35;
    public static final int CARD_DISABLED = 0xFF10181F;

    // ── Ränder: sehr schwaches Weiß, keine gezeichneten Linien ───────────
    public static final int BORDER = 0x0CFFFFFF;
    public static final int BORDER_HI = 0x19FFFFFF;
    public static final int BORDER_PRESSED = 0x26FFFFFF;

    // ── Schrift ──────────────────────────────────────────────────────────
    public static final int TEXT = 0xFFD5DBFF;
    public static final int TEXT_DIM = 0xFF757883;
    public static final int TEXT_FAINT = 0xFF4B5460;

    // ── Marke und Zustände ───────────────────────────────────────────────
    public static final int ACCENT = 0xFF2B4BFF;
    public static final int ACCENT_HOVER = 0xFF2843DD;
    public static final int ACCENT_PRESSED = 0xFF3957FF;
    public static final int ACCENT_DIM = 0xFF1F2F81;
    public static final int ACCENT_BG = 0x332B4BFF;
    public static final int DANGER = 0xFFFF4444;
    public static final int DANGER_HOVER = 0xFFD63434;
    public static final int OK = 0xFF239A60;
    /** Kaum sichtbarer Schleier für Hover-Zustände (ghost_overlay). */
    public static final int GHOST = 0x0CFFFFFF;
    public static final int GHOST_HOVER = 0x1AFFFFFF;

    /*
     * Eckradien als Skala, wie in OneConfigs In-Game-Oberfläche. Jede runde
     * Fläche greift eine Stufe ab, statt eine eigene Zahl zu erfinden — so
     * wirken verschachtelte Formen wie eine Familie.
     */
    /** Kästchen, Schieberegler-Knopf, Farbfeld. */
    public static final int R_XS = 6;
    /** Knöpfe, Chips, Listeneinträge. */
    public static final int R_SMALL = 8;
    /** Einstellungszeilen und Karten. */
    public static final int R_CARD = 16;
    /** Fenster, Klappmenüs, alles was gepolsterten Inhalt umschließt. */
    public static final int R_PANEL = 20;

    private VStyle() {
    }

    /**
     * Radius einer eingebetteten Fläche, damit beide Rundungen gleichmäßig
     * bleiben: innen = außen − Abstand, nach unten auf null begrenzt. Ohne
     * diese Regel kneift die Rundung an der Diagonale.
     */
    public static int concentric(int aussen, int abstand) {
        return Math.max(0, aussen - abstand);
    }

    /**
     * Rechteck mit runden Ecken. Minecrafts DrawContext kann nur Kanten
     * füllen, also wird zeilenweise gezeichnet: für jede Zeile der Ecke
     * ergibt der Kreissatz, wie weit sie eingerückt gehört.
     */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0 || w < 2 * r || h < 2 * r) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** Rahmen (1px) um einen Bereich — für rechteckige Flächen. */
    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.drawHorizontalLine(x, x + w - 1, y, color);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
        ctx.drawVerticalLine(x, y, y + h - 1, color);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, color);
    }

    /**
     * Haarlinien-Rahmen um ein rundes Rechteck. Gezeichnet wird als etwas
     * größeres Rechteck darunter — billiger als ein echter Umriss und bei
     * einem Pixel Stärke nicht zu unterscheiden.
     */
    public static void roundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        roundRect(ctx, x - 1, y - 1, w + 2, h + 2, r + 1, color);
    }

    /** Weicher Schlagschatten aus mehreren blassen Schichten. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r) {
        for (int i = 4; i >= 1; i--) {
            roundRect(ctx, x - i, y - i + 2, w + 2 * i, h + 2 * i, r + i, ((0x0C - i) << 24));
        }
    }

    /** Haupt-Panel: angehobene Fläche mit Haarlinie und Schatten. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        shadow(ctx, x, y, w, h, R_PANEL);
        roundOutline(ctx, x, y, w, h, R_PANEL, BORDER);
        roundRect(ctx, x, y, w, h, R_PANEL, PANEL);
    }

    /** Karte/Zeile innerhalb eines Panels. */
    public static void card(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        roundOutline(ctx, x, y, w, h, R_CARD, hovered ? BORDER_HI : BORDER);
        roundRect(ctx, x, y, w, h, R_CARD, hovered ? CARD_HOVER : CARD);
    }

    /** Waagerechte Trennlinie zwischen Zeilen. */
    public static void separator(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
    }

    /** Schalter als Pille mit wanderndem Knopf. */
    public static void toggle(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        roundRect(ctx, x, y, w, h, h / 2, on ? ACCENT : GHOST_HOVER);
        int knob = h - 4;
        int kx = on ? x + w - knob - 2 : x + 2;
        roundRect(ctx, kx, y + 2, knob, knob, knob / 2, 0xFFFFFFFF);
    }
}
