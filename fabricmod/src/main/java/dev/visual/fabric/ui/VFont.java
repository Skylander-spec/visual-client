package dev.visual.fabric.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Unsere eigene Schrift statt Minecrafts Pixelfont.
 *
 * Der Grund, warum das Menue trotz passender Masse nie richtig aussah, war
 * nicht das Layout, sondern die Schrift: Minecrafts Font ist ein Raster aus
 * 8 Pixeln, die Vorlage setzt eine echte TTF. Mitgeliefert wird Poppins,
 * eine Schrift unter der SIL Open Font License - frei weiterzugeben, solange
 * die Lizenzdatei mitreist. Sie liegt als sky-font-license.txt daneben.
 */
public final class VFont {
    public static final Identifier SKY = Identifier.of("visualsfabric", "sky");
    public static final Identifier SKY_MEDIUM = Identifier.of("visualsfabric", "sky_medium");

    private VFont() {
    }

    /** Text in unserer Schrift. */
    public static Text t(String s) {
        return Text.literal(s).setStyle(VFontCompat.schrift(SKY));
    }

    /** Text in der halbfetten Schnittweite - fuer Ueberschriften. */
    public static Text m(String s) {
        return Text.literal(s).setStyle(VFontCompat.schrift(SKY_MEDIUM));
    }

    public static void zeichne(DrawContext ctx, TextRenderer tr, String s, int x, int y, int farbe) {
        ctx.drawText(tr, t(s), x, y, farbe, false);
    }

    public static void zeichneMittig(DrawContext ctx, TextRenderer tr, String s, int mitteX, int y,
                                     int farbe) {
        ctx.drawText(tr, t(s), mitteX - breite(tr, s) / 2, y, farbe, false);
    }

    public static int breite(TextRenderer tr, String s) {
        return tr.getWidth(t(s));
    }
}
