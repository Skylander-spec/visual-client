package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;

/**
 * Schaltet Minecrafts Menü-Weichzeichner aus, solange ein Client-Screen offen
 * ist. Der Blur wird beim Öffnen eines beliebigen Screens angewandt (Option
 * „Menü-Hintergrund-Unschärfe"), nicht erst in {@code renderBackground} —
 * deshalb reicht ein eigener Hintergrund allein nicht aus.
 */
public final class BlurGuard {
    private static int saved = -1;

    private BlurGuard() {
    }

    /** Beim Öffnen aufrufen: merkt den Wert und stellt auf 0. */
    public static void off() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        var option = mc.options.getMenuBackgroundBlurriness();
        if (saved < 0) saved = option.getValue();
        if (option.getValue() != 0) option.setValue(0);
    }

    /** Beim Schließen aufrufen: stellt die Einstellung des Spielers zurück. */
    public static void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || saved < 0) return;
        mc.options.getMenuBackgroundBlurriness().setValue(saved);
        saved = -1;
    }
}
