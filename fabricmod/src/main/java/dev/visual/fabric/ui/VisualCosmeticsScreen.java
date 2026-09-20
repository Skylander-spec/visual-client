package dev.visual.fabric.ui;

import dev.visual.fabric.CapeManager;
import dev.visual.fabric.Compat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Kleiderschrank: jeder Umhang wird am eigenen Skin gezeigt, nicht als
 * flache Textur.
 *
 * Gezeichnet wird dafür der eigene Spieler mehrfach — einmal je Kachel —
 * und vor jedem Durchlauf sagt {@link CapeManager#vorschauSetzen} dem
 * Renderer, welcher Umhang gerade gelten soll. Das spart jede Vorbereitung:
 * Skin, Rüstung und Modellvariante stimmen von allein, weil es derselbe
 * Renderer ist wie im Spiel.
 *
 * Ohne Welt gibt es keinen Spieler zum Zeichnen. Dann zeigt die Kachel die
 * Textur selbst — besser als ein leeres Feld.
 */
public class VisualCosmeticsScreen extends VMouseScreen {

    /**
     * Den Cosmetics-Bildschirm oeffnen - moeglichst den, den OneConfigs
     * Renderer zeichnet.
     *
     * Ueber Class.forName und nicht per new: VCosmeticsScreen erbt eine
     * Klasse aus OneConfig. Fehlt OneConfig im Profil, wirft schon das
     * Aufloesen der Klasse einen NoClassDefFoundError - bei einem
     * direkten new kann das noch beim Pruefen dieser Methode passieren,
     * also bevor irgendein Fang greift. Der Umweg holt den Fehler
     * garantiert in den try-Block.
     */
    public static net.minecraft.client.gui.screen.Screen oeffnen() {
        try {
            return (net.minecraft.client.gui.screen.Screen)
                    Class.forName("dev.visual.fabric.ui.VCosmeticsScreen")
                            .getDeclaredConstructor()
                            .newInstance();
        } catch (Throwable fehler) {
            return new VisualCosmeticsScreen();
        }
    }

    private static final int NAV_H = 48;
    private static final int PAD = 28;
    private static final int ZU = 24;
    /** Linke Spalte mit den Rubriken — bei engem Fenster fällt sie weg. */
    private static final int RAIL_MAX = 96;
    /** Rechte Spalte mit der großen Vorschau. */
    private static final int VORSCHAU_MAX = 210;
    private static final int VORSCHAU_MIN = 132;
    private static final int KACHEL_W = 104;
    private static final int KACHEL_H = 150;
    private static final int LUECKE = 10;

    /** Ein Umhang aus dem Ordner des Launchers. Name null = keiner. */
    private record Umhang(String name, Identifier textur) {
    }

    private final List<Umhang> umhaenge = new ArrayList<>();
    private String angelegt;
    private int gewaehlt = 0;
    private int scroll = 0;

    public VisualCosmeticsScreen() {
        super(Text.literal("Visual Client"));
    }

    @Override
    protected void init() {
        super.init();
        umhaenge.clear();
        // Erster Eintrag ist immer der leere — sonst käme man nie wieder
        // zurück auf blank.
        umhaenge.add(new Umhang(null, null));
        angelegt = CapeManager.angelegt();

        Path d = CapeManager.datenOrdner();
        if (d != null) {
            try (var liste = Files.list(d.resolve("capes"))) {
                liste.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                        .sorted()
                        .forEach(p -> {
                            String voll = p.getFileName().toString();
                            String name = voll.substring(0, voll.length() - 4);
                            Identifier tex = laden(p, name);
                            if (tex != null) umhaenge.add(new Umhang(name, tex));
                        });
            } catch (Throwable t) {
                // Kein Ordner, keine Umhänge — bleibt beim leeren Eintrag
            }
        }
        for (int i = 0; i < umhaenge.size(); i++) {
            if (angelegt != null && angelegt.equals(umhaenge.get(i).name())) gewaehlt = i;
        }
    }

    /** Eine Cape-Datei als Textur registrieren; Kennung je Name stabil. */
    private Identifier laden(Path datei, String name) {
        Identifier id = Identifier.of("visualsfabric",
                "cape_vorschau/" + Integer.toHexString(name.hashCode()));
        try (InputStream in = Files.newInputStream(datei)) {
            Compat.registerTexture(client, id, NativeImage.read(in));
            return id;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected void setInitialFocus() {
    }

    // -- Aufbau ---------------------------------------------------------
    //
    // Ein Ort fuer alles: links der Spiegel mit dem eigenen Avatar, rechts
    // die Rubriken. Frueher lagen die Umhaenge hier und der Mini-Me in einer
    // eigenen Modulseite - wer sich anziehen wollte, musste zwischen zwei
    // Bildschirmen springen.

    private static final String[] RUBRIKEN = {"ui.capes", "mod.minime"};
    private int rubrik = 0;

    /** Breite des Spiegels innerhalb des Inhaltsbereichs. */
    private int spiegelB() {
        return Math.round(VPanel.inhaltW(width) * 0.34f);
    }

    private int rechtsX() {
        return VPanel.inhaltX(width) + spiegelB() + Math.round(width * 0.02f);
    }

    private int rechtsB() {
        return VPanel.inhaltX(width) + VPanel.inhaltW(width) - rechtsX();
    }

    private int pilleX(int i) {
        int x = rechtsX();
        for (int k = 0; k < i; k++) x += pilleB(k) + 6;
        return x;
    }

    private int pilleB(int i) {
        return VFont.breite(textRenderer, VText.t(RUBRIKEN[i])) + 18;
    }

    private int pilleY() {
        return VPanel.y(height) + Math.round(height * 0.105f);
    }

    private int pilleH() {
        return Math.round(height * 0.0408f);
    }

    private int rasterOben() {
        return VPanel.y(height) + Math.round(height * 0.148f);
    }

    private int spalten() {
        return 3;
    }

    private int kachelAbstand() {
        return Math.max(4, Math.round(width * 0.0125f));
    }

    private int kachelB() {
        int a = kachelAbstand();
        return (rechtsB() - (spalten() - 1) * a) / spalten();
    }

    private int kachelH() {
        return Math.round(height * 0.20f);
    }

    private int zeilenAbstand() {
        return kachelH() + Math.max(4, Math.round(height * 0.024f));
    }

    /** Die Einstellungen des Mini-Me, aus der Modulliste geholt. */
    private List<VSetting> miniEinstellungen() {
        for (VModule m : ModuleRegistry.all(this)) {
            if (m.id.equals("minime")) return m.settings;
        }
        return List.of();
    }

    private int zeileY(int i) {
        return rasterOben() + i * Math.round(height * 0.052f);
    }

    private int zeileH() {
        return Math.round(height * 0.044f);
    }

    @Override
    protected boolean handleScroll(double amount) {
        if (rubrik != 0) return false;
        int reihen = (umhaenge.size() + spalten() - 1) / spalten();
        int gesamt = reihen * zeilenAbstand();
        int sichtbar = VPanel.y(height) + VPanel.h(height) - rasterOben() - 8;
        scroll = Math.max(0, Math.min(Math.max(0, gesamt - sichtbar),
                scroll - (int) (amount * 24)));
        return true;
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int[] zu = VPanel.zuFeld(width, height);
        if (mouseX >= zu[0] && mouseX <= zu[0] + zu[2]
                && mouseY >= zu[1] && mouseY <= zu[1] + zu[3]) {
            close();
            return true;
        }

        if (VPanel.navGeklickt(this, mouseX, mouseY, width, height)) return true;

        for (int i = 0; i < RUBRIKEN.length; i++) {
            int bx = pilleX(i);
            if (mouseX >= bx && mouseX <= bx + pilleB(i)
                    && mouseY >= pilleY() && mouseY <= pilleY() + pilleH()) {
                rubrik = i;
                scroll = 0;
                return true;
            }
        }

        if (rubrik == 0) {
            int cols = spalten();
            int cw = kachelB();
            for (int i = 0; i < umhaenge.size(); i++) {
                int cx = rechtsX() + (i % cols) * (cw + kachelAbstand());
                int cy = rasterOben() + (i / cols) * zeilenAbstand() - scroll;
                if (mouseX >= cx && mouseX <= cx + cw
                        && mouseY >= cy && mouseY <= cy + kachelH()) {
                    gewaehlt = i;
                    anziehen(umhaenge.get(i));
                    return true;
                }
            }
            return false;
        }

        List<VSetting> liste = miniEinstellungen();
        for (int i = 0; i < liste.size(); i++) {
            int zy = zeileY(i);
            if (mouseY < zy || mouseY > zy + zeileH()) continue;
            if (mouseX < rechtsX() || mouseX > rechtsX() + rechtsB()) continue;
            VSetting s = liste.get(i);
            int rechts = rechtsX() + rechtsB();
            if (s instanceof VSetting.Toggle t) {
                t.set.accept(!t.get.getAsBoolean());
            } else if (s instanceof VSetting.Stepper st) {
                if (mouseX >= rechts - 24) st.next.run();
                else if (mouseX >= rechts - 120 && mouseX <= rechts - 96) st.prev.run();
            } else if (s instanceof VSetting.Action a) {
                a.run.run();
            }
            return true;
        }
        return false;
    }

    /** Einmal fehlgeschlagen heisst: nicht jeden Frame erneut versuchen. */
    private boolean panoramaAus = false;

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
        VPanel.rahmen(ctx, width, height);
        VPanel.seitenleiste(this, ctx, textRenderer, mouseX, mouseY, width, height,
                "ui.cosmetics");

        VFont.zeichne(ctx, textRenderer, VText.t("ui.wardrobe"), VPanel.inhaltX(width),
                VPanel.y(height) + Math.round(height * 0.042f), VStyle.TEXT);

        spiegel(ctx);

        for (int i = 0; i < RUBRIKEN.length; i++) {
            int bx = pilleX(i);
            int bw = pilleB(i);
            boolean aktiv = i == rubrik;
            boolean hover = mouseX >= bx && mouseX <= bx + bw
                    && mouseY >= pilleY() && mouseY <= pilleY() + pilleH();
            VStyle.roundRect(ctx, bx, pilleY(), bw, pilleH(), pilleH() / 2,
                    aktiv ? VStyle.ACCENT : hover ? 0x1AFFFFFF : 0x0CFFFFFF);
            if (!aktiv) {
                VStyle.roundOutline(ctx, bx, pilleY(), bw, pilleH(), pilleH() / 2, 0x1AFFFFFF);
            }
            VFont.zeichneMittig(ctx, textRenderer, VText.t(RUBRIKEN[i]), bx + bw / 2,
                    pilleY() + (pilleH() - 8) / 2, aktiv ? 0xFFFFFFFF : VStyle.TEXT_DIM);
        }

        if (rubrik == 0) capeRaster(ctx, mouseX, mouseY);
        else miniZeilen(ctx, mouseX, mouseY);

        VPanel.schliessen(ctx, textRenderer, mouseX, mouseY, width, height);
        super.render(ctx, mouseX, mouseY, delta);
    }

    /**
     * Der Spiegel. Zeigt den Avatar so, wie er in diesem Moment aussieht -
     * mit angelegtem Umhang und mit dem Mini-Me auf seinem Sitzplatz.
     */
    private void spiegel(DrawContext ctx) {
        int x = VPanel.inhaltX(width);
        int y = pilleY();
        int b = spiegelB();
        int h = VPanel.y(height) + VPanel.h(height) - y - Math.round(height * 0.03f);

        VStyle.roundRect(ctx, x, y, b, h, VStyle.R_SMALL, 0x14FFFFFF);
        VStyle.roundOutline(ctx, x, y, b, h, VStyle.R_SMALL, 0x14FFFFFF);

        VAvatar.zeichne(ctx, x + b / 2, y + Math.round(h * 0.16f), Math.round(h * 0.62f));

        String name = MinecraftClient.getInstance().getSession().getUsername();
        VFont.zeichneMittig(ctx, textRenderer, name, x + b / 2,
                y + h - Math.round(height * 0.055f), VStyle.TEXT);
        String getragen = angelegt == null ? VText.t("ui.nocape") : angelegt;
        VFont.zeichneMittig(ctx, textRenderer, getragen, x + b / 2,
                y + h - Math.round(height * 0.030f), VStyle.TEXT_FAINT);
    }

    private void capeRaster(DrawContext ctx, int mouseX, int mouseY) {
        int py = VPanel.y(height);
        ctx.enableScissor(rechtsX(), rasterOben(), rechtsX() + rechtsB(),
                py + VPanel.h(height) - 6);
        int cols = spalten();
        int cw = kachelB();
        int ch = kachelH();
        for (int i = 0; i < umhaenge.size(); i++) {
            int cx = rechtsX() + (i % cols) * (cw + kachelAbstand());
            int cy = rasterOben() + (i / cols) * zeilenAbstand() - scroll;
            if (cy + ch < rasterOben() || cy > py + VPanel.h(height)) continue;
            Umhang u = umhaenge.get(i);
            boolean hover = mouseX >= cx && mouseX <= cx + cw
                    && mouseY >= cy && mouseY <= cy + ch;
            boolean an = i == gewaehlt;

            VStyle.roundRect(ctx, cx, cy, cw, ch, VStyle.R_SMALL,
                    hover ? 0x26FFFFFF : 0x12FFFFFF);
            VStyle.roundOutline(ctx, cx, cy, cw, ch, VStyle.R_SMALL,
                    an ? VStyle.ACCENT : hover ? 0x33FFFFFF : 0x14FFFFFF);

            int balken = Math.round(height * 0.0417f);
            int kopf = ch - balken;
            zeichneTraeger(ctx, u, cx + 6, cy + 6, cw - 12, kopf - 12);

            int by = cy + kopf;
            int farbe = an ? VStyle.ACCENT : 0x66000000;
            VStyle.roundRect(ctx, cx, by, cw, balken, VStyle.R_XS, farbe);
            ctx.fill(cx, by, cx + cw, by + VStyle.R_XS, farbe);
            String name = u.name() == null ? VText.t("ui.nocape") : u.name();
            VFont.zeichneMittig(ctx, textRenderer, trimmen(name, cw - 10), cx + cw / 2,
                    by + (balken - 8) / 2, an ? 0xFFFFFFFF : VStyle.TEXT);
        }
        ctx.disableScissor();
    }

    private void miniZeilen(DrawContext ctx, int mouseX, int mouseY) {
        List<VSetting> liste = miniEinstellungen();
        int rechts = rechtsX() + rechtsB();
        for (int i = 0; i < liste.size(); i++) {
            VSetting s = liste.get(i);
            int zy = zeileY(i);
            int zh = zeileH();
            boolean hover = mouseX >= rechtsX() && mouseX <= rechts
                    && mouseY >= zy && mouseY <= zy + zh;
            VStyle.roundRect(ctx, rechtsX(), zy, rechtsB(), zh, VStyle.R_SMALL,
                    hover ? 0x1AFFFFFF : 0x0CFFFFFF);
            VFont.zeichne(ctx, textRenderer, s.label, rechtsX() + 10,
                    zy + (zh - 8) / 2, VStyle.TEXT);

            if (s instanceof VSetting.Toggle t) {
                boolean an = t.get.getAsBoolean();
                int bw = 26;
                int bh = 13;
                int bx = rechts - bw - 10;
                int by = zy + (zh - bh) / 2;
                VStyle.roundRect(ctx, bx, by, bw, bh, bh / 2, an ? VStyle.ACCENT : 0x26FFFFFF);
                VStyle.roundRect(ctx, an ? bx + bw - bh + 1 : bx + 1, by + 1,
                        bh - 2, bh - 2, (bh - 2) / 2, 0xFFFFFFFF);
            } else if (s instanceof VSetting.Stepper st) {
                zeichneStufe(ctx, rechts - 24, zy + (zh - 16) / 2, "+");
                zeichneStufe(ctx, rechts - 120, zy + (zh - 16) / 2, "\u2212");
                VFont.zeichneMittig(ctx, textRenderer, st.display.get(), rechts - 60,
                        zy + (zh - 8) / 2, VStyle.TEXT_DIM);
            } else if (s instanceof VSetting.Info in) {
                String wert = in.value.get();
                VFont.zeichne(ctx, textRenderer, wert,
                        rechts - 10 - VFont.breite(textRenderer, wert),
                        zy + (zh - 8) / 2, VStyle.TEXT_FAINT);
            }
        }
    }

    private void zeichneStufe(DrawContext ctx, int x, int y, String zeichen) {
        VStyle.roundRect(ctx, x, y, 16, 16, VStyle.R_XS, 0x1AFFFFFF);
        VFont.zeichneMittig(ctx, textRenderer, zeichen, x + 8, y + 4, VStyle.TEXT);
    }

    private String trimmen(String text, int maxBreite) {
        if (VFont.breite(textRenderer, text) <= maxBreite) return text;
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (VFont.breite(textRenderer, sb.toString() + ch + "\u2026") > maxBreite) break;
            sb.append(ch);
        }
        return sb + "\u2026";
    }

    /**
     * Die Vorderseite des Umhangs in die Kachel zeichnen. Sie liegt im
     * 64x32-Blatt bei (1,1) und ist 10x16 gross - wer das ganze Blatt malt,
     * bekommt ein winziges Bild mit Rueckseite und Raendern.
     */
    private void zeichneTraeger(DrawContext ctx, Umhang u, int x, int y, int w, int h) {
        if (u.textur() == null) {
            VFont.zeichneMittig(ctx, textRenderer, "\u2014", x + w / 2, y + h / 2 - 4,
                    VStyle.TEXT_FAINT);
            return;
        }
        int bh = Math.min(h, w * 16 / 10);
        int bw = bh * 10 / 16;
        Compat.drawTexAusschnitt(ctx, u.textur(), x + (w - bw) / 2, y + (h - bh) / 2,
                bw, bh, 1f, 1f, 10, 16, 64, 32, 0xFFFFFFFF);
    }

    private void anziehen(Umhang u) {
        try {
            CapeManager.anlegen(u.name());
            angelegt = u.name();
        } catch (Throwable t) {
            // Fehlschlag darf den Bildschirm nicht reissen
        }
    }

    @Override
    public void close() {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlurGuard.restore();
        if (mc.world == null) mc.setScreen(new VisualTitleScreen());
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
