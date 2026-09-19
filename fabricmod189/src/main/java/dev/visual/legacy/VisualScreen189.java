package dev.visual.legacy;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

/**
 * Einstellungen und HUD-Editor in einem Bildschirm — geoeffnet mit der
 * rechten Umschalttaste.
 *
 * Links die Bausteine mit Schaltern, rechts die Vorschau: dort laesst sich
 * jeder eingeschaltete Baustein direkt an seinen Platz ziehen.
 */
public class VisualScreen189 extends Screen {
    private static final int ROW_H = 24;
    private static final int PANEL_W = 190;

    private String dragging;
    private int grabX;
    private int grabY;

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        // Eigener Hintergrund statt renderBackground(): das Spiel bleibt sichtbar
        net.minecraft.client.gui.DrawableHelper.fill(0, 0, width, height, VStyle189.BG);

        // Vorschau: alle eingeschalteten Bausteine an ihrer echten Stelle
        Window w = new Window(client);
        int sw = (int) w.getScaledWidth();
        int sh = (int) w.getScaledHeight();
        for (HudElement e : VisualsLegacy.ELEMENTS) {
            if (!e.enabled()) continue;
            int[] b = HudRenderer189.elementBounds(textRenderer, e, sw, sh);
            boolean hover = inside(mouseX, mouseY, b) || e.id.equals(dragging);
            HudRenderer189.drawElement(textRenderer, e, sw, sh, hover);
        }
        if (VConfig189.getBool("tasten.on", true)) {
            int[] b = HudRenderer189.keystrokeBounds(sw, sh);
            HudRenderer189.drawKeystrokes(client, sw, sh,
                    inside(mouseX, mouseY, b) || "tasten".equals(dragging));
        }

        // Einstellungsleiste
        int px = 10;
        int py = 10;
        int rows = VisualsLegacy.ELEMENTS.size() + 1;
        int ph = 44 + rows * ROW_H;
        VStyle189.panel(px, py, PANEL_W, ph);
        textRenderer.drawWithShadow("Visual", px + 14, py + 12, VStyle189.TEXT);
        textRenderer.drawWithShadow(VText189.t("ui.drag"), px + 14, py + 24,
                VStyle189.TEXT_FAINT);

        int y = py + 40;
        for (HudElement e : VisualsLegacy.ELEMENTS) {
            row(px, y, VText189.t(e.title), e.enabled(), mouseX, mouseY);
            y += ROW_H;
        }
        row(px, y, VText189.t("hud.keystrokes"), VConfig189.getBool("tasten.on", true),
                mouseX, mouseY);

        super.render(mouseX, mouseY, delta);
    }

    private void row(int px, int y, String label, boolean on, int mouseX, int mouseY) {
        boolean hover = mouseX >= px + 8 && mouseX <= px + PANEL_W - 8
                && mouseY >= y && mouseY <= y + ROW_H - 4;
        if (hover) {
            VStyle189.roundRect(px + 8, y, PANEL_W - 16, ROW_H - 4, VStyle189.R_CARD, 0x14FFFFFF);
        }
        textRenderer.drawWithShadow(label, px + 14, y + 6, VStyle189.TEXT);
        VStyle189.toggle(px + PANEL_W - 52, y + 1, on);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;

        // Schalter in der Leiste
        int px = 10;
        int y = 10 + 40;
        for (HudElement e : VisualsLegacy.ELEMENTS) {
            if (hitRow(mouseX, mouseY, px, y)) {
                e.setEnabled(!e.enabled());
                VConfig189.save();
                return;
            }
            y += ROW_H;
        }
        if (hitRow(mouseX, mouseY, px, y)) {
            VConfig189.setBool("tasten.on", !VConfig189.getBool("tasten.on", true));
            VConfig189.save();
            return;
        }

        // Baustein aufnehmen
        Window w = new Window(client);
        int sw = (int) w.getScaledWidth();
        int sh = (int) w.getScaledHeight();
        for (HudElement e : VisualsLegacy.ELEMENTS) {
            if (!e.enabled()) continue;
            int[] b = HudRenderer189.elementBounds(textRenderer, e, sw, sh);
            if (inside(mouseX, mouseY, b)) {
                dragging = e.id;
                grabX = mouseX - b[0];
                grabY = mouseY - b[1];
                return;
            }
        }
        if (VConfig189.getBool("tasten.on", true)) {
            int[] b = HudRenderer189.keystrokeBounds(sw, sh);
            if (inside(mouseX, mouseY, b)) {
                dragging = "tasten";
                grabX = mouseX - b[0];
                grabY = mouseY - b[1];
            }
        }
    }

    private boolean hitRow(int mouseX, int mouseY, int px, int y) {
        return mouseX >= px + 8 && mouseX <= px + PANEL_W - 8
                && mouseY >= y && mouseY <= y + ROW_H - 4;
    }

    /**
     * 1.8.9 kennt kein mouseDragged mit Zustand — die gedrueckte Maus wird
     * hier ueber mouseClickMove gemeldet.
     */
    @Override
    protected void mouseDragged(int mouseX, int mouseY, int button, long heldMs) {
        if (dragging == null) return;
        Window w = new Window(client);
        int sw = (int) w.getScaledWidth();
        int sh = (int) w.getScaledHeight();
        int nx = (mouseX - grabX) * 1000 / Math.max(1, sw);
        int ny = (mouseY - grabY) * 1000 / Math.max(1, sh);
        if ("tasten".equals(dragging)) {
            VConfig189.setInt("tasten.x", clamp(nx));
            VConfig189.setInt("tasten.y", clamp(ny));
        } else {
            HudElement e = VisualsLegacy.byId(dragging);
            if (e != null) e.setPos(nx, ny);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            VConfig189.save();
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 1000 ? 1000 : v;
    }

    private static boolean inside(int mx, int my, int[] b) {
        return mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
    }

    @Override
    protected void keyPressed(char chr, int code) {
        // Escape und die rechte Umschalttaste schliessen
        if (code == 1 || code == 54) {
            client.setScreen(null);
            return;
        }
        super.keyPressed(chr, code);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
