package dev.visual.fabric.ui;

import dev.visual.fabric.HudEditorScreen;
import dev.visual.fabric.ModBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Das gemeinsame Panel unserer Bildschirme.
 *
 * Alle Masse sind an der Vorlage gemessen - bei 1920x1080 liegt das Panel
 * zwischen 96 und 1832 waagerecht und 107 und 972 senkrecht, die Trennlinie
 * zur Seitenleiste bei 423, der Inhalt beginnt bei 455 - und als Anteil der
 * Flaeche hinterlegt, damit sie auf jeder Fenstergroesse stimmen.
 *
 * Hier statt in jedem Bildschirm, weil Modul-Uebersicht und Kleiderschrank
 * denselben Rahmen tragen und zwei Kopien sonst auseinanderlaufen.
 */
public final class VPanel {
    private VPanel() {
    }

    public static int x(int breite) {
        return Math.round(breite * 0.050f);
    }

    public static int y(int hoehe) {
        return Math.round(hoehe * 0.099f);
    }

    public static int w(int breite) {
        return Math.round(breite * 0.904f);
    }

    public static int h(int hoehe) {
        return Math.round(hoehe * 0.801f);
    }

    public static int leisteW(int breite) {
        return Math.round(breite * 0.170f);
    }

    public static int inhaltX(int breite) {
        return x(breite) + leisteW(breite) + Math.round(breite * 0.017f);
    }

    public static int inhaltW(int breite) {
        return x(breite) + w(breite) - inhaltX(breite) - Math.round(breite * 0.015f);
    }

    /** Das Kreuz oben rechts: x, y, Breite, Hoehe. */
    public static int[] zuFeld(int breite, int hoehe) {
        int k = Math.round(hoehe * 0.033f);
        return new int[]{x(breite) + w(breite) - k - Math.round(breite * 0.012f),
                y(hoehe) + Math.round(hoehe * 0.030f), k, k};
    }

    public static void rahmen(DrawContext ctx, int breite, int hoehe) {
        int px = x(breite);
        int py = y(hoehe);
        int pw = w(breite);
        int ph = h(hoehe);
        VStyle.roundRect(ctx, px, py, pw, ph, VStyle.R_PANEL, 0xF00A0C10);
        VStyle.roundOutline(ctx, px, py, pw, ph, VStyle.R_PANEL, 0x14FFFFFF);
        int lw = leisteW(breite);
        ctx.fill(px + lw, py + 10, px + lw + 1, py + ph - 10, 0x14FFFFFF);
    }

    // ── Seitenleiste ────────────────────────────────────────────────────

    /** Ein Eintrag der Seitenleiste; Gruppe null = keine Ueberschrift davor. */
    public record Nav(String gruppe, String schluessel, String symbol, Runnable tun) {
    }

    public static List<Nav> eintraege(Screen von, String aktiv) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Nav> l = new ArrayList<>();
        l.add(new Nav("ui.secsettings", "ui.modules", "\u25a4",
                "ui.modules".equals(aktiv) ? null : () -> mc.setScreen(new VisualHomeScreen())));
        l.add(new Nav(null, "ui.hudedit", "\u25a6",
                "ui.hudedit".equals(aktiv) ? null : () -> mc.setScreen(new HudEditorScreen(von))));
        l.add(new Nav("ui.seclook", "ui.cosmetics", "\u25c8",
                "ui.cosmetics".equals(aktiv) ? null : () -> mc.setScreen(new VisualCosmeticsScreen())));
        l.add(new Nav("ui.secmore", "ui.modbrowser", "\u2699",
                "ui.modbrowser".equals(aktiv) ? null
                        : () -> mc.setScreen(new ModBrowserScreen(von, "mod"))));
        return l;
    }

    /**
     * Lage eines Nav-Eintrags. Ueberschriften brauchen eigene Zeilen, darum
     * werden sie mitgezaehlt - sonst saessen Klickflaeche und Beschriftung
     * auseinander.
     */
    public static int navY(List<Nav> nav, int i, int hoehe) {
        int yy = y(hoehe) + Math.round(hoehe * 0.148f);
        int zeile = Math.round(hoehe * 0.046f);
        int kopf = Math.round(hoehe * 0.030f);
        for (int k = 0; k < i; k++) {
            if (nav.get(k).gruppe() != null) yy += kopf;
            yy += zeile;
        }
        if (nav.get(i).gruppe() != null) yy += kopf;
        return yy;
    }

    public static int navH(int hoehe) {
        return Math.round(hoehe * 0.040f);
    }

    public static boolean navGeklickt(Screen von, double mouseX, double mouseY,
                                      int breite, int hoehe) {
        List<Nav> nav = eintraege(von, null);
        for (int i = 0; i < nav.size(); i++) {
            int ny = navY(nav, i, hoehe);
            if (mouseX >= x(breite) + 8 && mouseX <= x(breite) + leisteW(breite) - 8
                    && mouseY >= ny && mouseY <= ny + navH(hoehe)) {
                if (nav.get(i).tun() != null) nav.get(i).tun().run();
                return true;
            }
        }
        return false;
    }

    public static void seitenleiste(Screen von, DrawContext ctx, TextRenderer tr,
                                    int mouseX, int mouseY, int breite, int hoehe,
                                    String aktiv) {
        int px = x(breite);
        int lw = leisteW(breite);

        VFont.zeichne(ctx, tr, "VISUAL", px + 14, y(hoehe) + Math.round(hoehe * 0.042f),
                VStyle.TEXT);
        int wm = VFont.breite(tr, "VISUAL ");
        VFont.zeichne(ctx, tr, "CLIENT", px + 14 + wm, y(hoehe) + Math.round(hoehe * 0.042f),
                VStyle.ACCENT);

        List<Nav> nav = eintraege(von, aktiv);
        for (int i = 0; i < nav.size(); i++) {
            Nav n = nav.get(i);
            int ny = navY(nav, i, hoehe);
            if (n.gruppe() != null) {
                VFont.zeichne(ctx, tr, VText.t(n.gruppe()), px + 16,
                        ny - Math.round(hoehe * 0.022f), VStyle.TEXT_FAINT);
            }
            boolean ist = n.tun() == null;
            boolean hover = !ist && mouseX >= px + 8 && mouseX <= px + lw - 8
                    && mouseY >= ny && mouseY <= ny + navH(hoehe);
            if (ist || hover) {
                VStyle.roundRect(ctx, px + 8, ny, lw - 16, navH(hoehe), VStyle.R_SMALL,
                        ist ? VStyle.ACCENT : 0x14FFFFFF);
            }
            int farbe = ist ? 0xFFFFFFFF : hover ? VStyle.TEXT : VStyle.TEXT_DIM;
            VFont.zeichne(ctx, tr, n.symbol(), px + 16, ny + (navH(hoehe) - 8) / 2, farbe);
            VFont.zeichne(ctx, tr, VText.t(n.schluessel()), px + 30,
                    ny + (navH(hoehe) - 8) / 2, farbe);
        }

        String name = MinecraftClient.getInstance().getSession().getUsername();
        int fy = y(hoehe) + h(hoehe) - Math.round(hoehe * 0.055f);
        VStyle.roundRect(ctx, px + 14, fy, 14, 14, 4, VStyle.ACCENT_DIM);
        VFont.zeichneMittig(ctx, tr, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(),
                px + 21, fy + 3, 0xFFFFFFFF);
        VFont.zeichne(ctx, tr, name, px + 34, fy + 1, VStyle.TEXT);
        VFont.zeichne(ctx, tr, "Fabric " + MinecraftClient.getInstance().getGameVersion(),
                px + 34, fy + 10, VStyle.TEXT_FAINT);
    }

    public static void schliessen(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY,
                                  int breite, int hoehe) {
        int[] zu = zuFeld(breite, hoehe);
        boolean hover = mouseX >= zu[0] && mouseX <= zu[0] + zu[2]
                && mouseY >= zu[1] && mouseY <= zu[1] + zu[3];
        VStyle.roundRect(ctx, zu[0], zu[1], zu[2], zu[3], VStyle.R_SMALL,
                hover ? 0x33FF5555 : 0x14FFFFFF);
        VFont.zeichneMittig(ctx, tr, "\u2715", zu[0] + zu[2] / 2, zu[1] + (zu[3] - 8) / 2,
                hover ? 0xFFFF8888 : VStyle.TEXT_DIM);
    }
}
