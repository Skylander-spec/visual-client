package dev.visual.fabric;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

/**
 * Unsere Module als OneConfig-Seite.
 *
 * OneConfig zeichnet sein Menue mit Compose ueber einem eigenen
 * Skia-Renderer. Diese Datei haengt sich daran, statt das Aussehen
 * nachzubauen - dadurch ist es wirklich dasselbe Menue und nicht etwas
 * Aehnliches. Kopiert wird nichts: die LGPL ist genau fuer das Einbinden
 * gemacht, und OneConfig liegt ohnehin schon als Mod im Paket.
 *
 * Die Felder spiegeln unsere Konfiguration. Geschrieben wird weiter in
 * VConfig, damit es nur eine Wahrheit gibt - OneConfig haelt nur die
 * Anzeige.
 */
public class VisualOneConfig extends Config {
    @Switch(title = "Fullbright")
    public boolean fullbright = false;

    @Switch(title = "Koordinaten")
    public boolean coords = false;

    @Switch(title = "CPS")
    public boolean cps = false;

    @Switch(title = "Trefferanzeige")
    public boolean hitmarker = false;

    @Switch(title = "Mini-Me")
    public boolean miniMe = false;

    @Slider(title = "Mini-Me Groesse", min = 10f, max = 90f, step = 5f)
    public float miniMeSize = 35f;

    public VisualOneConfig() {
        // Kein initialize(): OneConfigs eigenes Beispiel ruft es auch nicht,
        // die Methode ist intern. Der Konstruktor reicht.
        super("visualclient.json", "Visual Client", Category.QOL);
        vonVConfig();
        addCallback("fullbright", () -> uebernehmen());
        addCallback("coords", () -> uebernehmen());
        addCallback("cps", () -> uebernehmen());
        addCallback("hitmarker", () -> uebernehmen());
        addCallback("miniMe", () -> uebernehmen());
        addCallback("miniMeSize", () -> uebernehmen());
    }

    /** Startwerte aus unserer eigenen Konfiguration holen. */
    private void vonVConfig() {
        VConfig c = VConfig.get();
        fullbright = c.fullbright;
        coords = c.coords;
        cps = c.cps;
        hitmarker = c.hitmarker;
        miniMe = c.miniMe;
        miniMeSize = c.miniMeSize;
    }

    /** Aenderungen zurueck in unsere Konfiguration schreiben. */
    private void uebernehmen() {
        VConfig c = VConfig.get();
        c.fullbright = fullbright;
        c.coords = coords;
        c.cps = cps;
        c.hitmarker = hitmarker;
        c.miniMe = miniMe;
        c.miniMeSize = Math.round(miniMeSize);
        VConfig.save();
    }
}
