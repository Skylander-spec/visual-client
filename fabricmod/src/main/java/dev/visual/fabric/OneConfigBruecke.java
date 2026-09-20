package dev.visual.fabric;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Bruecke zu OneConfig.
 *
 * OneConfig wird nur mitcompiliert, nicht mitgeliefert - es kommt als
 * eigener Mod aus dem Paket. Fehlt es, darf hier nichts passieren: schon
 * das Laden von {@link VisualOneConfig} wuerde sonst mit
 * NoClassDefFoundError abbrechen, weil die Oberklasse fehlt.
 *
 * Deshalb steht der Verweis ausschliesslich in {@link #anmelden()} hinter
 * der Pruefung - der Klassenlader fasst die Klasse erst an, wenn die
 * Methode wirklich laeuft.
 */
public final class OneConfigBruecke {
    private static Object seite;

    private OneConfigBruecke() {
    }

    public static boolean vorhanden() {
        try {
            return FabricLoader.getInstance().isModLoaded("oneconfig");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Meldet unsere Seite bei OneConfig an, wenn es da ist. */
    public static void anmelden() {
        if (!vorhanden() || seite != null) return;
        try {
            seite = new VisualOneConfig();
            System.out.println("[visualsfabric] Seite bei OneConfig angemeldet");
        } catch (Throwable t) {
            // Andere Fassung, andere API - dann bleibt unser eigenes Menue
            System.err.println("[visualsfabric] OneConfig gefunden, aber nicht nutzbar: " + t);
        }
    }
}
