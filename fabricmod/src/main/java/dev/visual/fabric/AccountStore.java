package dev.visual.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest die Konten des Launchers und erlaubt, das aktive umzustellen.
 *
 * Wichtig zur Einordnung: eine laufende Minecraft-Sitzung kann ihr Konto
 * nicht wechseln — die Anmeldedaten stehen beim Start fest. Die Auswahl hier
 * schreibt deshalb nur das aktive Konto in die Datei des Launchers zurück;
 * wirksam wird sie beim nächsten Start. Das ist ehrlicher, als einen Wechsel
 * vorzutäuschen, der im Spiel gar nicht möglich ist.
 */
public final class AccountStore {
    /** Ein Konto, wie der Launcher es ablegt. */
    public record Konto(String id, String name, String type, String uuid) {
        public boolean microsoft() {
            return "microsoft".equals(type);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Konto> konten = new ArrayList<>();
    private static String aktiv = null;
    private static long zuletztGelesen = 0;

    private AccountStore() {
    }

    /** Konten des Launchers; höchstens einmal je zwei Sekunden neu gelesen. */
    public static List<Konto> alle() {
        pruefen();
        return konten;
    }

    public static String aktivId() {
        pruefen();
        return aktiv;
    }

    public static Konto aktivesKonto() {
        pruefen();
        for (Konto k : konten) if (k.id().equals(aktiv)) return k;
        return konten.isEmpty() ? null : konten.get(0);
    }

    private static void pruefen() {
        long jetzt = System.currentTimeMillis();
        if (jetzt - zuletztGelesen < 2000) return;
        zuletztGelesen = jetzt;
        lesen();
    }

    private static void lesen() {
        Path datei = datei();
        if (datei == null) return;
        try (Reader r = Files.newBufferedReader(datei)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            List<Konto> neu = new ArrayList<>();
            JsonArray arr = root.getAsJsonArray("accounts");
            if (arr != null) {
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    neu.add(new Konto(
                            text(o, "id"), text(o, "name"), text(o, "type"), text(o, "uuid")));
                }
            }
            konten = neu;
            aktiv = text(root, "active");
        } catch (Throwable t) {
            // Datei fehlt oder ist beschädigt — dann eben keine Kontenliste
        }
    }

    /**
     * Aktives Konto umstellen. Wirkt beim nächsten Start des Spiels.
     *
     * Gelesen und geschrieben wird dieselbe Datei, die der Launcher nutzt;
     * die übrigen Felder bleiben unangetastet, damit nichts verloren geht.
     */
    public static boolean waehlen(String id) {
        Path datei = datei();
        if (datei == null) return false;
        try {
            JsonObject root;
            try (Reader r = Files.newBufferedReader(datei)) {
                root = JsonParser.parseReader(r).getAsJsonObject();
            }
            root.addProperty("active", id);
            try (Writer w = Files.newBufferedWriter(datei)) {
                GSON.toJson(root, w);
            }
            aktiv = id;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String text(JsonObject o, String feld) {
        JsonElement e = o.get(feld);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    /**
     * Der Launcher legt die Konten global unter %APPDATA%/.visualclient ab;
     * das Spiel läuft im Instanzordner darunter, deshalb beide Wege.
     */
    private static Path datei() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            Path p = Paths.get(appData, ".visualclient", "accounts.json");
            if (Files.isRegularFile(p)) return p;
        }
        Path rel = Paths.get("..", "..", "accounts.json");
        return Files.isRegularFile(rel) ? rel : null;
    }
}
