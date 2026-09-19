package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

import java.lang.reflect.Method;

/**
 * Haelt unsere eigenen Bildschirme auf einer festen Arbeitsflaeche.
 *
 * Die GUI-Skalierung aus den Vanilla-Optionen zerreisst das Layout: bei
 * Stufe 4 bleiben von 1920 Pixeln nur 480 uebrig, da passt keine Spalte mehr
 * hin, bei Stufe 1 schwimmen die Kacheln in 1920 Pixeln Leere. Darum rechnen
 * wir uns beim Oeffnen eine eigene Stufe aus — die groesste, bei der noch
 * mindestens {@link #MIN_B}x{@link #MIN_H} uebrig bleiben — und geben die
 * Vanilla-Stufe beim Schliessen unveraendert zurueck.
 */
public final class VScale {
    /** So viel Platz braucht unser Layout mindestens. */
    public static final int MIN_B = 640;
    public static final int MIN_H = 360;

    /** Laeuft gerade einer unserer Bildschirme mit eigener Stufe? */
    private static boolean entliehen = false;

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
            // Lieber schief skaliert als gar kein Bildschirm.
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
            // s.o.
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

    // setScaleFactor/getScaleFactor nehmen bis 1.21.5 ein double, ab 1.21.6 ein
    // int. Ein Reflex-Aufruf spart uns fuenf Kopien dieser Klasse.

    private static int faktor(Window fenster) throws Exception {
        Method m = Window.class.getMethod("getScaleFactor");
        return ((Number) m.invoke(fenster)).intValue();
    }

    private static void setzen(Window fenster, int wert) throws Exception {
        try {
            Window.class.getMethod("setScaleFactor", int.class).invoke(fenster, wert);
        } catch (NoSuchMethodException alt) {
            Window.class.getMethod("setScaleFactor", double.class).invoke(fenster, (double) wert);
        }
    }
}
