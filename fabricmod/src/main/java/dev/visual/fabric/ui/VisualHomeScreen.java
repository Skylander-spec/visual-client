package dev.visual.fabric.ui;

import dev.visual.fabric.VConfig;
import dev.visual.fabric.HudEditorScreen;
import dev.visual.fabric.ModBrowserScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Modul-Übersicht im Aufbau der Vorlage: ein Panel, das über dem
 * weichgezeichneten Spiel schwebt, links eine Seitenleiste mit Navigation,
 * rechts Kategorie-Pillen über einem Raster aus vier Kartenspalten.
 *
 * Alle Maße sind an der Vorlage gemessen (bei 1920x1080: Panel 96..1832 von
 * 107..972, Trennlinie bei 423, Karten 314 breit mit 24 Abstand, 127 hoch
 * plus 45 hoher Fußbalken) und als Anteil der Fläche hinterlegt, damit sie
 * auf jeder Fenstergröße stimmen.
 */
public class VisualHomeScreen extends VMouseScreen {

    /**
     * Den Modul-Bildschirm oeffnen - moeglichst den, den OneConfigs
     * Renderer zeichnet.
     *
     * Ueber Class.forName statt new, wie bei Titel und Cosmetics:
     * VModulesScreen erbt von OneConfig, und ohne OneConfig wirft schon
     * das Aufloesen der Klasse.
     */
    public static net.minecraft.client.gui.screen.Screen oeffnen() {
        if (!VConfig.get().composeUi) {
            return new VisualHomeScreen();
        }
        try {
            return (net.minecraft.client.gui.screen.Screen)
                    Class.forName("dev.visual.fabric.ui.VModulesScreen")
                            .getDeclaredConstructor()
                            .newInstance();
        } catch (Throwable fehler) {
            return new VisualHomeScreen();
        }
    }

    /** Reiter der oberen Leiste. Leer = alle Module. */
    private static final String[] REITER =
            {"tab.all", "tab.hud", "tab.combat", "tab.view", "tab.cosmetics"};
    private int reiter = 0;

    private List<VModule> modules = new ArrayList<>();
    private List<VModule> shown = new ArrayList<>();
    private TextFieldWidget search;
    private int scroll = 0;

    public VisualHomeScreen() {
        this(0);
    }

    /** Mit vorgewähltem Reiter öffnen — der Startbildschirm nutzt das für Cosmetics. */
    public VisualHomeScreen(int reiter) {
        super(Text.literal("Visual Client"));
        this.reiter = reiter;
    }

    @Override
    protected void init() {
        super.init();
        // Kein BlurGuard.off mehr: der Weichzeichner ist jetzt erwuenscht,
        // das Panel schwebt darueber wie in der Vorlage.
        modules = ModuleRegistry.all(this);
        // Lage und Breite setzt render() jedes Bild neu, weil sie am Panel
        // haengen - hier stehen nur Platzhalter.
        search = new TextFieldWidget(textRenderer, 0, 0, 100, 12,
                Text.literal(VText.t("ui.search")));
        // Kein Platzhalter am Widget: das zeichnet ihn mit Minecrafts
        // eigener Schrift, egal welchen Stil man setzt. render() malt ihn
        // selbst, solange nichts eingetippt ist.
        search.setDrawsBackground(false);
        search.setChangedListener(v -> {
            scroll = 0;
            filter();
        });
        addSelectableChild(search);
        filter();
    }

    /**
     * Minecraft setzt nach {@code init} den Anfangsfokus auf das erste
     * bedienbare Element — das wäre das Suchfeld, und dann landet jeder
     * Tastendruck dort statt im Menü. Also kein Vorfokus.
     */
    @Override
    protected void setInitialFocus() {
    }

    private void filter() {
        String q = search == null ? "" : search.getText();
        shown = modules.stream().filter(m -> m.matches(q)).filter(this::imReiter).toList();
    }

    /** Grobe Einteilung der Module auf die Reiter. */
    private boolean imReiter(VModule m) {
        if (reiter == 0) return true;
        String id = m.id;
        return switch (reiter) {
            case 1 -> id.contains("hud") || id.equals("keystrokes") || id.equals("counters")
                    || id.equals("armorhud") || id.equals("potions") || id.equals("scoreboard")
                    || id.equals("clock") || id.equals("watermark") || id.equals("items");
            case 2 -> id.equals("hitmarker") || id.equals("damage") || id.equals("armor")
                    || id.equals("target") || id.equals("radar") || id.equals("crosshair");
            case 3 -> id.equals("zoom") || id.equals("freelook") || id.equals("fullbright")
                    || id.equals("sky") || id.equals("hands") || id.equals("vanillahud");
            case 4 -> id.equals("cosmetics") || id.equals("minime") || id.equals("fakeplayer")
                    || id.equals("titlescreen") || id.equals("menukey")
                    || id.equals("mods") || id.equals("packs");
            default -> true;
        };
    }

    // ── Masse ───────────────────────────────────────────────────────────
    //
    // Alle Werte stammen aus der Vorlage, gemessen bei 1920x1080 und als
    // Anteil der Flaeche hinterlegt, damit sie auf jeder Fenstergroesse
    // passen: Panel 96..1832 von 107..972, Trennlinie zur Seitenleiste bei
    // 423, Inhalt ab 455. Karten 314 breit mit 24 Abstand, 127 hoch plus
    // 45 hoher Fussbalken, Zeilenabstand 198.

    private int panelX() {
        return Math.round(width * 0.050f);
    }

    private int panelY() {
        return Math.round(height * 0.099f);
    }

    private int panelW() {
        return Math.round(width * 0.904f);
    }

    private int panelH() {
        return Math.round(height * 0.801f);
    }

    /** Breite der Seitenleiste, gemessen bis zur Trennlinie. */
    private int leisteW() {
        return Math.round(width * 0.170f);
    }

    /** Linke Kante des Karteninhalts. */
    private int inhaltX() {
        return panelX() + leisteW() + Math.round(width * 0.017f);
    }

    private int inhaltW() {
        return panelX() + panelW() - inhaltX() - Math.round(width * 0.015f);
    }

    private int spalten() {
        return 4;
    }

    private int kartenAbstand() {
        return Math.max(4, Math.round(width * 0.0125f));
    }

    private int kartenB() {
        int a = kartenAbstand();
        return (inhaltW() - (spalten() - 1) * a) / spalten();
    }

    /** Dunkler Teil der Karte, ohne den Fussbalken. */
    private int kartenKopfH() {
        return Math.round(height * 0.1176f);
    }

    private int balkenH() {
        return Math.round(height * 0.0417f);
    }

    private int kartenH() {
        return kartenKopfH() + balkenH();
    }

    private int zeilenAbstand() {
        return kartenH() + Math.max(4, Math.round(height * 0.024f));
    }

    /** Oberkante der Kartenflaeche, unter Titel und Kategorie-Pillen. */
    private int gridTop() {
        return panelY() + Math.round(height * 0.148f);
    }

    /** Waagerechte Lage und Breite einer Kategorie-Pille. */
    private int pilleX(int i) {
        int x = inhaltX();
        for (int k = 0; k < i; k++) x += pilleB(k) + 6;
        return x;
    }

    private int pilleB(int i) {
        return VFont.breite(textRenderer, VText.t(REITER[i])) + 18;
    }

    private int pilleY() {
        return panelY() + Math.round(height * 0.105f);
    }

    private int pilleH() {
        return Math.round(height * 0.0408f);
    }



    private int maxScroll() {
        int reihen = (shown.size() + spalten() - 1) / spalten();
        int gesamt = reihen * zeilenAbstand();
        int sichtbar = panelY() + panelH() - gridTop() - 8;
        return Math.max(0, gesamt - sichtbar);
    }

    @Override
    protected boolean handleScroll(double amount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (amount * 24)));
        return true;
    }

    /**
     * Ein Eintrag der Seitenleiste. In der Vorlage steht dort die Navigation
     * zwischen den Bildschirmen, nicht die Kategorien - die sitzen als Pillen
     * ueber dem Raster.
     */
    private record Nav(String gruppe, String schluessel, String symbol, Runnable tun) {
    }

    private List<Nav> navEintraege() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Nav> l = new ArrayList<>();
        l.add(new Nav("ui.secsettings", "ui.modules", "\u25a4", null));
        l.add(new Nav(null, "ui.hudedit", "\u25a6", () -> mc.setScreen(new HudEditorScreen(this))));
        l.add(new Nav("ui.seclook", "ui.cosmetics", "\u25c8", () -> mc.setScreen(VisualCosmeticsScreen.oeffnen())));
        l.add(new Nav("ui.secmore", "ui.modbrowser", "\u2699",
                () -> mc.setScreen(new ModBrowserScreen(this, "mod"))));
        return l;
    }

    /**
     * Lage eines Nav-Eintrags. Ueberschriften brauchen eigene Zeilen, darum
     * werden sie mitgezaehlt - sonst saessen Klickflaeche und Beschriftung
     * auseinander.
     */
    private int navY(int i) {
        List<Nav> nav = navEintraege();
        int y = panelY() + Math.round(height * 0.148f);
        int zeile = Math.round(height * 0.046f);
        int kopf = Math.round(height * 0.030f);
        for (int k = 0; k < i; k++) {
            if (nav.get(k).gruppe() != null) y += kopf;
            y += zeile;
        }
        if (nav.get(i).gruppe() != null) y += kopf;
        return y;
    }

    private int navH() {
        return Math.round(height * 0.040f);
    }

    /** Das Kreuz oben rechts im Panel. */
    private int[] zuFeld() {
        int k = Math.round(height * 0.033f);
        return new int[]{panelX() + panelW() - k - Math.round(width * 0.012f),
                panelY() + Math.round(height * 0.030f), k, k};
    }

    private int sucheB() {
        return Math.round(width * 0.17f);
    }

    private int sucheLinks() {
        return inhaltX() + inhaltW() - sucheB() - zuFeld()[2] - 8;
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int[] zu = zuFeld();
        if (mouseX >= zu[0] && mouseX <= zu[0] + zu[2]
                && mouseY >= zu[1] && mouseY <= zu[1] + zu[3]) {
            close();
            return true;
        }

        List<Nav> nav = navEintraege();
        for (int i = 0; i < nav.size(); i++) {
            int ny = navY(i);
            if (mouseX >= panelX() + 8 && mouseX <= panelX() + leisteW() - 8
                    && mouseY >= ny && mouseY <= ny + navH()) {
                if (nav.get(i).tun() != null) nav.get(i).tun().run();
                return true;
            }
        }

        for (int i = 0; i < REITER.length; i++) {
            int bx = pilleX(i);
            if (mouseX >= bx && mouseX <= bx + pilleB(i)
                    && mouseY >= pilleY() && mouseY <= pilleY() + pilleH()) {
                reiter = i;
                scroll = 0;
                filter();
                return true;
            }
        }

        if (mouseY < gridTop()) return false;
        int cols = spalten();
        int cw = kartenB();
        for (int i = 0; i < shown.size(); i++) {
            int cx = inhaltX() + (i % cols) * (cw + kartenAbstand());
            int cy = gridTop() + (i / cols) * zeilenAbstand() - scroll;
            if (mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + kartenH()) {
                VModule m = shown.get(i);
                if (m.action != null) {
                    m.action.run();
                } else if (m.settings.isEmpty() && m.setEnabled != null) {
                    m.setEnabled.accept(!m.enabled.getAsBoolean());
                } else {
                    MinecraftClient.getInstance().setScreen(new ModuleDetailScreen(this, m));
                }
                return true;
            }
        }
        return false;
    }

    /** Einmal fehlgeschlagen heisst: nicht jeden Frame erneut versuchen. */
    private boolean panoramaAus = false;

    /**
     * Hintergrund wie in der Vorlage: Spiel beziehungsweise Panorama bleiben
     * sichtbar und werden weichgezeichnet, darueber schwebt das Panel. Frueher
     * lag hier eine deckende Flaeche ueber dem ganzen Bildschirm.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean gezeichnet = false;
        if (mc.world == null && !panoramaAus) {
            try {
                renderPanoramaBackground(ctx, delta);
                gezeichnet = true;
            } catch (Throwable t) {
                panoramaAus = true;
            }
        }
        if (mc.world == null && !gezeichnet) {
            ctx.fillGradient(0, 0, width, height, 0xFF0E1419, 0xFF11171C);
        }
        try {
            weichzeichnen(ctx);
        } catch (Throwable t) {
            // Ohne Weichzeichner reicht der Schleier
        }
        ctx.fill(0, 0, width, height, 0x66060A0E);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        VStyle.roundRect(ctx, px, py, pw, ph, VStyle.R_PANEL, 0xF00A0C10);
        VStyle.roundOutline(ctx, px, py, pw, ph, VStyle.R_PANEL, 0x14FFFFFF);
        int lw = leisteW();
        ctx.fill(px + lw, py + 10, px + lw + 1, py + ph - 10, 0x14FFFFFF);

        seitenleiste(ctx, mouseX, mouseY);

        VFont.zeichne(ctx, textRenderer, VText.t("ui.modules"), inhaltX(),
                py + Math.round(height * 0.042f), VStyle.TEXT);

        for (int i = 0; i < REITER.length; i++) {
            int bx = pilleX(i);
            int bw = pilleB(i);
            boolean aktiv = i == reiter;
            boolean hover = mouseX >= bx && mouseX <= bx + bw
                    && mouseY >= pilleY() && mouseY <= pilleY() + pilleH();
            VStyle.roundRect(ctx, bx, pilleY(), bw, pilleH(), pilleH() / 2,
                    aktiv ? VStyle.ACCENT : hover ? 0x1AFFFFFF : 0x0CFFFFFF);
            if (!aktiv) {
                VStyle.roundOutline(ctx, bx, pilleY(), bw, pilleH(), pilleH() / 2, 0x1AFFFFFF);
            }
            VFont.zeichneMittig(ctx, textRenderer, VText.t(REITER[i]), bx + bw / 2,
                    pilleY() + (pilleH() - 8) / 2, aktiv ? 0xFFFFFFFF : VStyle.TEXT_DIM);
        }

        ctx.enableScissor(inhaltX(), gridTop(), inhaltX() + inhaltW(), py + ph - 6);
        int cols = spalten();
        int cw = kartenB();
        for (int i = 0; i < shown.size(); i++) {
            int cx = inhaltX() + (i % cols) * (cw + kartenAbstand());
            int cy = gridTop() + (i / cols) * zeilenAbstand() - scroll;
            if (cy + kartenH() < gridTop() || cy > py + ph) continue;
            drawCard(ctx, shown.get(i), cx, cy, cw, mouseX, mouseY);
        }
        ctx.disableScissor();

        int sx = sucheLinks();
        int[] zu = zuFeld();
        VStyle.roundRect(ctx, sx, zu[1], sucheB(), zu[3], VStyle.R_SMALL, 0x14FFFFFF);
        boolean zuHover = mouseX >= zu[0] && mouseX <= zu[0] + zu[2]
                && mouseY >= zu[1] && mouseY <= zu[1] + zu[3];
        VStyle.roundRect(ctx, zu[0], zu[1], zu[2], zu[3], VStyle.R_SMALL,
                zuHover ? 0x33FF5555 : 0x14FFFFFF);
        VFont.zeichneMittig(ctx, textRenderer, "\u2715", zu[0] + zu[2] / 2,
                zu[1] + (zu[3] - 8) / 2, zuHover ? 0xFFFF8888 : VStyle.TEXT_DIM);

        search.setX(sx + 8);
        search.setY(zu[1] + (zu[3] - 10) / 2);
        search.setWidth(sucheB() - 16);
        search.render(ctx, mouseX, mouseY, delta);
        if (search.getText().isEmpty()) {
            VFont.zeichne(ctx, textRenderer, VText.t("ui.search"), sx + 8,
                    zu[1] + (zu[3] - 8) / 2, VStyle.TEXT_FAINT);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void seitenleiste(DrawContext ctx, int mouseX, int mouseY) {
        int px = panelX();
        int lw = leisteW();

        VFont.zeichne(ctx, textRenderer, "VISUAL", px + 14,
                panelY() + Math.round(height * 0.042f), VStyle.TEXT);
        int w = VFont.breite(textRenderer, "VISUAL ");
        VFont.zeichne(ctx, textRenderer, "CLIENT", px + 14 + w,
                panelY() + Math.round(height * 0.042f), VStyle.ACCENT);

        List<Nav> nav = navEintraege();
        for (int i = 0; i < nav.size(); i++) {
            Nav n = nav.get(i);
            int ny = navY(i);
            if (n.gruppe() != null) {
                VFont.zeichne(ctx, textRenderer, VText.t(n.gruppe()), px + 16,
                        ny - Math.round(height * 0.022f), VStyle.TEXT_FAINT);
            }
            boolean aktiv = n.tun() == null;
            boolean hover = !aktiv && mouseX >= px + 8 && mouseX <= px + lw - 8
                    && mouseY >= ny && mouseY <= ny + navH();
            if (aktiv || hover) {
                VStyle.roundRect(ctx, px + 8, ny, lw - 16, navH(), VStyle.R_SMALL,
                        aktiv ? VStyle.ACCENT : 0x14FFFFFF);
            }
            int farbe = aktiv ? 0xFFFFFFFF : hover ? VStyle.TEXT : VStyle.TEXT_DIM;
            VFont.zeichne(ctx, textRenderer, n.symbol(), px + 16, ny + (navH() - 8) / 2, farbe);
            VFont.zeichne(ctx, textRenderer, VText.t(n.schluessel()), px + 30,
                    ny + (navH() - 8) / 2, farbe);
        }

        String name = MinecraftClient.getInstance().getSession().getUsername();
        int fy = panelY() + panelH() - Math.round(height * 0.055f);
        VStyle.roundRect(ctx, px + 14, fy, 14, 14, 4, VStyle.ACCENT_DIM);
        VFont.zeichneMittig(ctx, textRenderer,
                name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), px + 21, fy + 3,
                0xFFFFFFFF);
        VFont.zeichne(ctx, textRenderer, name, px + 34, fy + 1, VStyle.TEXT);
        VFont.zeichne(ctx, textRenderer,
                "Fabric " + MinecraftClient.getInstance().getGameVersion(),
                px + 34, fy + 10, VStyle.TEXT_FAINT);
    }

    /**
     * Karte im Aufbau der Vorlage: grosses Symbol mittig im dunklen Teil,
     * darunter ein Fussbalken mit dem Namen. Der Balken ist im Akzent, wenn
     * das Modul an ist, sonst grau - so sieht man den Zustand aus der
     * Entfernung, ohne die Beschriftung lesen zu muessen.
     */
    private void drawCard(DrawContext ctx, VModule m, int x, int y, int w,
                          int mouseX, int mouseY) {
        int kopf = kartenKopfH();
        int balken = balkenH();
        int h = kopf + balken;
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        boolean on = m.enabled != null && m.enabled.getAsBoolean();

        VStyle.roundRect(ctx, x, y, w, h, VStyle.R_SMALL, hover ? 0x26FFFFFF : 0x12FFFFFF);
        VStyle.roundOutline(ctx, x, y, w, h, VStyle.R_SMALL, hover ? 0x33FFFFFF : 0x14FFFFFF);

        skalieren(ctx, x + w / 2f, y + kopf / 2f - 8, 2.0f);
        try {
            VFont.zeichneMittig(ctx, textRenderer, m.icon, 0, 0,
                    on ? VStyle.TEXT : VStyle.TEXT_DIM);
        } finally {
            zurueckskalieren(ctx);
        }

        int by = y + kopf;
        // Aus heisst dunkler Balken mit heller Schrift, nicht heller Balken
        // mit blasser Schrift - so bleibt der Name in beiden Zustaenden
        // lesbar und der Unterschied trotzdem auf einen Blick sichtbar.
        int balkenFarbe = on ? VStyle.ACCENT : 0x66000000;
        VStyle.roundRect(ctx, x, by, w, balken, VStyle.R_XS, balkenFarbe);
        // Obere Ecken wieder eckig, sonst schwebt der Balken neben dem Kopf
        ctx.fill(x, by, x + w, by + VStyle.R_XS, balkenFarbe);
        VFont.zeichneMittig(ctx, textRenderer, trim(m.title, w - 10), x + w / 2,
                by + (balken - 8) / 2, on ? 0xFFFFFFFF : VStyle.TEXT);
    }

    private String trim(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (textRenderer.getWidth(sb.toString() + ch + "…") > maxWidth) break;
            sb.append(ch);
        }
        return sb + "…";
    }

    /**
     * Ausdrücklich der eigene Startbildschirm, wenn keine Welt läuft; im
     * Spiel dagegen zurück ins Geschehen.
     *
     * Wichtig: im Test schloss die Esc-Taste diesen Bildschirm nicht — in
     * einem Profil mit vielen Mods greift offenbar etwas anderes die Taste
     * ab, bevor Screen.keyPressed sie sieht. Deshalb gibt es das Kreuz oben
     * rechts; es ist der verlässliche Ausgang und darf nicht wegfallen.
     */
    @Override
    public void close() {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlurGuard.restore();
        if (mc.world == null) mc.setScreen(VisualTitleScreen.oeffnen());
        else mc.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        BlurGuard.restore();
        super.removed();
    }
}
