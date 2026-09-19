package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/** Kampf-Feedback: Hitmarker am Fadenkreuz und roter Schaden-Rand. */
public final class CombatOverlay {
    private static final Identifier HITMARKER =
            Identifier.of("visualsfabric", "textures/gui/hitmarker.png");
    private static final Identifier VIGNETTE =
            Identifier.of("visualsfabric", "textures/gui/damage_vignette.png");
    /** Wie lange der Hitmarker nach einem Treffer sichtbar bleibt. */
    private static final long HIT_MS = 280;

    private static long lastHit = -HIT_MS;

    private CombatOverlay() {
    }

    /** Wird beim Angriff auf eine Entity aufgerufen (Fabric-Event, kein Mixin). */
    public static void onAttack(MinecraftClient mc) {
        VConfig c = VConfig.get();
        if (!c.hitmarker) return;
        lastHit = System.currentTimeMillis();
        if (c.hitSound && mc.player != null) {
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 1.7f);
        }
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        VConfig c = VConfig.get();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        if (c.damageTint && mc.player != null && mc.player.hurtTime > 0
                && mc.player.maxHurtTime > 0) {
            float f = mc.player.hurtTime / (float) mc.player.maxHurtTime;
            int alpha = (int) (Math.min(1f, f) * 200);
            Compat.drawTex(ctx, VIGNETTE, 0, 0, w, h, 256, 256,
                    (alpha << 24) | 0x00FFFFFF);
        }

        if (c.hitmarker) {
            long dt = System.currentTimeMillis() - lastHit;
            if (dt < HIT_MS) {
                float t = 1f - dt / (float) HIT_MS;
                int size = 9 + (int) (5 * (1f - t)); // kurzes Aufziehen
                int alpha = (int) (255 * Math.min(1f, t * 1.6f));
                Compat.drawTex(ctx, HITMARKER, w / 2 - size / 2, h / 2 - size / 2,
                        size, size, 32, 32, (alpha << 24) | 0x00FFFFFF);
            }
        }
    }
}
