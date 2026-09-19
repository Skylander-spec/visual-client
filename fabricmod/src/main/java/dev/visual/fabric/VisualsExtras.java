package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import org.lwjgl.glfw.GLFW;

/**
 * Tick-basierte Visuals ohne Mixin: Zoom, Schulterkamera und Treffer-Partikel.
 * Bewusst nur stabile Client-APIs, damit nichts am Renderpfad hängt.
 */
public final class VisualsExtras {
    private static final int ZOOM_KEY = GLFW.GLFW_KEY_C;
    private static final int CAM_KEY = GLFW.GLFW_KEY_B;

    /** Merkt, ob wir die Tageszeit gerade festhalten (zum sauberen Zurückschalten). */
    private static boolean skyOverridden = false;
    private static boolean zooming = false;
    private static double savedFov = -1;
    private static boolean camHeld = false;
    private static Perspective savedPerspective = null;

    private VisualsExtras() {
    }

    private static final int FREELOOK_KEY = GLFW.GLFW_KEY_LEFT_ALT;
    private static boolean freeActive = false;
    private static float lockYaw;
    private static float lockPitch;
    private static float freeYaw;
    private static float freePitch;

    public static boolean isFreelookActive() {
        return freeActive;
    }

    public static float freeYaw() {
        return freeYaw;
    }

    public static float freePitch() {
        return freePitch;
    }

    public static void tick(MinecraftClient mc) {
        VConfig c = VConfig.get();
        handleZoom(mc, c);
        handleCamera(mc, c);
        handleSky(mc, c);
        handleFreelook(mc, c);
    }

    /**
     * Freelook ohne Eingriff in die Mauseingabe: Minecraft dreht den Spieler
     * ganz normal, wir übernehmen die Drehung als Blickrichtung der Kamera und
     * setzen den Spieler danach auf seine gesperrte Ausrichtung zurück. So
     * bleibt die Laufrichtung stehen, während sich der Blick löst.
     */
    private static void handleFreelook(MinecraftClient mc, VConfig c) {
        if (!c.freelook || mc.player == null) {
            freeActive = false;
            return;
        }
        boolean down = mc.currentScreen == null && Compat.isKeyDown(mc, FREELOOK_KEY);
        if (down && !freeActive) {
            lockYaw = mc.player.getYaw();
            lockPitch = mc.player.getPitch();
            freeYaw = lockYaw;
            freePitch = lockPitch;
            freeActive = true;
        } else if (down) {
            freeYaw += mc.player.getYaw() - lockYaw;
            freePitch = Math.max(-90f, Math.min(90f, freePitch + mc.player.getPitch() - lockPitch));
            mc.player.setYaw(lockYaw);
            mc.player.setPitch(lockPitch);
        } else if (freeActive) {
            freeActive = false;
        }
    }

    /**
     * Custom Sky über die Tageszeit: setzt die Uhrzeit clientseitig fest.
     * Rein optisch — der Server behält seine eigene Zeit, deshalb wird sie
     * jeden Tick nachgezogen.
     */
    private static void handleSky(MinecraftClient mc, VConfig c) {
        if (mc.world == null) {
            skyOverridden = false;
            return;
        }
        if (c.timeMode == 0) {
            // Beim Ausschalten die Zeit wieder laufen lassen — sonst bleibt die
            // Welt in der zuletzt gesetzten Tageszeit stehen.
            if (skyOverridden) {
                mc.world.setTime(mc.world.getTime(), mc.world.getTimeOfDay(), true);
                skyOverridden = false;
            }
            return;
        }
        long timeOfDay = switch (c.timeMode) {
            case 1 -> 6000L;    // Mittag
            case 2 -> 12000L;   // Sonnenuntergang
            case 3 -> 18000L;   // Nacht
            default -> 6000L;
        };
        mc.world.setTime(mc.world.getTime(), timeOfDay, false);
        skyOverridden = true;
    }

    /** Zoom auf gehaltener Taste C — setzt nur das Sichtfeld, kein Rendereingriff. */
    private static void handleZoom(MinecraftClient mc, VConfig c) {
        if (!c.zoom || mc.player == null) {
            if (zooming) restoreFov(mc);
            return;
        }
        boolean down = mc.currentScreen == null && Compat.isKeyDown(mc, ZOOM_KEY);
        if (down && !zooming) {
            savedFov = mc.options.getFov().getValue();
            mc.options.getFov().setValue((int) Math.max(10, savedFov / 4));
            zooming = true;
        } else if (!down && zooming) {
            restoreFov(mc);
        }
    }

    private static void restoreFov(MinecraftClient mc) {
        if (savedFov > 0) mc.options.getFov().setValue((int) savedFov);
        savedFov = -1;
        zooming = false;
    }

    /** Schulterkamera auf gehaltener Taste B (danach wieder Ego-Sicht). */
    private static void handleCamera(MinecraftClient mc, VConfig c) {
        if (!c.shoulderCam || mc.player == null) {
            if (camHeld) restoreCamera(mc);
            return;
        }
        boolean down = mc.currentScreen == null && Compat.isKeyDown(mc, CAM_KEY);
        if (down && !camHeld) {
            savedPerspective = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            camHeld = true;
        } else if (!down && camHeld) {
            restoreCamera(mc);
        }
    }

    private static void restoreCamera(MinecraftClient mc) {
        if (savedPerspective != null) mc.options.setPerspective(savedPerspective);
        savedPerspective = null;
        camHeld = false;
    }

    /** Kleiner Partikel-Burst am getroffenen Gegner. */
    public static void hitParticles(MinecraftClient mc, Entity target) {
        if (!VConfig.get().hitParticles || mc.world == null) return;
        for (int i = 0; i < 8; i++) {
            double ox = (mc.world.random.nextDouble() - 0.5) * target.getWidth();
            double oy = mc.world.random.nextDouble() * target.getHeight() * 0.8 + 0.3;
            double oz = (mc.world.random.nextDouble() - 0.5) * target.getWidth();
            Compat.spawnParticle(mc.world, ParticleTypes.CRIT,
                    target.getX() + ox, target.getY() + oy, target.getZ() + oz,
                    (mc.world.random.nextDouble() - 0.5) * 0.3,
                    mc.world.random.nextDouble() * 0.2,
                    (mc.world.random.nextDouble() - 0.5) * 0.3);
        }
    }
}
