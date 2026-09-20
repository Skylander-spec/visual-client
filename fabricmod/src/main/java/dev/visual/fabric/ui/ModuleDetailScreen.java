package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Detailseite eines Moduls: Kopf mit Zurück, An/Aus und Zurücksetzen,
 * darunter die Einstellungszeilen.
 */
public class ModuleDetailScreen extends VMouseScreen {
    private static final int ROW_H = 26;
    /** Dieselbe Leiste wie in der Übersicht — eine Sprache für beide Seiten. */
    private static final int NAV_H = 48;
    private static final int PAD = 28;
    private static final int ZU = 24;

    private final Screen parent;
    private final VModule module;
    private int scroll = 0;

    public ModuleDetailScreen(Screen parent, VModule module) {
        super(Text.literal(module.title));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        BlurGuard.off();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    /** Das Kreuz verlässt das Menü ganz, nicht nur diese Seite. */
    private void verlassen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlurGuard.restore();
        if (mc.world == null) mc.setScreen(VisualTitleScreen.oeffnen());
        else mc.setScreen(null);
    }

    private int left() {
        return PAD;
    }

    private int rowsTop() {
        return NAV_H + 34;
    }

    private int rowWidth() {
        return width - 2 * PAD;
    }

    private int zuX() {
        return width - PAD - ZU;
    }

    /** Linke Kante des Hauptschalters, links neben dem Kreuz. */
    private int schalterX() {
        return zuX() - 12 - 40;
    }

    @Override
    protected boolean handleScroll(double amount) {
        int total = module.settings.size() * ROW_H;
        int max = Math.max(0, total - (height - rowsTop() - 20));
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 20)));
        return true;
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        // Obere Leiste: Zurück links, Schalter und Kreuz rechts
        if (mouseY < NAV_H) {
            if (mouseX >= zuX() && mouseX <= zuX() + ZU) {
                verlassen();
                return true;
            }
            if (module.setEnabled != null
                    && mouseX >= schalterX() && mouseX <= schalterX() + 40) {
                module.setEnabled.accept(!module.enabled.getAsBoolean());
                return true;
            }
            close();
            return true;
        }
        // Zeilen
        int x = left();
        int w = rowWidth();
        for (int i = 0; i < module.settings.size(); i++) {
            int y = rowsTop() + i * ROW_H - scroll;
            if (mouseY < y || mouseY > y + ROW_H - 4) continue;
            VSetting s = module.settings.get(i);
            if (s instanceof VSetting.Toggle t) {
                if (mouseX >= x + w - 30) {
                    t.set.accept(!t.get.getAsBoolean());
                    return true;
                }
            } else if (s instanceof VSetting.Stepper st) {
                if (mouseX >= x + w - 30 && mouseX <= x + w - 8) {
                    st.next.run();
                    return true;
                }
                if (mouseX >= x + w - 150 && mouseX <= x + w - 128) {
                    st.prev.run();
                    return true;
                }
            } else if (s instanceof VSetting.Action a) {
                if (mouseX >= x && mouseX <= x + w) {
                    a.run.run();
                    return true;
                }
            }
        }
        return false;
    }

    /** Wie in der Übersicht: eigener Hintergrund ohne Minecrafts Weichzeichner. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, VStyle.BG);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // Obere Leiste wie in der Übersicht
        ctx.fill(0, 0, width, NAV_H, VStyle.PANEL);
        ctx.fill(0, NAV_H, width, NAV_H + 1, VStyle.BORDER);
        ctx.drawText(textRenderer, "‹", PAD, NAV_H / 2 - 4, VStyle.TEXT_DIM, false);
        ctx.drawText(textRenderer, module.title, PAD + 14, NAV_H / 2 - 4, VStyle.TEXT, false);

        if (module.setEnabled != null) {
            boolean on = module.enabled.getAsBoolean();
            VStyle.toggle(ctx, schalterX(), NAV_H / 2 - 10, 40, 20, on);
        }
        int zx = zuX();
        int zy = (NAV_H - ZU) / 2;
        boolean zuHover = mouseX >= zx && mouseX <= zx + ZU && mouseY >= zy && mouseY <= zy + ZU;
        VStyle.roundRect(ctx, zx, zy, ZU, ZU, VStyle.R_SMALL,
                zuHover ? VStyle.GHOST_HOVER : VStyle.CARD);
        ctx.drawCenteredTextWithShadow(textRenderer, "✕", zx + ZU / 2, zy + (ZU - 8) / 2,
                zuHover ? VStyle.TEXT : VStyle.TEXT_DIM);

        ctx.drawText(textRenderer, module.description, PAD, NAV_H + 10, VStyle.TEXT_FAINT, false);

        if (module.settings.isEmpty()) {
            ctx.drawText(textRenderer, VText.t("ui.nosettings"),
                    left(), rowsTop(), VStyle.TEXT_FAINT, false);
        }

        int x = left();
        int w = rowWidth();
        for (int i = 0; i < module.settings.size(); i++) {
            int y = rowsTop() + i * ROW_H - scroll;
            if (y + ROW_H < rowsTop() || y > height) continue;
            drawRow(ctx, module.settings.get(i), x, y, w, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawRow(DrawContext ctx, VSetting s, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = mouseY >= y && mouseY <= y + ROW_H - 4 && mouseX >= x && mouseX <= x + w;
        if (hover) VStyle.roundRect(ctx, x, y, w, ROW_H - 4, VStyle.R_CARD, 0x14FFFFFF);
        ctx.drawText(textRenderer, s.label, x + 8, y + 6, VStyle.TEXT, false);

        if (s instanceof VSetting.Toggle t) {
            // Derselbe Pillenschalter wie im Kopf — ein Bedienelement, eine Form
            boolean on = t.get.getAsBoolean();
            int bx = x + w - 44;
            VStyle.roundRect(ctx, bx, y + 4, 36, 18, 9, on ? VStyle.ACCENT : 0x33FFFFFF);
            VStyle.roundRect(ctx, on ? bx + 19 : bx + 2, y + 6, 14, 14, 7, 0xFFFFFFFF);
        } else if (s instanceof VSetting.Stepper st) {
            // Zwischen den beiden Knöpfen muss auch ein langer Wert wie
            // „Linke Schulter" Platz haben — vorher lagen 58 Pixel dazwischen,
            // und der Text schob sich über das Minus.
            String value = textRenderer.trimToWidth(st.display.get(), 92);
            button(ctx, x + w - 150, y + 4, "−");
            button(ctx, x + w - 30, y + 4, "+");
            int vw = textRenderer.getWidth(value);
            ctx.drawText(textRenderer, value, x + w - 89 - vw / 2, y + 6, VStyle.TEXT, false);
        } else if (s instanceof VSetting.Info info) {
            String value = info.value.get();
            VStyle.roundRect(ctx, x + w - 90, y + 4, 82, 18, VStyle.R_CARD, 0x14FFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, value, x + w - 49, y + 9, VStyle.TEXT_DIM);
        } else if (s instanceof VSetting.Action a) {
            VStyle.roundRect(ctx, x, y, w, ROW_H - 4, VStyle.R_CARD,
                    hover ? 0x33FF453A : 0x1FFF453A);
            ctx.drawCenteredTextWithShadow(textRenderer, s.label, x + w / 2, y + 6,
                    a.tint.getAsInt());
        }
    }

    private void button(DrawContext ctx, int x, int y, String label) {
        VStyle.roundRect(ctx, x, y, 22, 18, VStyle.R_CARD, 0x1FFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + 11, y + 5, VStyle.TEXT);
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
