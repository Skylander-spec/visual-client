package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;

/**
 * Wie lange man noch im Kampf steht.
 *
 * Auf PvP-Servern gilt man nach einem Treffer eine Weile als "im Kampf" und
 * darf sich nicht ausloggen. Der Server sagt einem das nicht, also zaehlen
 * wir selbst: jeder ausgeteilte und jeder einsteckte Treffer setzt die Uhr
 * zurueck.
 *
 * Die Dauer ist einstellbar, weil sie von Server zu Server unterschiedlich
 * ist - 15 Sekunden sind der haeufigste Wert, aber eben nicht der einzige.
 */
public final class CombatTimer {
    /** Zeitpunkt, an dem der Kampf ablaeuft, in Millisekunden. */
    private static long laeuftAb = 0;
    /** Letzter gesehener Trefferzustand, um Flanken zu erkennen. */
    private static int letzterSchmerz = 0;

    private CombatTimer() {
    }

    /** Ein Treffer - ausgeteilt oder einsteckt. */
    public static void treffer() {
        laeuftAb = System.currentTimeMillis() + VConfig.get().combatDauer * 1000L;
    }

    /**
     * Jeden Tick pruefen, ob wir Schaden bekommen haben. hurtTime springt beim
     * Treffer auf 10 und zaehlt herunter; die steigende Flanke ist der Treffer.
     */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            laeuftAb = 0;
            letzterSchmerz = 0;
            return;
        }
        int schmerz = mc.player.hurtTime;
        if (schmerz > letzterSchmerz) treffer();
        letzterSchmerz = schmerz;
    }

    /** Laeuft gerade ein Kampf? */
    public static boolean imKampf() {
        return verbleibend() > 0;
    }

    /** Verbleibende Sekunden, 0 wenn kein Kampf laeuft. */
    public static float verbleibend() {
        long rest = laeuftAb - System.currentTimeMillis();
        return rest <= 0 ? 0f : rest / 1000f;
    }

    /** Anteil der verbleibenden Zeit, 0 bis 1 - fuer den Balken. */
    public static float anteil() {
        int dauer = Math.max(1, VConfig.get().combatDauer);
        return Math.max(0f, Math.min(1f, verbleibend() / dauer));
    }
}
