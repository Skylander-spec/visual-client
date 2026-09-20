package dev.visual.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** Config unter %APPDATA%/.visualclient/visualsfabric.json (launcher-geteilt). */
public class VConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VConfig instance;

    /** Sichtbarkeit eigener Rüstung: Helm, Brust, Hose, Schuhe. */
    public boolean[] armorSelf = {true, true, true, true};
    /** Sichtbarkeit der Rüstung anderer Spieler. */
    public boolean[] armorOthers = {true, true, true, true};

    public boolean crosshair = false;
    public int crosshairColor = 0x22D3EE;
    public boolean keystrokes = false;
    public boolean fps = false;

    // Extras (Ideen aus Lunar/Badlion/NoRisk)
    public boolean radar = false;
    public int radarRange = 64;
    public int radarSize = 46;        // Radius in Pixeln
    public boolean radarNames = false;
    public boolean radarNorthUp = false;
    /** Eigener Startbildschirm statt Minecrafts Auswahl. */
    public boolean customTitle = true;

    /**
     * Die von OneConfig gezeichneten Bildschirme (Start und Cosmetics).
     *
     * Standard aus. Sie sind gebaut und uebersetzen, aber ungeprueft im
     * Spiel - und ein ungeprueftes Stueck gehoert nicht auf den
     * Startbildschirm, denn wenn es dort klemmt, ist der ganze Client
     * unbenutzbar. Genau das ist einmal passiert. Wer sie sehen will,
     * schaltet sie hier an.
     */
    public boolean composeUi = false;

    /**
     * Minecrafts drehendes Panorama hinter dem Startbildschirm.
     *
     * Standard aus: das Vanilla-Panorama ist genau das, wonach ein
     * eigener Client nicht aussehen soll. Stattdessen zeichnen wir einen
     * eigenen Hintergrund. Wer das Panorama mag, schaltet es hier an.
     */
    public boolean titelPanorama = false;
    /** Minecrafts eigene Knoepfe im Client-Stil zeichnen. */
    public boolean clientMenues = true;

    public boolean fullbright = false;
    public boolean coords = false;
    public boolean cps = false;
    public boolean hitmarker = false;
    /** Zeigt an, wie lange man auf PvP-Servern noch im Kampf steht. */
    public boolean combatTimer = false;
    /** Wie lange ein Treffer nachwirkt, in Sekunden - je Server verschieden. */
    public int combatDauer = 15;
    public boolean hitSound = true;
    public boolean damageTint = false;
    public boolean armorHud = false;
    public boolean ping = false;
    /** Frei verschiebbare HUD-Positionen: Element-Schlüssel -> {x, y}. */
    public java.util.Map<String, int[]> hudPos = new java.util.HashMap<>();
    public int timeMode = 0;          // 0 = aus, 1 = Tag, 2 = Sonnenuntergang, 3 = Nacht
    public boolean scoreboardClean = false;
    public float mainHandX = 0f, mainHandY = 0f, mainHandZ = 0f, mainHandScale = 1f;
    public float offHandX = 0f, offHandY = 0f, offHandZ = 0f, offHandScale = 1f;
    public boolean vanillaHudMove = false;
    public boolean menuKey = true;   // Rechts-Shift öffnet das Menü
    public boolean watermark = false;
    public boolean clock = false;
    public boolean targetHud = false;
    public boolean itemCounter = false;
    public boolean freelook = false;
    public boolean screenShake = false;
    public boolean zoom = true;
    public boolean shoulderCam = false;
    public boolean hitParticles = false;
    public boolean potionHud = false;
    /** Mini-Me: verkleinerte Ausgabe des eigenen Spielers als Begleiter. */
    public boolean miniMe = false;
    public int miniMePos = 1;        // 0 = Kopf, 1 = linke Schulter, 2 = rechte Schulter
    public int miniMeSize = 35;      // Prozent der normalen Größe
    public boolean miniMeSitzt = true;   // sitzt statt zu stehen
    public int miniMeHut = 0;            // 0 = keiner, sonst Index in MiniMe.HUETE
    public boolean miniMeFluegel = false;
    public String miniMeSkin = "";       // Name einer PNG im Skin-Ordner, leer = eigener Skin
    public int crosshairStyle = 0;   // 0 = Kreuz, 1 = Punkt, 2 = Kreis
    public int crosshairSize = 5;
    public int crosshairGap = 2;
    public boolean crosshairOutline = true;
    public boolean skyTint = false;
    public int skyColor = 0x1A2A6C;

    public static VConfig get() {
        if (instance == null) load();
        return instance;
    }

    private static File file() {
        String appdata = System.getenv("APPDATA");
        File dir = appdata != null
                ? new File(appdata, ".visualclient")
                : new File(System.getProperty("user.home"), ".visualclient");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "visualsfabric.json");
    }

    public static void load() {
        File f = file();
        if (f.exists()) {
            try (FileReader r = new FileReader(f)) {
                instance = GSON.fromJson(r, VConfig.class);
            } catch (IOException ignored) {
            }
        }
        if (instance == null) instance = new VConfig();
        save();
    }

    public static void save() {
        try (FileWriter w = new FileWriter(file())) {
            GSON.toJson(get(), w);
        } catch (IOException ignored) {
        }
    }
}
