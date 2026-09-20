package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

/**
 * Haelt unsere eigenen Bildschirme auf einer festen Arbeitsflaeche.
 *
 * Die GUI-Skalierung aus den Vanilla-Optionen zerreisst das Layout: bei
 * Stufe 4 bleiben von 1920 Pixeln nur 480 uebrig, da passt keine Spalte mehr
 * hin, bei Stufe 1 schwimmen die Kacheln in 1920 Pixeln Leere. Darum rechnen
 * wir uns beim Oeffnen eine eigene Stufe aus — die groesste, bei der noch
 * mindestens {@link #MIN_B}x{@link #MIN_H} uebrig bleiben — und geben die
 * Vanilla-Stufe beim Schliessen zurueck.
 */
public final class VScale {
    /** So viel Platz braucht unser Layout mindestens. */
    public static final int MIN_B = 640;
    public static final int MIN_H = 360;

    /** Laeuft gerade einer unserer Bildschirme mit eigener Stufe? */
    private static boolean entliehen = false;
    /** Einmal schiefgegangen heisst: nicht bei jedem Bildschirm neu klagen. */
    private static boolean gemeldet = false;

    private VScale() {
    }

    /** Setzt die feste Stufe und schreibt die neuen Masse in den Bildschirm. */
    public static void anwenden(Screen bildschirm) {
        try {
            Window fenster = MinecraftClient.getInstance().getWindow();
            entliehen = true;
            int soll = stufe(fenster);
            if (faktor(fenster) != soll) setzen(fenster, soll);
            bildschirm.width = fenster.getScaledWidth();
            bildschirm.height = fenster.getScaledHeight();
        } catch (Throwable t) {
            klagen(t);
        }
    }

    /** Gibt die Vanilla-Stufe zurueck, sobald keiner unserer Bildschirme mehr offen ist. */
    public static void zuruecksetzen() {
        try {
            if (!entliehen) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            Window fenster = mc.getWindow();
            // Neu ausrechnen statt gemerkt: zwischendurch kann das Fenster
            // seine Groesse geaendert haben, dann waere der alte Wert falsch.
            int zurueck = fenster.calculateScaleFactor(
                    mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            if (faktor(fenster) != zurueck) setzen(fenster, zurueck);
        } catch (Throwable t) {
            klagen(t);
        } finally {
            entliehen = false;
        }
    }

    /** Groesste Stufe, bei der die Mindestflaeche noch passt. */
    private static int stufe(Window fenster) {
        int b = fenster.getFramebufferWidth();
        int h = fenster.getFramebufferHeight();
        int f = 1;
        while (b / (f + 1) >= MIN_B && h / (f + 1) >= MIN_H) f++;
        return f;
    }

    // getScaleFactor liefert bis 1.21.5 ein double, ab 1.21.6 ein int; bei
    // setScaleFactor ist es umgekehrt. Beides schluckt der Java-Compiler ohne
    // Fallunterscheidung: der Cast passt auf beides, und ein int weitet sich
    // von allein zum double. (Reflection waere hier falsch — die Namen werden
    // zur Laufzeit remappt, getMethod("getScaleFactor") findet nichts.)

    private static int faktor(Window fenster) {
        return (int) fenster.getScaleFactor();
    }

    private static void setzen(Window fenster, int wert) {
        fenster.setScaleFactor(wert);
    }

    private static void klagen(Throwable t) {
        if (gemeldet) return;
        gemeldet = true;
        System.err.println("[visualsfabric] Feste UI-Groesse nicht moeglich: " + t);
    }
}
