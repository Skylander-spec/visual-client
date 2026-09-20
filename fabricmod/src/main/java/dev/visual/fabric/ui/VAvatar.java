package dev.visual.fabric.ui;

import dev.visual.fabric.CapeManager;
import dev.visual.fabric.Compat;
import dev.visual.fabric.MiniSkin;
import dev.visual.fabric.VConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Der Spiegel: der eigene Avatar von vorn, so wie er gerade aussieht.
 *
 * Gezeichnet wird er aus den Flaechen der Skin-Textur statt als echtes
 * Modell. Das hat einen handfesten Grund: ein Entity-Modell braucht einen
 * Spieler, und auf dem Startbildschirm gibt es keinen - dort blieb die
 * Vorschau darum bisher leer. So steht der Avatar ueberall, im Spiel wie
 * davor, mit Umhang und mit dem Mini-Me auf der Schulter.
 *
 * Die Masse sind die des Minecraft-Spielermodells: der Kopf ist 8x8, der
 * Rumpf 8x12, Arme und Beine je 4x12, macht 16 breit und 32 hoch.
 */
public final class VAvatar {
    /** Hoehe der Figur in Modelleinheiten - Kopf 8, Rumpf 12, Beine 12. */
    public static final int EINHEITEN_HOCH = 32;
    /** Breite mit ausgestreckten Armen. */
    public static final int EINHEITEN_BREIT = 16;

    private VAvatar() {
    }

    /**
     * Zeichnet den Avatar so gross, dass er in ein Feld der Hoehe
     * {@code hoehe} passt, mittig auf {@code mitteX}.
     */
    public static void zeichne(DrawContext ctx, int mitteX, int oben, int hoehe) {
        int s = Math.max(1, hoehe / EINHEITEN_HOCH);
        Identifier haut = Compat.spielerHaut();
        if (haut == null) return;

        int cx = mitteX;
        int y = oben;

        // Umhang zuerst, er liegt hinter der Figur
        // Von vorn verschwindet ein Umhang fast vollstaendig hinter dem
        // Koerper - er ist nur eine Einheit breiter. Im Spiegel soll man
        // aber sehen, was man traegt, darum haengt er hier etwas breiter
        // und laenger als im Spiel.
        Identifier cape = CapeManager.get(net.minecraft.client.MinecraftClient.getInstance());
        if (cape != null) {
            teil(ctx, cape, cx - 7 * s, y + 7 * s, 14 * s, 21 * s, 1, 1, 10, 16, 64, 32);
        }

        // Kopf, Rumpf, Arme, Beine - erst die Grundschicht
        teil(ctx, haut, cx - 4 * s, y, 8 * s, 8 * s, 8, 8, 8, 8, 64, 64);
        teil(ctx, haut, cx - 4 * s, y + 8 * s, 8 * s, 12 * s, 20, 20, 8, 12, 64, 64);
        teil(ctx, haut, cx - 8 * s, y + 8 * s, 4 * s, 12 * s, 44, 20, 4, 12, 64, 64);
        teil(ctx, haut, cx + 4 * s, y + 8 * s, 4 * s, 12 * s, 36, 52, 4, 12, 64, 64);
        teil(ctx, haut, cx - 4 * s, y + 20 * s, 4 * s, 12 * s, 4, 20, 4, 12, 64, 64);
        teil(ctx, haut, cx, y + 20 * s, 4 * s, 12 * s, 20, 52, 4, 12, 64, 64);

        // Darueber die zweite Schicht: Hut, Jacke, Aermel, Hosenbeine
        teil(ctx, haut, cx - 4 * s, y, 8 * s, 8 * s, 40, 8, 8, 8, 64, 64);
        teil(ctx, haut, cx - 4 * s, y + 8 * s, 8 * s, 12 * s, 20, 36, 8, 12, 64, 64);
        teil(ctx, haut, cx - 8 * s, y + 8 * s, 4 * s, 12 * s, 44, 36, 4, 12, 64, 64);
        teil(ctx, haut, cx + 4 * s, y + 8 * s, 4 * s, 12 * s, 52, 52, 4, 12, 64, 64);
        teil(ctx, haut, cx - 4 * s, y + 20 * s, 4 * s, 12 * s, 4, 36, 4, 12, 64, 64);
        teil(ctx, haut, cx, y + 20 * s, 4 * s, 12 * s, 4, 52, 4, 12, 64, 64);

        miniMe(ctx, cx, y, s);
    }

    /**
     * Der Mini-Me auf seinem Sitzplatz. Gezeichnet wird er aus derselben
     * Textur, nur kleiner - so zeigt der Spiegel wirklich, was man traegt.
     */
    private static void miniMe(DrawContext ctx, int cx, int y, int s) {
        VConfig c = VConfig.get();
        if (!c.miniMe) return;
        Identifier haut = MiniSkin.textur();
        if (haut == null) haut = Compat.spielerHaut();
        if (haut == null) return;

        int ms = Math.max(1, Math.round(s * Math.max(0.15f, Math.min(0.9f, c.miniMeSize / 100f))));
        // Sitzplaetze wie im Spiel: 0 Kopf, 1 und 2 die Schultern
        int mx = switch (c.miniMePos) {
            case 1 -> cx + 6 * s;
            case 2 -> cx - 6 * s;
            default -> cx;
        };
        int my = c.miniMePos == 0 ? y - 16 * ms : y + 8 * s - 20 * ms;

        teil(ctx, haut, mx - 4 * ms, my, 8 * ms, 8 * ms, 8, 8, 8, 8, 64, 64);
        teil(ctx, haut, mx - 4 * ms, my + 8 * ms, 8 * ms, 12 * ms, 20, 20, 8, 12, 64, 64);
        teil(ctx, haut, mx - 8 * ms, my + 8 * ms, 4 * ms, 12 * ms, 44, 20, 4, 12, 64, 64);
        teil(ctx, haut, mx + 4 * ms, my + 8 * ms, 4 * ms, 12 * ms, 36, 52, 4, 12, 64, 64);
        teil(ctx, haut, mx - 4 * ms, my, 8 * ms, 8 * ms, 40, 8, 8, 8, 64, 64);
    }

    private static void teil(DrawContext ctx, Identifier tex, int x, int y, int b, int h,
                             float u, float v, int qb, int qh, int tb, int th) {
        try {
            Compat.drawTexAusschnitt(ctx, tex, x, y, b, h, u, v, qb, qh, tb, th, 0xFFFFFFFF);
        } catch (Throwable t) {
            // Eine fehlende Flaeche darf den Spiegel nicht reissen
        }
    }
}
