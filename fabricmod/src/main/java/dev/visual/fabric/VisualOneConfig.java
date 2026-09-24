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

    @Slider(title = "Mini-Me Groesse", min = 15f, max = 80f, step = 5f)
    public float miniMeSize = 35f;

    public VisualOneConfig() {
        // Kein initialize(): OneConfigs eigenes Beispiel ruft es auch nicht,
        // die Methode ist intern. Der Konstruktor reicht.
        super("visualclient.json", "Visual Client", Category.QOL);
        vonVConfig();
        addCallback("fullbright", () -> { VConfig.get().fullbright = fullbright; VConfig.save(); });
        addCallback("coords", () -> { VConfig.get().coords = coords; VConfig.save(); });
        addCallback("cps", () -> { VConfig.get().cps = cps; VConfig.save(); });
        addCallback("hitmarker", () -> { VConfig.get().hitmarker = hitmarker; VConfig.save(); });
        addCallback("miniMe", () -> { VConfig.get().miniMe = miniMe; VConfig.save(); });
        addCallback("miniMeSize", () -> { VConfig.get().miniMeSize = Math.max(15, Math.min(80, Math.round(miniMeSize))); VConfig.save(); });
    }

    /** Startwerte aus unserer eigenen Konfiguration holen. */
    public void vonVConfig() {
        VConfig c = VConfig.get();
        fullbright = c.fullbright;
        coords = c.coords;
        cps = c.cps;
        hitmarker = c.hitmarker;
        miniMe = c.miniMe;
        miniMeSize = c.miniMeSize;
    }

    @org.polyfrost.oneconfig.api.config.v1.annotations.Button(title = "Alle Client-Module")
    public void modules() {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(dev.visual.fabric.ui.VisualHomeScreen.eigeneModule());
    }

    @org.polyfrost.oneconfig.api.config.v1.annotations.Button(title = "Cosmetics und Mini-Me")
    public void cosmetics() {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(dev.visual.fabric.ui.VisualCosmeticsScreen.oeffnen());
    }

    @org.polyfrost.oneconfig.api.config.v1.annotations.Button(title = "HUD bearbeiten")
    public void hud() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        mc.setScreen(new HudEditorScreen(mc.currentScreen));
    }
}
