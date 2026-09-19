package dev.visual.visualsmod.hud;

import dev.visual.visualsmod.config.VisualsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Zentrales HUD: Custom Crosshair, Keystrokes + CPS, FPS, Armor-Status.
 * Alles rendert ohne Framebuffer/Shader — nur fill() und drawString().
 */
public class HudRenderer {
    private static final int PANEL = 0xC80B0F14;      // dunkles Panel, halbtransparent
    private static final int PANEL_ACTIVE = 0xE022D3EE;
    private static final int TEXT = 0xFFE2E8F0;
    private static final int TEXT_DARK = 0xFF06222A;

    @SubscribeEvent
    public void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        VisualsConfig c = VisualsConfig.INSTANCE;
        if (c.crosshairEnabled && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            event.setCanceled(true);
            drawCrosshair(event.getGuiGraphics(), c);
        }
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        VisualsConfig c = VisualsConfig.INSTANCE;
        GuiGraphics g = event.getGuiGraphics();

        if (c.keystrokes) drawKeystrokes(g, mc, c);
        if (c.showFps || c.showCps) drawInfo(g, mc, c);
        if (c.armorStatus) drawArmor(g, mc);
    }

    private void drawCrosshair(GuiGraphics g, VisualsConfig c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.player != null) {
            int cx = g.guiWidth() / 2;
            int cy = g.guiHeight() / 2;
            int col = 0xFF000000 | c.crosshairColor;
            int glow = 0x5522D3EE;
            switch (c.crosshairStyle) {
                case 1 -> {                                   // Dot
                    if (c.crosshairGlow) g.fill(cx - 2, cy - 2, cx + 2, cy + 2, glow);
                    g.fill(cx - 1, cy - 1, cx + 1, cy + 1, col);
                }
                case 2 -> {                                   // Circle
                    for (int i = -3; i <= 3; i++) {
                        int j = (int) Math.round(Math.sqrt(16 - i * i));
                        g.fill(cx + i, cy - j, cx + i + 1, cy - j + 1, col);
                        g.fill(cx + i, cy + j - 1, cx + i + 1, cy + j, col);
                        g.fill(cx - j, cy + i, cx - j + 1, cy + i + 1, col);
                        g.fill(cx + j - 1, cy + i, cx + j, cy + i + 1, col);
                    }
                    g.fill(cx, cy, cx + 1, cy + 1, col);
                }
                default -> {                                  // Cross
                    if (c.crosshairGlow) {
                        g.fill(cx - 6, cy - 1, cx + 6, cy + 1, glow);
                        g.fill(cx - 1, cy - 6, cx + 1, cy + 6, glow);
                    }
                    g.fill(cx - 5, cy, cx - 2, cy + 1, col);
                    g.fill(cx + 2, cy, cx + 5, cy + 1, col);
                    g.fill(cx, cy - 5, cx + 1, cy - 2, col);
                    g.fill(cx, cy + 2, cx + 1, cy + 5, col);
                }
            }
        }
    }

    private void key(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                     String label, boolean down) {
        g.fill(x, y, x + w, y + h, down ? PANEL_ACTIVE : PANEL);
        int tw = mc.font.width(label);
        g.drawString(mc.font, label, x + (w - tw) / 2, y + (h - 8) / 2,
                down ? TEXT_DARK : TEXT, false);
    }

    private void drawKeystrokes(GuiGraphics g, Minecraft mc, VisualsConfig c) {
        int x = 6, y = 6, s = 20, gap = 2;
        var o = mc.options;
        key(g, mc, x + s + gap, y, s, s, keyName(o.keyUp), o.keyUp.isDown());
        key(g, mc, x, y + s + gap, s, s, keyName(o.keyLeft), o.keyLeft.isDown());
        key(g, mc, x + s + gap, y + s + gap, s, s, keyName(o.keyDown), o.keyDown.isDown());
        key(g, mc, x + 2 * (s + gap), y + s + gap, s, s, keyName(o.keyRight), o.keyRight.isDown());
        int my = y + 2 * (s + gap);
        int mw = (3 * s + 2 * gap - gap) / 2;
        String lmb = c.showCps ? "LMB " + CpsTracker.leftCps() : "LMB";
        String rmb = c.showCps ? "RMB " + CpsTracker.rightCps() : "RMB";
        key(g, mc, x, my, mw, 14, lmb, o.keyAttack.isDown());
        key(g, mc, x + mw + gap, my, mw, 14, rmb, o.keyUse.isDown());
        key(g, mc, x, my + 16, 3 * s + 2 * gap, 12, "———", o.keyJump.isDown());
    }

    private String keyName(net.minecraft.client.KeyMapping key) {
        String n = key.getTranslatedKeyMessage().getString();
        return n.length() > 3 ? n.substring(0, 3) : n.toUpperCase();
    }

    private void drawInfo(GuiGraphics g, Minecraft mc, VisualsConfig c) {
        StringBuilder sb = new StringBuilder();
        if (c.showFps) sb.append(mc.getFps()).append(" fps");
        if (c.showFps && c.showCps) sb.append("  ·  ");
        if (c.showCps) sb.append(CpsTracker.leftCps()).append(" cps");
        String text = sb.toString();
        int w = mc.font.width(text) + 8;
        int x = g.guiWidth() - w - 6;
        g.fill(x, 6, x + w, 20, PANEL);
        g.drawString(mc.font, text, x + 4, 9, TEXT, false);
    }

    private void drawArmor(GuiGraphics g, Minecraft mc) {
        int x = g.guiWidth() - 26;
        int y = g.guiHeight() / 2 - 40;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                y += 18;
                continue;
            }
            g.fill(x - 2, y - 1, x + 18, y + 17, PANEL);
            g.renderItem(stack, x, y);
            if (stack.isDamageableItem()) {
                float dur = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
                int barCol = dur > 0.5f ? 0xFF22D3EE : dur > 0.2f ? 0xFFFACC15 : 0xFFF45568;
                g.fill(x - 2, y + 15, x - 2 + (int) (20 * dur), y + 17, barCol);
            }
            y += 20;
        }
    }
}
