package dev.visual.fabric;

import dev.visual.fabric.ui.VText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** HUD-Zeichnung (Crosshair, FPS, Keystrokes, Radar, Koordinaten, CPS). */
public final class HudOverlay {
    private static final int PANEL = 0xC80B0F14;
    private static final int PANEL_ACTIVE = 0xE022D3EE;
    private static final int TEXT = 0xFFE2E8F0;
    private static final int TEXT_DARK = 0xFF06222A;
    private static final int ACCENT = 0xFF22D3EE;

    private static PlayerEntity lastTarget;
    private static long lastTargetAt;

    private HudOverlay() {
    }

    /** Fadenkreuz in drei Stilen, Größe/Lücke/Kontur einstellbar. */
    public static void drawCrosshair(DrawContext ctx) {
        VConfig c = VConfig.get();
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int col = 0xFF000000 | c.crosshairColor;
        int len = Math.max(1, c.crosshairSize);
        int gap = Math.max(0, c.crosshairGap);
        switch (c.crosshairStyle) {
            case 1 -> { // Punkt
                if (c.crosshairOutline) ctx.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xC0000000);
                ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            }
            case 2 -> { // Kreis (Ring aus vier Bögen, grob gerastert)
                int r = len + gap;
                if (c.crosshairOutline) ring(ctx, cx, cy, r, 2, 0xC0000000);
                ring(ctx, cx, cy, r, 1, col);
                ctx.fill(cx, cy, cx + 1, cy + 1, col);
            }
            default -> { // Kreuz
                if (c.crosshairOutline) {
                    ctx.fill(cx - gap - len - 1, cy - 1, cx - gap + 1, cy + 2, 0xC0000000);
                    ctx.fill(cx + gap - 1, cy - 1, cx + gap + len + 1, cy + 2, 0xC0000000);
                    ctx.fill(cx - 1, cy - gap - len - 1, cx + 2, cy - gap + 1, 0xC0000000);
                    ctx.fill(cx - 1, cy + gap, cx + 2, cy + gap + len + 1, 0xC0000000);
                }
                ctx.fill(cx - gap - len, cy, cx - gap, cy + 1, col);
                ctx.fill(cx + gap, cy, cx + gap + len, cy + 1, col);
                ctx.fill(cx, cy - gap - len, cx + 1, cy - gap, col);
                ctx.fill(cx, cy + gap, cx + 1, cy + gap + len, col);
            }
        }
    }

    /** Grober Ring über vier Kanten — wenige Zeichenaufrufe. */
    private static void ring(DrawContext ctx, int cx, int cy, int r, int t, int col) {
        ctx.fill(cx - r, cy - r, cx + r + 1, cy - r + t, col);
        ctx.fill(cx - r, cy + r - t + 1, cx + r + 1, cy + r + 1, col);
        ctx.fill(cx - r, cy - r, cx - r + t, cy + r + 1, col);
        ctx.fill(cx + r - t + 1, cy - r, cx + r + 1, cy + r + 1, col);
    }

    /** Aktive Effekte mit den echten Vanilla-Sprites, links oben. */
    private static void drawPotions(DrawContext ctx, MinecraftClient mc, int x0, int y0) {
        if (mc.player == null) return;
        int y = y0;
        for (StatusEffectInstance eff : mc.player.getStatusEffects()) {
            // Vanilla-Effekttextur direkt laden — in allen Versionen derselbe Pfad,
            // im Gegensatz zum Sprite-Manager (ab 1.21.9 entfernt).
            Identifier id = Registries.STATUS_EFFECT.getId(eff.getEffectType().value());
            if (id != null) {
                Identifier tex = Identifier.of(id.getNamespace(),
                        "textures/mob_effect/" + id.getPath() + ".png");
                Compat.drawTex(ctx, tex, x0, y, 18, 18, 18, 18, 0xFFFFFFFF);
            }
            String time = StatusEffectUtil.getDurationText(eff, 1f, 20f).getString();
            ctx.drawText(mc.textRenderer, time, x0 + 22, y + 5, TEXT, true);
            y += 22;
        }
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        VConfig c = VConfig.get();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        if (c.fps) {
            int[] p = HudElements.pos("fps", w, h);
            infoLine(ctx, mc, mc.getCurrentFps() + " fps", p[0], p[1]);
        }
        if (c.cps) {
            int[] p = HudElements.pos("cps", w, h);
            infoLine(ctx, mc, ClickTracker.left() + " cps", p[0], p[1]);
        }
        if (c.combatTimer && CombatTimer.imKampf()) {
            int[] p = HudElements.pos("combat", w, h);
            kampfAnzeige(ctx, mc, p[0], p[1]);
        }
        if (c.coords) {
            int[] p = HudElements.pos("coords", w, h);
            String text = String.format("%.0f %.0f %.0f  %s",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), facing(mc.player.getYaw()));
            infoLine(ctx, mc, text, p[0], p[1]);
        }
        if (c.ping) {
            int[] p = HudElements.pos("ping", w, h);
            infoLine(ctx, mc, ping(mc) + " ms", p[0], p[1]);
        }
        if (c.keystrokes) {
            int[] p = HudElements.pos("keystrokes", w, h);
            drawKeystrokes(ctx, mc, p[0], p[1]);
        }
        if (c.radar) {
            int[] p = HudElements.pos("radar", w, h);
            drawRadar(ctx, mc, c.radarRange, p[0], p[1]);
        }
        if (c.armorHud) {
            int[] p = HudElements.pos("armor", w, h);
            drawArmor(ctx, mc, p[0], p[1]);
        }
        if (c.potionHud) {
            int[] p = HudElements.pos("potions", w, h);
            drawPotions(ctx, mc, p[0], p[1]);
        }
        if (c.scoreboardClean) {
            int[] p = HudElements.pos("scoreboard", w, h);
            drawScoreboard(ctx, mc, p[0], p[1]);
        }
        if (c.watermark) {
            int[] p = HudElements.pos("watermark", w, h);
            drawWatermark(ctx, mc, p[0], p[1]);
        }
        if (c.clock) {
            int[] p = HudElements.pos("clock", w, h);
            infoLine(ctx, mc, java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), p[0], p[1]);
        }
        if (c.targetHud) {
            int[] p = HudElements.pos("target", w, h);
            drawTargetHud(ctx, mc, p[0], p[1]);
        }
        if (c.itemCounter) {
            int[] p = HudElements.pos("items", w, h);
            drawItemCounter(ctx, mc, p[0], p[1]);
        }
        CombatOverlay.render(ctx, mc);
    }

    /**
     * Zeichnet ein Element für den HUD-Editor an beliebiger Stelle — mit echten
     * Daten, wo vorhanden, sonst mit Beispielwerten, damit auch abgeschaltete
     * oder gerade leere Anzeigen ihre wahre Größe zeigen.
     */
    public static void preview(DrawContext ctx, MinecraftClient mc, String key, int x, int y) {
        switch (key) {
            case "fps" -> infoLine(ctx, mc, mc.getCurrentFps() + " fps", x, y);
            case "cps" -> infoLine(ctx, mc, ClickTracker.left() + " cps", x, y);
            case "combat" -> kampfAnzeige(ctx, mc, x, y);
            case "ping" -> infoLine(ctx, mc, ping(mc) + " ms", x, y);
            case "clock" -> infoLine(ctx, mc, java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), x, y);
            case "coords" -> infoLine(ctx, mc, mc.player == null ? "0 0 0  N"
                    : String.format("%.0f %.0f %.0f  %s", mc.player.getX(), mc.player.getY(),
                    mc.player.getZ(), facing(mc.player.getYaw())), x, y);
            case "watermark" -> drawWatermark(ctx, mc, x, y);
            case "keystrokes" -> drawKeystrokes(ctx, mc, x, y);
            case "radar" -> drawRadar(ctx, mc, VConfig.get().radarRange, x, y);
            case "armor" -> {
                if (hasArmor(mc)) drawArmor(ctx, mc, x, y);
                else placeholder(ctx, mc, x, y, 100, 22, VText.t("hud.armorweapon"));
            }
            case "potions" -> {
                if (mc.player != null && !mc.player.getStatusEffects().isEmpty()) {
                    drawPotions(ctx, mc, x, y);
                } else {
                    placeholder(ctx, mc, x, y, 62, 22, "Effekte");
                }
            }
            case "items" -> {
                if (countsAny(mc)) drawItemCounter(ctx, mc, x, y);
                else placeholder(ctx, mc, x, y, 52, 60, "Items");
            }
            case "target" -> {
                if (lastTarget != null) drawTargetHud(ctx, mc, x, y);
                else placeholder(ctx, mc, x, y, 118, 40, VText.t("hud.target"));
            }
            case "scoreboard" -> {
                if (mc.world != null && mc.world.getScoreboard()
                        .getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null) {
                    drawScoreboard(ctx, mc, x, y);
                } else {
                    placeholder(ctx, mc, x, y, 110, 70, "Scoreboard");
                }
            }
            case "vanilla_hotbar" -> placeholder(ctx, mc, x, y, 182, 22, VText.t("hud.hotbar"));
            case "vanilla_status" -> placeholder(ctx, mc, x, y, 182, 20, VText.t("hud.status"));
            default -> {
            }
        }
    }

    private static boolean hasArmor(MinecraftClient mc) {
        if (mc.player == null) return false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) return true;
        }
        return false;
    }

    private static boolean countsAny(MinecraftClient mc) {
        if (mc.player == null) return false;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            Item item = inv.getStack(i).getItem();
            if (item == Items.TOTEM_OF_UNDYING || item == Items.ENDER_PEARL
                    || item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE
                    || item == Items.ARROW) {
                return true;
            }
        }
        return false;
    }

    /** Fläche in Originalgröße mit Beschriftung, wenn gerade keine Daten da sind. */
    private static void placeholder(DrawContext ctx, MinecraftClient mc, int x, int y,
                                    int w, int h, String label) {
        ctx.fill(x, y, x + w, y + h, 0x66101820);
        ctx.fill(x, y, x + 1, y + h, ACCENT);
        ctx.drawText(mc.textRenderer, "§8" + label, x + 5, y + (h - 8) / 2, 0xFF6C7C88, false);
    }

    /** Wortmarke im Client-Stil. */
    private static void drawWatermark(DrawContext ctx, MinecraftClient mc, int x, int y) {
        String a = "VISUAL";
        String b = "CLIENT";
        int wa = mc.textRenderer.getWidth(a);
        int total = wa + mc.textRenderer.getWidth(b) + 12;
        ctx.fill(x, y, x + total, y + 15, PANEL);
        ctx.fill(x, y, x + 1, y + 15, ACCENT);
        ctx.drawText(mc.textRenderer, a, x + 5, y + 4, TEXT, false);
        ctx.drawText(mc.textRenderer, b, x + 5 + wa, y + 4, ACCENT, false);
    }

    /**
     * Anzeige zum anvisierten Spieler: Name, Lebensbalken und Entfernung.
     * Bleibt nach dem Wegschauen kurz stehen, damit sie im Kampf nicht flackert.
     */
    private static void drawTargetHud(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity player) {
            lastTarget = player;
            lastTargetAt = System.currentTimeMillis();
        }
        if (lastTarget == null || System.currentTimeMillis() - lastTargetAt > 3000) return;
        PlayerEntity t = lastTarget;
        int w = 118;
        int h = 40;
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + 1, y + h, ACCENT);

        String name = t.getName().getString();
        ctx.drawText(mc.textRenderer, name, x + 6, y + 5, TEXT, false);

        float health = Math.max(0f, t.getHealth());
        float max = Math.max(1f, t.getMaxHealth());
        float ratio = Math.min(1f, health / max);
        int barW = w - 12;
        ctx.fill(x + 6, y + 18, x + 6 + barW, y + 24, 0xC0101820);
        int col = ratio > 0.5f ? 0xFF4ADE80 : ratio > 0.25f ? 0xFFFACC15 : 0xFFEF4444;
        ctx.fill(x + 6, y + 18, x + 6 + (int) (barW * ratio), y + 24, col);

        String hp = String.format(java.util.Locale.ROOT, "%.1f ❤", health
                + t.getAbsorptionAmount());
        ctx.drawText(mc.textRenderer, hp, x + 6, y + 28, TEXT, false);
        if (mc.player != null) {
            String dist = String.format(java.util.Locale.ROOT, "%.1f m",
                    mc.player.distanceTo(t));
            ctx.drawText(mc.textRenderer, dist,
                    x + w - mc.textRenderer.getWidth(dist) - 6, y + 28, 0xFF8A98A4, false);
        }
    }

    /** Zählt wichtige PvP-Items im Inventar und zeigt sie mit Item-Modell. */
    private static void drawItemCounter(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        Item[] watch = {Items.TOTEM_OF_UNDYING, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE,
                Items.ENDER_PEARL, Items.ARROW};
        var inv = mc.player.getInventory();
        int drawn = 0;
        for (Item item : watch) {
            int count = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (st.getItem() == item) count += st.getCount();
            }
            if (count == 0) continue;
            int row = y + drawn * 20;
            ctx.fill(x, row, x + 52, row + 18, PANEL);
            ctx.drawItem(new ItemStack(item), x + 2, row + 1);
            ctx.drawText(mc.textRenderer, String.valueOf(count), x + 24, row + 5, TEXT, false);
            drawn++;
        }
    }

    /**
     * Aufgeräumtes Scoreboard: transparente Fläche, keine roten Zahlen,
     * frei platzierbar. Ersetzt die Vanilla-Seitenleiste (im Mixin abgeschaltet).
     */
    private static void drawScoreboard(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.world == null) return;
        Scoreboard board = mc.world.getScoreboard();
        ScoreboardObjective obj = board.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return;

        List<String> lines = new ArrayList<>();
        for (ScoreboardEntry entry : board.getScoreboardEntries(obj)) {
            if (entry.hidden()) continue;
            Team team = board.getScoreHolderTeam(entry.owner());
            lines.add(Team.decorateName(team, entry.name()).getString());
        }
        java.util.Collections.reverse(lines);
        if (lines.size() > 15) lines = lines.subList(0, 15);

        String title = obj.getDisplayName().getString();
        int width = mc.textRenderer.getWidth(title);
        for (String l : lines) width = Math.max(width, mc.textRenderer.getWidth(l));
        width += 10;
        int height = 14 + lines.size() * 10 + 4;

        ctx.fill(x, y, x + width, y + height, 0x66000000);
        ctx.fill(x, y, x + width, y + 12, 0x88000000);
        ctx.fill(x, y, x + 1, y + height, ACCENT);
        ctx.drawText(mc.textRenderer, title, x + (width - mc.textRenderer.getWidth(title)) / 2,
                y + 2, TEXT, false);
        int ly = y + 15;
        for (String line : lines) {
            ctx.drawText(mc.textRenderer, line, x + 5, ly, TEXT, false);
            ly += 10;
        }
    }

    /** Ping des eigenen Spielers (0 im Einzelspieler). */
    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    /**
     * Rüstung + Waffe über der Hotbar: echte Item-Modelle statt nachgebauter
     * Symbole, darunter ein Haltbarkeitsbalken.
     */
    private static void drawArmor(DrawContext ctx, MinecraftClient mc, int ox, int oy) {
        if (mc.player == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        List<ItemStack> shown = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) shown.add(stack);
        }
        if (shown.isEmpty()) return;
        int step = 20;
        int x = ox;
        int y = oy;
        for (ItemStack stack : shown) {
            ctx.drawItem(stack, x + 2, y);
            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                float left = 1f - stack.getDamage() / (float) stack.getMaxDamage();
                int col = left > 0.5f ? 0xFF4ADE80 : left > 0.2f ? 0xFFFACC15 : 0xFFEF4444;
                ctx.fill(x + 2, y + 17, x + 18, y + 19, 0xC0000000);
                ctx.fill(x + 2, y + 17, x + 2 + (int) (16 * left), y + 19, col);
            }
            x += step;
        }
    }

    private static void infoLine(DrawContext ctx, MinecraftClient mc, String text, int x, int y) {
        int w = mc.textRenderer.getWidth(text) + 8;
        ctx.fill(x, y, x + w, y + 14, PANEL);
        ctx.fill(x, y, x + 1, y + 14, ACCENT);
        ctx.drawText(mc.textRenderer, text, x + 4, y + 3, TEXT, false);
    }

    private static String facing(float yaw) {
        float y = MathHelper.wrapDegrees(yaw);
        if (y >= -45 && y < 45) return "S";
        if (y >= 45 && y < 135) return "W";
        if (y >= 135 || y < -135) return "N";
        return "O";
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int s = 20, gap = 2;
        key(ctx, mc, x + s + gap, y, s, s, "W", mc.options.forwardKey.isPressed());
        key(ctx, mc, x, y + s + gap, s, s, "A", mc.options.leftKey.isPressed());
        key(ctx, mc, x + s + gap, y + s + gap, s, s, "S", mc.options.backKey.isPressed());
        key(ctx, mc, x + 2 * (s + gap), y + s + gap, s, s, "D", mc.options.rightKey.isPressed());
        int my = y + 2 * (s + gap);
        int mw = (3 * s + 2 * gap - gap) / 2;
        key(ctx, mc, x, my, mw, 14, "LMB", mc.options.attackKey.isPressed());
        key(ctx, mc, x + mw + gap, my, mw, 14, "RMB", mc.options.useKey.isPressed());
        key(ctx, mc, x, my + 16, 3 * s + 2 * gap, 12, "———", mc.options.jumpKey.isPressed());
    }

    private static void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h,
                            String label, boolean down) {
        ctx.fill(x, y, x + w, y + h, down ? PANEL_ACTIVE : PANEL);
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2,
                down ? TEXT_DARK : TEXT, false);
    }

    /** Player-Radar unten links: Punkte für Spieler in Reichweite, rotiert mit Blickrichtung. */
    private static void drawRadar(DrawContext ctx, MinecraftClient mc, int range, int ox, int oy) {
        if (mc.world == null) return;
        VConfig cfg = VConfig.get();
        int r = Math.max(24, cfg.radarSize);
        int cx = ox + r;
        int cy = oy + r;
        // Hintergrund + Rahmen
        ctx.fill(cx - r, cy - r, cx + r, cy + r, 0xC0060D0C);
        for (int i = 0; i < 360; i += 4) {
            double a = Math.toRadians(i);
            int px = cx + (int) (Math.cos(a) * r);
            int py = cy + (int) (Math.sin(a) * r);
            ctx.fill(px, py, px + 1, py + 1, 0x4022D3EE);
        }
        // Fadenkreuz
        ctx.fill(cx - r, cy, cx + r, cy + 1, 0x3022D3EE);
        ctx.fill(cx, cy - r, cx + 1, cy + r, 0x3022D3EE);
        // eigener Pfeil (Mitte)
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        // Nordausrichtung: Karte steht fest, sonst dreht sie mit dem Blick
        float yaw = cfg.radarNorthUp ? 180f : mc.player.getYaw();
        double sin = Math.sin(Math.toRadians(-yaw + 180));
        double cos = Math.cos(Math.toRadians(-yaw + 180));
        int count = 0;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dx = p.getX() - mc.player.getX();
            double dz = p.getZ() - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range) continue;
            count++;
            // relativ zur Blickrichtung rotieren, auf Radius skalieren
            double scale = (r - 4) / (double) range;
            double rx = (dx * cos - dz * sin) * scale;
            double rz = (dx * sin + dz * cos) * scale;
            int dotX = cx + (int) rx;
            int dotY = cy + (int) rz;
            int col = dist < 16 ? 0xFFF45568 : 0xFF22D3EE; // nah = rot
            ctx.fill(dotX - 2, dotY - 2, dotX + 3, dotY + 3, 0xC0000000);
            ctx.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, col);
            if (cfg.radarNames) {
                String name = p.getName().getString();
                if (name.length() > 8) name = name.substring(0, 8);
                ctx.drawText(mc.textRenderer, name,
                        dotX - mc.textRenderer.getWidth(name) / 2, dotY - 12, col, true);
            }
        }
        String label = count + VText.t("hud.players");
        ctx.drawText(mc.textRenderer, label, cx - r + 2, cy + r - 10,
                count > 0 ? ACCENT : 0xFF52756B, false);
    }

    /**
     * Kampf-Anzeige: Restzeit als Zahl mit einem Balken darunter. Der Balken
     * leert sich, damit man die Restzeit auch ohne Lesen abschaetzen kann.
     */
    private static void kampfAnzeige(DrawContext ctx, MinecraftClient mc, int x, int y) {
        String text = String.format(java.util.Locale.ROOT, "%.1fs",
                CombatTimer.verbleibend());
        infoLine(ctx, mc, text, x, y);

        int breite = 70;
        int balken = Math.round(breite * CombatTimer.anteil());
        ctx.fill(x, y + 14, x + breite, y + 17, 0x66000000);
        // Rot wenn es knapp wird, sonst der Akzentton
        int farbe = CombatTimer.anteil() < 0.25f ? 0xFFFF4444 : 0xFF2B4BFF;
        if (balken > 0) ctx.fill(x, y + 14, x + balken, y + 17, farbe);
    }
}
