package dev.visual.fabric.ui;

import dev.visual.fabric.AccountStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Startbildschirm nach dem Vorbild von OneClient: Wortmarke mittig, darunter
 * schmale durchscheinende Knöpfe, links die gespeicherten Server als
 * Schnellstart, rechts Konto und Aktionen.
 *
 * Die Bildschirme dahinter bleiben Minecrafts eigene — angefasst wird nur
 * die Auswahl davor. Deshalb funktionieren Welten, Server und Optionen
 * unverändert, auch mit anderen Mods.
 */
public class VisualTitleScreen extends VMouseScreen {
    private static final int RAND = 28;
    private static final int SPALTE = 196;
    private static final int MITTE_B = 232;
    private static final int KNOPF_H = 26;
    private static final int LUECKE = 8;
    private static final int ZEILE_H = 28;

    /** Eine anklickbare Fläche. */
    private record Feld(int x, int y, int w, int h, String symbol, String titel, String unter,
                        Runnable tun) {
        boolean trifft(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private final List<Feld> felder = new ArrayList<>();
    private final List<ServerInfo> server = new ArrayList<>();
    private boolean kontenOffen = false;

    /** Einmal fehlgeschlagen heißt: nicht jeden Frame erneut versuchen. */
    private boolean panoramaAus = false;

    public VisualTitleScreen() {
        super(Text.literal("Visual Client"));
    }

    @Override
    protected void init() {
        felder.clear();
        server.clear();
        MinecraftClient mc = MinecraftClient.getInstance();

        // Gespeicherte Server für den Schnellstart links
        try {
            ServerList liste = new ServerList(mc);
            liste.loadFile();
            for (int i = 0; i < Math.min(3, liste.size()); i++) server.add(liste.get(i));
        } catch (Throwable t) {
            // Keine Serverdatei — dann bleibt die linke Spalte eben leer
        }

        // Mittelspalte
        int mx = (width - MITTE_B) / 2;
        int my = height / 2 - 6;
        felder.add(new Feld(mx, my, MITTE_B, KNOPF_H, "▣", VText.t("ui.singleplayer"), null,
                () -> mc.setScreen(new SelectWorldScreen(this))));
        felder.add(new Feld(mx, my + KNOPF_H + LUECKE, MITTE_B, KNOPF_H, "◉", VText.t("ui.multiplayer"),
                null, () -> mc.setScreen(new MultiplayerScreen(this))));
        felder.add(new Feld(mx, my + 2 * (KNOPF_H + LUECKE), MITTE_B, KNOPF_H, "⊕", VText.t("ui.realms"),
                null, () -> mc.setScreen(new RealmsMainScreen(this))));

        int halb = (MITTE_B - LUECKE) / 2;
        int ry = my + 3 * (KNOPF_H + LUECKE);
        felder.add(new Feld(mx, ry, halb, KNOPF_H, "⚙", VText.t("ui.options"), null,
                () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        felder.add(new Feld(mx + halb + LUECKE, ry, halb, KNOPF_H, "▤", VText.t("ui.modules"), null,
                () -> mc.setScreen(new VisualHomeScreen())));

        // Rechte Spalte unter dem Konto-Chip
        int rx = width - RAND - SPALTE;
        int ry2 = my + KNOPF_H + LUECKE;
        felder.add(new Feld(rx, ry2, SPALTE, KNOPF_H, "◈", VText.t("ui.cosmetics"), null,
                () -> mc.setScreen(new VisualHomeScreen(4))));
        felder.add(new Feld(rx, ry2 + KNOPF_H + LUECKE, SPALTE, KNOPF_H, "✕", VText.t("ui.quit"),
                null, mc::scheduleStop));

        // Schnellstart links
        int lx = RAND;
        int ly = my;
        for (int i = 0; i < server.size(); i++) {
            ServerInfo s = server.get(i);
            felder.add(new Feld(lx, ly + i * (ZEILE_H + 6), SPALTE, ZEILE_H, null, s.name,
                    s.playerCountLabel == null ? s.address : s.playerCountLabel.getString(),
                    () -> ConnectScreen.connect(this, mc, ServerAddress.parse(s.address), s,
                            false, null)));
        }
    }

    /**
     * Hintergrund: das drehende Panorama des Vanilla-Startbildschirms,
     * darüber ein dunkler Schleier, damit die Knöpfe lesbar bleiben.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean gezeichnet = false;
        if (!panoramaAus) {
            try {
                renderPanoramaBackground(ctx, delta);
                gezeichnet = true;
            } catch (Throwable t) {
                // Schlägt das Panorama fehl, bleibt der Verlauf als Rückfall
                panoramaAus = true;
            }
        }
        if (!gezeichnet) {
            ctx.fillGradient(0, 0, width, height, 0xFF0E1419, 0xFF11171C);
        }
        // Dunkler Schleier, damit die Knöpfe lesbar bleiben
        ctx.fill(0, 0, width, height, 0x73060A0E);
        ctx.fillGradient(0, 0, width, 90, 0x4D00060A, 0x00000000);
        ctx.fillGradient(0, height - 90, width, height, 0x00000000, 0x4D00060A);
    }

    /**
     * Ab 1.21.9 fragt Screen nach, ob sich das Panorama drehen darf —
     * Minecraft tut das nur auf dem Startbildschirm. Bewusst ohne
     * {@code @Override}: ältere Versionen kennen die Methode nicht, dort
     * bleibt sie ungenutzt liegen.
     */
    protected boolean allowRotatingPanorama() {
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        wortmarke(ctx);
        for (Feld f : felder) zeichneFeld(ctx, f, f.trifft(mouseX, mouseY));
        spaltenKoepfe(ctx);
        kontoChipZeichnen(ctx, mouseX, mouseY);
        fusszeile(ctx);
        if (kontenOffen) kontenListe(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Kopf ────────────────────────────────────────────────────────────

    private void wortmarke(DrawContext ctx) {
        int cx = width / 2;
        int cy = height / 2 - 82;

        // Bildmarke: vier Rauten um die Mitte
        int s = 7;
        VStyle.roundRect(ctx, cx - s / 2, cy - 18, s, s, 2, VStyle.TEXT);
        VStyle.roundRect(ctx, cx - s / 2, cy - 4, s, s, 2, VStyle.TEXT);
        VStyle.roundRect(ctx, cx - 14, cy - 11, s, s, 2, VStyle.ACCENT);
        VStyle.roundRect(ctx, cx + 7, cy - 11, s, s, 2, VStyle.ACCENT);

        String wort = "VISUAL CLIENT";
        int w = textRenderer.getWidth(wort);
        ctx.drawText(textRenderer, wort, cx - w / 2, cy + 12, VStyle.TEXT, false);
    }

    private void spaltenKoepfe(DrawContext ctx) {
        int my = height / 2 - 6;
        if (!server.isEmpty()) {
            ctx.drawText(textRenderer, VText.t("ui.quickstart"), RAND, my - 14, VStyle.TEXT_DIM, false);
        }
    }

    private void fusszeile(DrawContext ctx) {
        int y = height - 20;
        ctx.drawText(textRenderer, "VISUAL", RAND, y, VStyle.TEXT_DIM, false);
        int w = textRenderer.getWidth("VISUAL ");
        ctx.drawText(textRenderer, "CLIENT", RAND + w, y, VStyle.ACCENT, false);
        String v = "Fabric " + MinecraftClient.getInstance().getGameVersion();
        ctx.drawText(textRenderer, v, RAND + w + textRenderer.getWidth("CLIENT ") + 8, y,
                VStyle.TEXT_FAINT, false);
    }

    // ── Konto ───────────────────────────────────────────────────────────

    private int[] kontoChip() {
        return new int[]{width - RAND - SPALTE, height / 2 - 6, SPALTE, KNOPF_H};
    }

    private void kontoChipZeichnen(DrawContext ctx, int mouseX, int mouseY) {
        int[] c = kontoChip();
        AccountStore.Konto k = AccountStore.aktivesKonto();
        String name = k == null ? VText.t("ui.noaccount") : k.name();
        boolean hover = mouseX >= c[0] && mouseX <= c[0] + c[2]
                && mouseY >= c[1] && mouseY <= c[1] + c[3];
        glas(ctx, c[0], c[1], c[2], c[3], hover || kontenOffen);
        avatar(ctx, c[0] + 4, c[1] + 4, 18, name);
        ctx.drawText(textRenderer, name, c[0] + 28, c[1] + 9, VStyle.TEXT, false);
        ctx.drawText(textRenderer, kontenOffen ? "▴" : "▾", c[0] + c[2] - 14,
                c[1] + 9, VStyle.TEXT_DIM, false);
    }

    /** Platzhalter-Bild aus dem Anfangsbuchstaben, überall gleich verfügbar. */
    private void avatar(DrawContext ctx, int x, int y, int g, String name) {
        VStyle.roundRect(ctx, x, y, g, g, VStyle.R_XS, farbeAus(name));
        String b = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        ctx.drawText(textRenderer, b, x + (g - textRenderer.getWidth(b)) / 2,
                y + (g - 8) / 2, 0xFFFFFFFF, false);
    }

    private static int farbeAus(String name) {
        int h = name.hashCode();
        return 0xFF000000 | ((80 + Math.abs(h % 120)) << 16)
                | ((80 + Math.abs((h >> 8) % 120)) << 8) | (120 + Math.abs((h >> 16) % 110));
    }

    private int[] listeMasse() {
        int[] c = kontoChip();
        return new int[]{c[0], c[1] + c[3] + 6, c[2]};
    }

    private void kontenListe(DrawContext ctx, int mouseX, int mouseY) {
        List<AccountStore.Konto> liste = AccountStore.alle();
        int[] m = listeMasse();
        int h = 22 + Math.max(1, liste.size()) * ZEILE_H + 20;
        VStyle.panel(ctx, m[0], m[1], m[2], h);
        ctx.drawText(textRenderer, VText.t("ui.switchaccount"), m[0] + 12, m[1] + 8, VStyle.TEXT_DIM, false);

        String aktiv = AccountStore.aktivId();
        for (int i = 0; i < liste.size(); i++) {
            AccountStore.Konto k = liste.get(i);
            int ky = m[1] + 22 + i * ZEILE_H;
            boolean hover = mouseX >= m[0] + 6 && mouseX <= m[0] + m[2] - 6
                    && mouseY >= ky && mouseY <= ky + ZEILE_H - 4;
            if (hover) {
                VStyle.roundRect(ctx, m[0] + 6, ky, m[2] - 12, ZEILE_H - 4,
                        VStyle.concentric(VStyle.R_PANEL, 6), VStyle.GHOST_HOVER);
            }
            avatar(ctx, m[0] + 12, ky + 3, 18, k.name());
            ctx.drawText(textRenderer, k.name(), m[0] + 36, ky + 8,
                    k.id().equals(aktiv) ? VStyle.TEXT : VStyle.TEXT_DIM, false);
            if (k.id().equals(aktiv)) {
                ctx.drawText(textRenderer, "●", m[0] + m[2] - 20, ky + 8, VStyle.ACCENT, false);
            }
        }
        ctx.drawText(textRenderer, VText.t("ui.nextstart"), m[0] + 12, m[1] + h - 14,
                VStyle.TEXT_FAINT, false);
    }

    // ── Knöpfe ──────────────────────────────────────────────────────────

    /** Durchscheinende Fläche mit Haarlinie — der Grundbaustein aller Knöpfe. */
    private void glas(DrawContext ctx, int x, int y, int w, int h, boolean hover) {
        VStyle.roundOutline(ctx, x, y, w, h, VStyle.R_SMALL,
                hover ? VStyle.BORDER_HI : VStyle.BORDER);
        VStyle.roundRect(ctx, x, y, w, h, VStyle.R_SMALL, hover ? 0xB3222C35 : 0x991A2229);
    }

    private void zeichneFeld(DrawContext ctx, Feld f, boolean hover) {
        glas(ctx, f.x(), f.y(), f.w(), f.h(), hover);
        if (f.symbol() != null) {
            // Mittelspalten-Knopf: Symbol und Beschriftung zusammen mittig
            String s = f.symbol() + "  " + f.titel();
            int tw = textRenderer.getWidth(s);
            ctx.drawText(textRenderer, s, f.x() + (f.w() - tw) / 2, f.y() + (f.h() - 8) / 2,
                    hover ? VStyle.TEXT : VStyle.TEXT_DIM, false);
        } else {
            // Serverzeile: Bild links, Name oben, Spielerzahl darunter
            avatar(ctx, f.x() + 5, f.y() + 5, 18, f.titel());
            ctx.drawText(textRenderer, kuerzen(f.titel(), f.w() - 60), f.x() + 29, f.y() + 5,
                    VStyle.TEXT, false);
            if (f.unter() != null) {
                ctx.drawText(textRenderer, kuerzen(f.unter(), f.w() - 60), f.x() + 29,
                        f.y() + 16, VStyle.TEXT_FAINT, false);
            }
            ctx.drawText(textRenderer, "›", f.x() + f.w() - 14, f.y() + 9,
                    VStyle.TEXT_DIM, false);
        }
    }

    private String kuerzen(String text, int maxBreite) {
        if (textRenderer.getWidth(text) <= maxBreite) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (textRenderer.getWidth(sb.toString() + c + "…") > maxBreite) break;
            sb.append(c);
        }
        return sb + "…";
    }

    // ── Eingaben ────────────────────────────────────────────────────────

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int[] c = kontoChip();
        if (mouseX >= c[0] && mouseX <= c[0] + c[2] && mouseY >= c[1] && mouseY <= c[1] + c[3]) {
            kontenOffen = !kontenOffen;
            return true;
        }

        if (kontenOffen) {
            List<AccountStore.Konto> liste = AccountStore.alle();
            int[] m = listeMasse();
            for (int i = 0; i < liste.size(); i++) {
                int ky = m[1] + 22 + i * ZEILE_H;
                if (mouseX >= m[0] + 6 && mouseX <= m[0] + m[2] - 6
                        && mouseY >= ky && mouseY <= ky + ZEILE_H - 4) {
                    AccountStore.waehlen(liste.get(i).id());
                    kontenOffen = false;
                    return true;
                }
            }
            kontenOffen = false;
            return true;
        }

        for (Feld f : felder) {
            if (f.trifft(mouseX, mouseY)) {
                f.tun().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
