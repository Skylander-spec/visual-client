package dev.visual.fabric;

import net.minecraft.client.gui.DrawContext;

/**
 * Beschreibt alle frei verschiebbaren HUD-Bausteine an einer Stelle: Schlüssel,
 * Anzeigename, Größe (für den Editor) und Standardposition. Renderer und
 * HUD-Editor lesen beide hier — so können sie nicht auseinanderlaufen.
 */
public final class HudElements {
    /** {@code label} ist ein Sprachschluessel, kein fertiges Wort. */
    public record Element(String key, String label, int width, int height) {
    }

    public static final Element[] ALL = {
            new Element("fps", "hud.fps", 62, 14),
            new Element("cps", "hud.cps", 62, 14),
            new Element("coords", "hud.coords", 120, 14),
            new Element("ping", "hud.ping", 62, 14),
            new Element("keystrokes", "hud.keystrokes", 64, 72),
            new Element("radar", "hud.radar", 92, 92),
            new Element("armor", "hud.armor", 100, 22),
            new Element("potions", "hud.potions", 62, 22),
            new Element("scoreboard", "hud.scoreboard", 110, 70),
            new Element("watermark", "hud.watermark", 80, 15),
            new Element("clock", "hud.clock", 48, 14),
            new Element("target", "hud.target", 118, 40),
            new Element("combat", "hud.combat", 74, 22),
            new Element("items", "hud.items", 52, 60),
            new Element("vanilla_hotbar", "hud.hotbar", 182, 22),
            new Element("vanilla_status", "hud.status", 182, 20)
    };

    private HudElements() {
    }

    /**
     * Standardpositionen in drei Spalten (links, rechts, unten mittig), bewusst
     * so gestaffelt, dass sich nichts überdeckt — vorher lagen z. B. Scoreboard
     * und Potion-Effekte übereinander.
     */
    public static int[] defaultPos(String key, int screenW, int screenH) {
        return switch (key) {
            // rechte Spalte, von oben nach unten gestapelt
            case "fps" -> new int[]{screenW - 68, 6};
            case "cps" -> new int[]{screenW - 68, 24};
            case "combat" -> new int[]{screenW / 2 - 37, 54};
            case "ping" -> new int[]{screenW - 68, 42};
            case "coords" -> new int[]{screenW - 126, 78};
            case "keystrokes" -> new int[]{6, 6};
            case "radar" -> new int[]{6, screenH - 2 * Math.max(24, VConfig.get().radarSize) - 30};
            case "armor" -> new int[]{screenW / 2 - 50, screenH - 84};
            case "potions" -> new int[]{6, 84};
            case "scoreboard" -> new int[]{screenW - 116, 146};
            case "watermark" -> new int[]{6, screenH - 20};
            case "clock" -> new int[]{screenW - 68, 60};
            case "target" -> new int[]{screenW - 124, 98};
            case "items" -> new int[]{6, 112};
            case "vanilla_hotbar" -> new int[]{screenW / 2 - 91, screenH - 22};
            case "vanilla_status" -> new int[]{screenW / 2 - 91, screenH - 42};
            default -> new int[]{6, 6};
        };
    }

    /** Tatsächliche Größe — der Radar hängt an seiner Einstellung. */
    public static int[] size(Element e) {
        if (e.key().equals("radar")) {
            int s = Math.max(24, VConfig.get().radarSize) * 2;
            return new int[]{s, s};
        }
        return new int[]{e.width(), e.height()};
    }

    /** Gespeicherte Position oder Standard. */
    public static int[] pos(String key, int screenW, int screenH) {
        int[] p = VConfig.get().hudPos.get(key);
        if (p != null && p.length == 2) {
            // in den sichtbaren Bereich klemmen, falls die Auflösung kleiner wurde
            return new int[]{Math.max(0, Math.min(p[0], screenW - 10)),
                    Math.max(0, Math.min(p[1], screenH - 10))};
        }
        return defaultPos(key, screenW, screenH);
    }

    /** Verschiebung gegenüber der Vanilla-Standardposition. */
    public static int[] offset(String key, int screenW, int screenH) {
        int[] p = pos(key, screenW, screenH);
        int[] d = defaultPos(key, screenW, screenH);
        return new int[]{p[0] - d[0], p[1] - d[1]};
    }

    public static void set(String key, int x, int y) {
        VConfig.get().hudPos.put(key, new int[]{x, y});
    }

    /** Rahmen für den Editor. */
    public static void outline(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col);
        ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y, x + 1, y + h, col);
        ctx.fill(x + w - 1, y, x + w, y + h, col);
    }
}
