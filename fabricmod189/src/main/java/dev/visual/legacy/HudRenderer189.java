package dev.visual.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.Window;

/**
 * Zeichnet die HUD-Bausteine. Wird aus dem InGameHud-Mixin aufgerufen,
 * nachdem Minecraft sein eigenes HUD gezeichnet hat.
 */
public final class HudRenderer189 {
    private static final int PAD = 4;

    private HudRenderer189() {
    }

    public static void render() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.textRenderer == null) return;
        // Im Bearbeiten-Bildschirm zeichnet dieser die Bausteine selbst
        if (mc.currentScreen instanceof VisualScreen189) return;

        Window w = new Window(mc);
        int sw = (int) w.getScaledWidth();
        int sh = (int) w.getScaledHeight();

        for (HudElement e : VisualsLegacy.ELEMENTS) {
            if (!e.enabled()) continue;
            drawElement(mc.textRenderer, e, sw, sh, false);
        }
        if (VConfig189.getBool("tasten.on", true)) {
            drawKeystrokes(mc, sw, sh, false);
        }
    }

    /** Ein Baustein an seiner gespeicherten Stelle. */
    public static void drawElement(TextRenderer tr, HudElement e, int sw, int sh,
                                   boolean highlight) {
        String text = e.text();
        int x = e.xPermille() * sw / 1000;
        int y = e.yPermille() * sh / 1000;
        int tw = tr.getStringWidth(text);
        VStyle189.roundRect(x, y, tw + PAD * 2, 12 + PAD, VStyle189.R_CARD,
                highlight ? VStyle189.CARD_HOVER : 0x66000000);
        tr.drawWithShadow(text, x + PAD, y + PAD, VStyle189.TEXT);
    }

    public static int[] elementBounds(TextRenderer tr, HudElement e, int sw, int sh) {
        int tw = tr.getStringWidth(e.text());
        return new int[]{e.xPermille() * sw / 1000, e.yPermille() * sh / 1000,
                tw + PAD * 2, 12 + PAD};
    }

    /** WASD plus Maustasten als Kachelblock. */
    public static void drawKeystrokes(MinecraftClient mc, int sw, int sh, boolean highlight) {
        int x = VConfig189.getInt("tasten.x", 20) * sw / 1000;
        int y = VConfig189.getInt("tasten.y", 300) * sh / 1000;
        int s = 16;
        int g = 2;
        if (highlight) {
            VStyle189.roundRect(x - 2, y - 2, 3 * s + 2 * g + 4, 2 * s + g + 4,
                    VStyle189.R_CARD, VStyle189.CARD_HOVER);
        }
        key(mc, x + s + g, y, s, mc.options.forwardKey, "W");
        key(mc, x, y + s + g, s, mc.options.leftKey, "A");
        key(mc, x + s + g, y + s + g, s, mc.options.backKey, "S");
        key(mc, x + 2 * (s + g), y + s + g, s, mc.options.rightKey, "D");
    }

    public static int[] keystrokeBounds(int sw, int sh) {
        int s = 16;
        int g = 2;
        return new int[]{VConfig189.getInt("tasten.x", 20) * sw / 1000,
                VConfig189.getInt("tasten.y", 300) * sh / 1000,
                3 * s + 2 * g, 2 * s + g};
    }

    private static void key(MinecraftClient mc, int x, int y, int s, KeyBinding kb, String label) {
        boolean down = kb.isPressed();
        VStyle189.roundRect(x, y, s, s, 3, down ? VStyle189.ACCENT : 0x66000000);
        int tw = mc.textRenderer.getStringWidth(label);
        mc.textRenderer.drawWithShadow(label, x + (s - tw) / 2f, y + (s - 8) / 2f,
                down ? 0xFF0B1416 : VStyle189.TEXT);
    }
}
