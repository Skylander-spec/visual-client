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
    private static final int RAND = 26;
    private static final int SPALTE = 146;
    private static final int MITTE_B = 218;
    private static final int KNOPF_H = 21;
    private static final int LUECKE = 9;
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
        super.init();
        felder.clear();
        server.clear();
        MinecraftClient mc = MinecraftClient.getInstance();

        // Gespeicherte Server für den Schnellstart links
        if (!schmal()) try {
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

        // Cosmetics und Beenden: neben der Mitte, wenn Platz ist — sonst
        // darunter. Bei Minecrafts Standardfenster (854x480) ueberlappten
        // sich die drei Spalten sonst zu einem Brei.
        int rx = schmal() ? mx : width - RAND - SPALTE;
        int rb = schmal() ? halb : SPALTE;
        int ry2 = schmal() ? ry + KNOPF_H + LUECKE : my + KNOPF_H + LUECKE;
        felder.add(new Feld(rx, ry2, rb, KNOPF_H, "◈", VText.t("ui.cosmetics"), null,
                () -> mc.setScreen(new VisualCosmeticsScreen())));
        felder.add(new Feld(schmal() ? rx + halb + LUECKE : rx,
                schmal() ? ry2 : ry2 + KNOPF_H + LUECKE, rb, KNOPF_H, "✕",
                VText.t("ui.quit"), null, mc::scheduleStop));

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
        // Weichzeichner statt Schleier: so macht es OneClient auch, nur mit
        // einem eigenen Mod. Minecraft bringt ihn seit 1.20.5 selbst mit.
        if (gezeichnet) {
            try {
                weichzeichnen(ctx);
            } catch (Throwable t) {
                // Kein Weichzeichner - dann reicht der leichte Schleier unten
            }
        }
        // Nur noch ein Hauch dunkler, damit die Schrift trägt
        ctx.fill(0, 0, width, height, 0x33060A0E);
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

    /** Hoehe der Wortmarke in Textzeilen — an der Vorlage abgemessen. */
    private static final float WORT_FAKTOR = 2.0f;
    /** Zusaetzlicher Buchstabenabstand der Wortmarke, in Pixeln vor dem Vergroessern. */
    private static final int WORT_SPERRE = 1;

    private void wortmarke(DrawContext ctx) {
        int cx = width / 2;
        int cy = height / 2 - 82;

        // Bildmarke: vier Rauten um die Mitte. Abgemessen an der Vorlage,
        // dort ist sie rund ein Drittel so hoch wie der Abstand zur ersten
        // Knopfreihe — deshalb deutlich groesser als frueher.
        int s = 10;
        VStyle.roundRect(ctx, cx - s / 2, cy - 26, s, s, 3, VStyle.TEXT);
        VStyle.roundRect(ctx, cx - s / 2, cy - 6, s, s, 3, VStyle.TEXT);
        VStyle.roundRect(ctx, cx - 20, cy - 16, s, s, 3, VStyle.ACCENT);
        VStyle.roundRect(ctx, cx + 10, cy - 16, s, s, 3, VStyle.ACCENT);

        // Wortmarke doppelt so gross und gesperrt gesetzt. Minecrafts Font
        // kennt keinen Buchstabenabstand, also wird Zeichen fuer Zeichen
        // gezeichnet und der Abstand von Hand dazugerechnet.
        String wort = "VISUAL CLIENT";
        int breite = 0;
        for (int i = 0; i < wort.length(); i++) {
            breite += textRenderer.getWidth(String.valueOf(wort.charAt(i))) + WORT_SPERRE;
        }
        breite -= WORT_SPERRE;

        skalieren(ctx, cx - breite * WORT_FAKTOR / 2f, cy + 14, WORT_FAKTOR);
        try {
            int x = 0;
            for (int i = 0; i < wort.length(); i++) {
                String z = String.valueOf(wort.charAt(i));
                ctx.drawText(textRenderer, VFont.t(z), x, 0, VStyle.TEXT, false);
                x += VFont.breite(textRenderer, z) + WORT_SPERRE;
            }
        } finally {
            zurueckskalieren(ctx);
        }
    }

    private void spaltenKoepfe(DrawContext ctx) {
        int my = height / 2 - 6;
        if (!server.isEmpty()) {
            ctx.drawText(textRenderer, VFont.t(VText.t("ui.quickstart")), RAND, my - 14, VStyle.TEXT_DIM, false);
        }
    }

    private void fusszeile(DrawContext ctx) {
        int y = height - 20;
        ctx.drawText(textRenderer, VFont.t("VISUAL"), RAND, y, VStyle.TEXT_DIM, false);
        int w = VFont.breite(textRenderer, "VISUAL ");
        ctx.drawText(textRenderer, VFont.t("CLIENT"), RAND + w, y, VStyle.ACCENT, false);
        String v = "Fabric " + MinecraftClient.getInstance().getGameVersion();
        ctx.drawText(textRenderer, VFont.t(v), RAND + w + VFont.breite(textRenderer, "CLIENT ") + 8, y,
                VStyle.TEXT_FAINT, false);
    }

    // ── Konto ───────────────────────────────────────────────────────────

    /**
     * Zu schmal fuer drei Spalten? Minecrafts Standardfenster ist 854x480,
     * und bei ueblicher GUI-Skalierung bleiben davon rund 430 Pixel Breite —
     * da passen Schnellstart, Mitte und rechte Spalte nicht nebeneinander.
     */
    private boolean schmal() {
        return width < 2 * (RAND + SPALTE) + MITTE_B + 2 * LUECKE;
    }

    private int[] kontoChip() {
        if (schmal()) {
            int b = Math.min(SPALTE, width - 2 * RAND);
            return new int[]{width - RAND - b, RAND, b, KNOPF_H};
        }
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
        ctx.drawText(textRenderer, VFont.t(name), c[0] + 28, c[1] + 9, VStyle.TEXT, false);
        ctx.drawText(textRenderer, VFont.t(kontenOffen ? "▴" : "▾"), c[0] + c[2] - 14,
                c[1] + 9, VStyle.TEXT_DIM, false);
    }

    /** Platzhalter-Bild aus dem Anfangsbuchstaben, überall gleich verfügbar. */
    private void avatar(DrawContext ctx, int x, int y, int g, String name) {
        VStyle.roundRect(ctx, x, y, g, g, VStyle.R_XS, farbeAus(name));
        String b = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        ctx.drawText(textRenderer, VFont.t(b), x + (g - VFont.breite(textRenderer, b)) / 2,
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
        ctx.drawText(textRenderer, VFont.t(VText.t("ui.switchaccount")), m[0] + 12, m[1] + 8, VStyle.TEXT_DIM, false);

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
            ctx.drawText(textRenderer, VFont.t(k.name()), m[0] + 36, ky + 8,
                    k.id().equals(aktiv) ? VStyle.TEXT : VStyle.TEXT_DIM, false);
            if (k.id().equals(aktiv)) {
                ctx.drawText(textRenderer, VFont.t("●"), m[0] + m[2] - 20, ky + 8, VStyle.ACCENT, false);
            }
        }
        ctx.drawText(textRenderer, VFont.t(VText.t("ui.nextstart")), m[0] + 12, m[1] + h - 14,
                VStyle.TEXT_FAINT, false);
    }

    // ── Knöpfe ──────────────────────────────────────────────────────────

    /** Durchscheinende Fläche mit Haarlinie — der Grundbaustein aller Knöpfe. */
    /**
     * Knopf auf dem Startbildschirm.
     *
     * Die Fuellung ist bewusst fast schwarz und nur halb deckend, kein
     * eingefaerbtes Blau: an der Vorlage nachgerechnet ergaben zwei Messungen
     * derselben Flaeche ueber verschiedenem Untergrund (37 ueber 69 und 68
     * ueber rund 130) einen Alphawert um 0,5 bei nahezu schwarzer Farbe.
     * Dadurch nimmt der Knopf den Ton des weichgezeichneten Panoramas an,
     * statt als blauer Balken darueber zu liegen.
     */
    private void glas(DrawContext ctx, int x, int y, int w, int h, boolean hover) {
        VStyle.roundRect(ctx, x, y, w, h, VStyle.R_SMALL, hover ? 0x99202428 : 0x80101214);
        VStyle.roundOutline(ctx, x, y, w, h, VStyle.R_SMALL,
                hover ? 0x33FFFFFF : 0x1AFFFFFF);
    }

    private void zeichneFeld(DrawContext ctx, Feld f, boolean hover) {
        glas(ctx, f.x(), f.y(), f.w(), f.h(), hover);
        if (f.symbol() != null) {
            // Mittelspalten-Knopf: Symbol und Beschriftung zusammen mittig
            String s = f.symbol() + "  " + f.titel();
            int tw = VFont.breite(textRenderer, s);
            ctx.drawText(textRenderer, VFont.t(s), f.x() + (f.w() - tw) / 2, f.y() + (f.h() - 8) / 2,
                    hover ? VStyle.TEXT : VStyle.TEXT_DIM, false);
        } else {
            // Serverzeile: Bild links, Name oben, Spielerzahl darunter
            avatar(ctx, f.x() + 5, f.y() + 5, 18, f.titel());
            ctx.drawText(textRenderer, VFont.t(kuerzen(f.titel(), f.w() - 60)), f.x() + 29, f.y() + 5,
                    VStyle.TEXT, false);
            if (f.unter() != null) {
                ctx.drawText(textRenderer, VFont.t(kuerzen(f.unter(), f.w() - 60)), f.x() + 29,
                        f.y() + 16, VStyle.TEXT_FAINT, false);
            }
            ctx.drawText(textRenderer, VFont.t("›"), f.x() + f.w() - 14, f.y() + 9,
                    VStyle.TEXT_DIM, false);
        }
    }

    private String kuerzen(String text, int maxBreite) {
        if (VFont.breite(textRenderer, text) <= maxBreite) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (VFont.breite(textRenderer, sb.toString() + c + "…") > maxBreite) break;
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
