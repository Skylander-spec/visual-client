package dev.visual.visualsmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * JSON-Config unter %APPDATA%/.visualclient/visualsmod.json —
 * bewusst außerhalb des Spielordners, damit der Launcher dieselbe Datei
 * lesen/schreiben kann (Profil-übergreifend).
 */
public class VisualsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static VisualsConfig INSTANCE = new VisualsConfig();

    // HUD
    public boolean crosshairEnabled = true;
    public int crosshairStyle = 0;          // 0=Cross 1=Dot 2=Circle
    public int crosshairColor = 0x22D3EE;   // Cyan
    public boolean crosshairGlow = true;
    public boolean keystrokes = true;
    public boolean showCps = true;
    public boolean showFps = true;
    public boolean armorStatus = true;
    public boolean sidebarRestyle = true;
    public boolean hideSidebarNumbers = true;
    public boolean tabRestyle = true;

    // Effekte
    public boolean hitParticles = true;
    public int particleCount = 6;           // Performance-Regler (0-16)
    public boolean killEffect = true;
    public boolean killSound = true;
    public boolean hitSound = false;        // false = Sound kommt nur vom Resource Pack

    // Cosmetics
    public boolean capeEnabled = true;

    public static File configFile() {
        String appdata = System.getenv("APPDATA");
        File dir = appdata != null
                ? new File(appdata, ".visualclient")
                : new File(System.getProperty("user.home"), ".visualclient");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "visualsmod.json");
    }

    public static File cosmeticsDir() {
        File dir = new File(configFile().getParentFile(), "cosmetics");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void load() {
        File f = configFile();
        if (f.exists()) {
            try (FileReader r = new FileReader(f)) {
                VisualsConfig loaded = GSON.fromJson(r, VisualsConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (IOException ignored) {
            }
        }
        save();
    }

    public static void save() {
        try (FileWriter w = new FileWriter(configFile())) {
            GSON.toJson(INSTANCE, w);
        } catch (IOException ignored) {
        }
    }
}
