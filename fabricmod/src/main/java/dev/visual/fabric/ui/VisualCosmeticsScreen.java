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
        BlurGuard.off();
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

    /**
     * Breite der Rubrikenspalte. Bei kleiner Fensterbreite — etwa bei hoher
     * GUI-Skalierung oder Minecrafts Standardfenster — faellt sie ganz weg,
     * sonst bliebe fuer die Kacheln nichts uebrig.
     */
    private int railBreite() {
        return width < 520 ? 0 : RAIL_MAX;
    }

    /** Vorschauspalte, schrumpft mit und verschwindet erst ganz zum Schluss. */
    private int vorschauBreite() {
        if (width < 340) return 0;
        if (width >= 560) return VORSCHAU_MAX;
        return Math.max(VORSCHAU_MIN, width - 340 + VORSCHAU_MIN);
    }

    private int spalten() {
        int platz = width - railBreite() - vorschauBreite() - 3 * PAD;
        return Math.max(1, (platz + LUECKE) / (KACHEL_W + LUECKE));
    }

    private int rasterX() {
        return railBreite() + PAD;
    }

    private int rasterY() {
        return NAV_H + 20;
    }

    private int zuX() {
        return width - PAD - ZU;
    }

    private int vorschauX() {
        return width - PAD - vorschauBreite();
    }

    private int vorschauH() {
        return height - (NAV_H + 20) - PAD;
    }

    /** Oberkante des Anlegen-Knopfes — Zeichnen und Klick lesen beide hier. */
    private int knopfY() {
        return NAV_H + 20 + vorschauH() - 46;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, VStyle.BG);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // Kopfleiste wie in der Modul-Übersicht
        ctx.fill(0, 0, width, NAV_H, VStyle.PANEL);
        ctx.fill(0, NAV_H, width, NAV_H + 1, VStyle.BORDER);
        ctx.drawText(textRenderer, "VISUAL", PAD, NAV_H / 2 - 9, VStyle.TEXT, false);
        ctx.drawText(textRenderer, "CLIENT", PAD, NAV_H / 2 + 1, VStyle.ACCENT, false);
        ctx.drawText(textRenderer, VText.t("ui.wardrobe"), PAD + 70, NAV_H / 2 - 4,
                VStyle.TEXT, false);

        int zx = zuX();
        int zy = (NAV_H - ZU) / 2;
        boolean zuHover = mouseX >= zx && mouseX <= zx + ZU && mouseY >= zy && mouseY <= zy + ZU;
        VStyle.roundRect(ctx, zx, zy, ZU, ZU, VStyle.R_SMALL,
                zuHover ? VStyle.GHOST_HOVER : VStyle.CARD);
        ctx.drawCenteredTextWithShadow(textRenderer, "✕", zx + ZU / 2, zy + (ZU - 8) / 2,
                zuHover ? VStyle.TEXT : VStyle.TEXT_DIM);

        // Linke Rubrikenspalte. Bisher gibt es nur Umhänge, aber die Spalte
        // hält den Platz für das, was noch dazukommt.
        int rail = railBreite();
        if (rail > 0) {
            ctx.fill(0, NAV_H + 1, rail, height, VStyle.PANEL);
            ctx.fill(rail, NAV_H + 1, rail + 1, height, VStyle.BORDER);
            VStyle.roundRect(ctx, 8, NAV_H + 12, rail - 16, 24, VStyle.R_SMALL,
                    VStyle.ACCENT_BG);
            ctx.drawText(textRenderer, VText.t("ui.capes"), 18, NAV_H + 20,
                    VStyle.TEXT, false);
        }

        zeichneRaster(ctx, mouseX, mouseY);
        zeichneVorschau(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void zeichneRaster(DrawContext ctx, int mouseX, int mouseY) {
        int cols = spalten();
        int x0 = rasterX();
        int y0 = rasterY();
        for (int i = 0; i < umhaenge.size(); i++) {
            int cx = x0 + (i % cols) * (KACHEL_W + LUECKE);
            int cy = y0 + (i / cols) * (KACHEL_H + LUECKE) - scroll;
            if (cy + KACHEL_H < y0 || cy > height) continue;

            boolean hover = mouseX >= cx && mouseX <= cx + KACHEL_W
                    && mouseY >= cy && mouseY <= cy + KACHEL_H;
            boolean aktiv = i == gewaehlt;
            VStyle.roundOutline(ctx, cx, cy, KACHEL_W, KACHEL_H, VStyle.R_CARD,
                    aktiv ? VStyle.ACCENT : hover ? VStyle.BORDER_HI : VStyle.BORDER);
            VStyle.roundRect(ctx, cx, cy, KACHEL_W, KACHEL_H, VStyle.R_CARD,
                    hover || aktiv ? VStyle.CARD_HOVER : VStyle.CARD);

            Umhang u = umhaenge.get(i);
            zeichneTraeger(ctx, u, cx + 8, cy + 8, KACHEL_W - 16, KACHEL_H - 34, 34);

            String name = u.name() == null ? VText.t("ui.nocape") : u.name();
            name = textRenderer.trimToWidth(name, KACHEL_W - 12);
            ctx.drawCenteredTextWithShadow(textRenderer, name, cx + KACHEL_W / 2,
                    cy + KACHEL_H - 18, aktiv ? VStyle.ACCENT : VStyle.TEXT);
        }
    }

    private void zeichneVorschau(DrawContext ctx, int mouseX, int mouseY) {
        if (vorschauBreite() <= 0) return;
        int px = vorschauX();
        int py = NAV_H + 20;
        int ph = vorschauH();
        VStyle.roundOutline(ctx, px, py, vorschauBreite(), ph, VStyle.R_PANEL, VStyle.BORDER);
        VStyle.roundRect(ctx, px, py, vorschauBreite(), ph, VStyle.R_PANEL, VStyle.PANEL);

        Umhang u = umhaenge.get(Math.min(gewaehlt, umhaenge.size() - 1));
        zeichneTraeger(ctx, u, px + 20, py + 16, vorschauBreite() - 40, ph - 110, 62);

        int ty = py + ph - 86;
        String name = u.name() == null ? VText.t("ui.nocape") : u.name();
        ctx.drawText(textRenderer, textRenderer.trimToWidth(name, vorschauBreite() - 32),
                px + 16, ty, VStyle.TEXT, false);
        boolean istAn = u.name() == null ? angelegt == null : u.name().equals(angelegt);

        int bx = px + 16;
        int by = knopfY();
        int bw = vorschauBreite() - 32;
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + 26;
        VStyle.roundRect(ctx, bx, by, bw, 26, VStyle.R_SMALL,
                istAn ? VStyle.GHOST_HOVER : hover ? VStyle.ACCENT_HOVER : VStyle.ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer,
                VText.t(istAn ? "ui.worn" : "ui.wear"), bx + bw / 2, by + 9,
                istAn ? VStyle.TEXT_DIM : 0xFFFFFFFF);
    }

    /**
     * Den eigenen Spieler mit diesem Umhang in ein Feld zeichnen. Ohne Welt
     * gibt es keinen — dann die Textur selbst, damit die Kachel etwas zeigt.
     */
    private void zeichneTraeger(DrawContext ctx, Umhang u, int x, int y, int w, int h,
                                int groesse) {
        LivingEntity spieler = client == null ? null : client.player;
        if (spieler == null) {
            if (u.textur() != null) {
                // Cape-Bilder sind doppelt so breit wie hoch
                int bh = Math.min(h, w / 2);
                Compat.drawTex(ctx, u.textur(), x + (w - bh * 2) / 2, y + (h - bh) / 2,
                        bh * 2, bh, 64, 32, 0xFFFFFFFF);
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer, "—", x + w / 2, y + h / 2 - 4,
                        VStyle.TEXT_FAINT);
            }
            return;
        }
        try {
            CapeManager.vorschauSetzen(u.textur());
            // Der Blickpunkt liegt weit über dem Feld: dadurch neigt sich die
            // Figur nach vorn und der Umhang wird sichtbar, statt dass man nur
            // auf die Vorderseite schaut.
            InventoryScreen.drawEntity(ctx, x, y, x + w, y + h, groesse, 0.0f,
                    x + w / 2f, y - 40f, spieler);
        } catch (Throwable t) {
            // Ein Fehler hier darf nicht den ganzen Bildschirm reißen
        } finally {
            CapeManager.vorschauLoeschen();
        }
    }

    @Override
    protected boolean handleScroll(double amount) {
        int cols = spalten();
        int reihen = (umhaenge.size() + cols - 1) / cols;
        int max = Math.max(0, reihen * (KACHEL_H + LUECKE) - (height - rasterY() - 16));
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 24)));
        return true;
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (mouseY < NAV_H) {
            if (mouseX >= zuX() && mouseX <= zuX() + ZU) {
                verlassen();
                return true;
            }
            return false;
        }

        int px = vorschauX();
        int by = knopfY();
        if (mouseX >= px + 16 && mouseX <= px + vorschauBreite() - 16
                && mouseY >= by && mouseY <= by + 26) {
            Umhang u = umhaenge.get(Math.min(gewaehlt, umhaenge.size() - 1));
            CapeManager.anlegen(u.name());
            angelegt = u.name();
            return true;
        }

        int cols = spalten();
        int x0 = rasterX();
        int y0 = rasterY();
        for (int i = 0; i < umhaenge.size(); i++) {
            int cx = x0 + (i % cols) * (KACHEL_W + LUECKE);
            int cy = y0 + (i / cols) * (KACHEL_H + LUECKE) - scroll;
            if (mouseX >= cx && mouseX <= cx + KACHEL_W
                    && mouseY >= cy && mouseY <= cy + KACHEL_H) {
                gewaehlt = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        verlassen();
    }

    private void verlassen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlurGuard.restore();
        mc.setScreen(new VisualHomeScreen(4));
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
