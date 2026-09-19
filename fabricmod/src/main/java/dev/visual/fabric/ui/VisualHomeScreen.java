package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Modul-Übersicht im Stil von OneClient: waagerechte Leiste oben mit
 * Wortmarke und Reitern, darunter ein Raster aus Kacheln.
 *
 * Die Maße stammen aus OneClients Gestaltung: Leistenhöhe und Innenabstand
 * im selben Verhältnis, und der aktive Reiter bekommt eine Unterstreichung,
 * die beim Überfahren schon halb erscheint (dort 27 bzw. 18 Pixel breit).
 */
public class VisualHomeScreen extends VMouseScreen {
    /** Höhe der oberen Leiste (OneClient: 80 px bei Desktop-Maßstab). */
    private static final int NAV_H = 48;
    /** Seitlicher Innenabstand (OneClient: HOME_PADDING_PX = 48). */
    private static final int PAD = 28;
    private static final int UNDERLINE_ACTIVE = 27;
    private static final int UNDERLINE_HOVER = 18;
    private static final int CARD_H = 54;
    /** Kantenlänge des Schließen-Knopfes rechts in der Leiste. */
    private static final int ZU = 24;
    private static final int GAP = 10;

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
        BlurGuard.off();
        modules = ModuleRegistry.all(this);
        search = new TextFieldWidget(textRenderer, sucheX() + 8, (NAV_H - 22) / 2, 182, 22,
                Text.literal(VText.t("ui.search")));
        search.setPlaceholder(Text.literal("§8" + VText.t("ui.search")));
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
            case 4 -> id.equals("minime") || id.equals("fakeplayer")
                    || id.equals("titlescreen") || id.equals("menukey")
                    || id.equals("mods") || id.equals("packs");
            default -> true;
        };
    }

    /** Waagerechte Lage und Breite eines Reiters. */
    private int reiterX(int i) {
        int x = PAD + 110;
        for (int k = 0; k < i; k++) x += reiterBreite(k) + 22;
        return x;
    }

    private int reiterBreite(int i) {
        return textRenderer.getWidth(VText.t(REITER[i]));
    }

    /** Linke Kante der Suchfläche — der Schließen-Knopf sitzt rechts daneben. */
    private int sucheX() {
        return width - PAD - ZU - 8 - 198;
    }

    private int zuX() {
        return width - PAD - ZU;
    }

    private int columns() {
        return Math.max(1, (width - 2 * PAD) / 260);
    }

    private int cardW() {
        int cols = columns();
        return (width - 2 * PAD - (cols - 1) * GAP) / cols;
    }

    private int gridTop() {
        return NAV_H + 18;
    }

    private int maxScroll() {
        int rows = (shown.size() + columns() - 1) / columns();
        int total = rows * (CARD_H + GAP);
        return Math.max(0, total - (height - gridTop() - 16));
    }

    @Override
    protected boolean handleScroll(double amount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (amount * 24)));
        return true;
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        // Reiter in der oberen Leiste
        if (mouseY < NAV_H) {
            int zx = zuX();
            int zy = (NAV_H - ZU) / 2;
            if (mouseX >= zx && mouseX <= zx + ZU && mouseY >= zy && mouseY <= zy + ZU) {
                close();
                return true;
            }
            for (int i = 0; i < REITER.length; i++) {
                int rx = reiterX(i);
                int rw = reiterBreite(i);
                if (mouseX >= rx - 6 && mouseX <= rx + rw + 6) {
                    reiter = i;
                    scroll = 0;
                    filter();
                    return true;
                }
            }
            return false;
        }
        if (mouseY < gridTop()) return false;
        int cols = columns();
        int cw = cardW();
        for (int i = 0; i < shown.size(); i++) {
            int cx = PAD + (i % cols) * (cw + GAP);
            int cy = gridTop() + (i / cols) * (CARD_H + GAP) - scroll;
            if (mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + CARD_H) {
                VModule m = shown.get(i);
                if (m.action != null) {
                    m.action.run();
                } else if (m.settings.isEmpty() && m.setEnabled != null) {
                    // Kachel ohne Unterpunkte schaltet direkt um
                    m.setEnabled.accept(!m.enabled.getAsBoolean());
                } else {
                    MinecraftClient.getInstance().setScreen(new ModuleDetailScreen(this, m));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Eigener Hintergrund statt {@code super.renderBackground} — das würde seit
     * 1.20.5 den Weichzeichner über das Spiel legen.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, VStyle.BG);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // Obere Leiste
        ctx.fill(0, 0, width, NAV_H, VStyle.PANEL);
        ctx.fill(0, NAV_H, width, NAV_H + 1, VStyle.BORDER);

        // Wortmarke links
        ctx.drawText(textRenderer, "VISUAL", PAD, NAV_H / 2 - 9, VStyle.TEXT, false);
        ctx.drawText(textRenderer, "CLIENT", PAD, NAV_H / 2 + 1, VStyle.ACCENT, false);

        // Reiter mit Unterstreichung: aktiv breit, beim Überfahren halb
        int beschriftungY = NAV_H / 2 - 4;
        for (int i = 0; i < REITER.length; i++) {
            int rx = reiterX(i);
            int rw = reiterBreite(i);
            boolean aktiv = i == reiter;
            boolean hover = mouseY < NAV_H && mouseX >= rx - 6 && mouseX <= rx + rw + 6;
            ctx.drawText(textRenderer, VText.t(REITER[i]), rx, beschriftungY,
                    aktiv ? VStyle.TEXT : VStyle.TEXT_DIM, false);
            int ul = aktiv ? UNDERLINE_ACTIVE : hover ? UNDERLINE_HOVER : 0;
            if (ul > 0) {
                int ux = rx + (rw - ul) / 2;
                VStyle.roundRect(ctx, ux, beschriftungY + 13, ul, 2, 1,
                        aktiv ? VStyle.ACCENT : VStyle.TEXT_DIM);
            }
        }

        // Suchfeld-Fläche rechts, daneben das Kreuz zum Verlassen
        VStyle.roundRect(ctx, sucheX(), (NAV_H - 26) / 2, 198, 26,
                VStyle.R_CARD, VStyle.CARD);
        int zx = zuX();
        int zy = (NAV_H - ZU) / 2;
        boolean zuHover = mouseX >= zx && mouseX <= zx + ZU && mouseY >= zy && mouseY <= zy + ZU;
        VStyle.roundRect(ctx, zx, zy, ZU, ZU, VStyle.R_SMALL,
                zuHover ? VStyle.GHOST_HOVER : VStyle.CARD);
        ctx.drawCenteredTextWithShadow(textRenderer, "✕", zx + ZU / 2, zy + (ZU - 8) / 2,
                zuHover ? VStyle.TEXT : VStyle.TEXT_DIM);

        // Zähler unter der Leiste
        ctx.drawText(textRenderer, VText.t("ui.count", shown.size(), modules.size()),
                PAD, NAV_H + 6, VStyle.TEXT_FAINT, false);

        // Kacheln
        int cols = columns();
        int cw = cardW();
        for (int i = 0; i < shown.size(); i++) {
            int cx = PAD + (i % cols) * (cw + GAP);
            int cy = gridTop() + (i / cols) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < gridTop() || cy > height) continue;
            drawCard(ctx, shown.get(i), cx, cy, cw, mouseX, mouseY);
        }

        // Suchfeld über allem
        search.render(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCard(DrawContext ctx, VModule m, int x, int y, int w,
                          int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + CARD_H;
        boolean on = m.enabled != null && m.enabled.getAsBoolean();
        VStyle.card(ctx, x, y, w, CARD_H, hover);
        // Aktiv wird durch das Symbolfeld angezeigt, nicht durch einen
        // leuchtenden Rahmen — ruhiger und leichter zu lesen.

        // Symbolfeld: gefüllt im Akzent, wenn das Modul an ist
        VStyle.roundRect(ctx, x + 8, y + 11, 32, 32, VStyle.R_CARD,
                on ? VStyle.ACCENT : 0x14FFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, m.icon, x + 24, y + 22,
                on ? 0xFF0B1416 : VStyle.TEXT_DIM);

        ctx.drawText(textRenderer, m.title, x + 48, y + 12, VStyle.TEXT, false);
        String desc = trim(m.description, w - 62);
        ctx.drawText(textRenderer, desc, x + 48, y + 26, VStyle.TEXT_FAINT, false);

        String state = m.enabled == null
                ? VText.t(m.action != null ? "ui.open" : "ui.configure")
                : VText.t(on ? "ui.on" : "ui.offstate");
        int sw = textRenderer.getWidth(state);
        ctx.drawText(textRenderer, state, x + w - sw - 8, y + CARD_H - 14,
                on ? VStyle.ACCENT : VStyle.TEXT_FAINT, false);
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
