package dev.visual.legacy;

import net.minecraft.client.gui.DrawableHelper;

/**
 * Derselbe Apple-Look wie im 1.21er Mod und im Launcher — hier auf 1.8.9
 * umgesetzt. Gezeichnet wird ausschliesslich mit DrawableHelper.fill;
 * DrawContext gibt es in dieser Version noch nicht.
 */
public final class VStyle189 {
    public static final int ACCENT = 0xFF22D3EE;
    public static final int BG = 0xC0000000;
    public static final int PANEL = 0xF51C1C1E;
    public static final int CARD = 0xD92C2C2E;
    public static final int CARD_HOVER = 0xF03A3A3C;
    public static final int BORDER = 0x1AFFFFFF;
    public static final int TEXT = 0xFFF5F5F7;
    public static final int TEXT_DIM = 0x9EEBEBF5;
    public static final int TEXT_FAINT = 0x52EBEBF5;

    public static final int R_PANEL = 8;
    public static final int R_CARD = 5;

    private VStyle189() {
    }

    /**
     * Rechteck mit runden Ecken. Fuer jede Eckzeile liefert der Kreissatz,
     * wie weit sie eingerueckt gehoert.
     */
    public static void roundRect(int x, int y, int w, int h, int r, int color) {
        if (r <= 0 || w < 2 * r || h < 2 * r) {
            DrawableHelper.fill(x, y, x + w, y + h, color);
            return;
        }
        DrawableHelper.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            DrawableHelper.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            DrawableHelper.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** Weicher Schlagschatten aus mehreren blassen Schichten. */
    public static void shadow(int x, int y, int w, int h, int r) {
        for (int i = 4; i >= 1; i--) {
            roundRect(x - i, y - i + 2, w + 2 * i, h + 2 * i, r + i, ((0x0C - i) << 24));
        }
    }

    public static void panel(int x, int y, int w, int h) {
        shadow(x, y, w, h, R_PANEL);
        roundRect(x, y, w, h, R_PANEL, PANEL);
    }

    /** Schalter im macOS-Stil: gefuellte Pille, Knopf wandert. */
    public static void toggle(int x, int y, boolean on) {
        roundRect(x, y, 36, 18, 9, on ? ACCENT : 0x33FFFFFF);
        roundRect(on ? x + 19 : x + 2, y + 2, 14, 14, 7, 0xFFFFFFFF);
    }
}
