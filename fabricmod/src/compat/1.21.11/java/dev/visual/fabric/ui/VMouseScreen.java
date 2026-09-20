package dev.visual.fabric.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Screen-Basis mit versionsunabhängigen Maus-Rückrufen. Ab 1.21.9 kommen
 * Maus-Ereignisse als Click-Objekt; Unterklassen sehen nur handle*.
 */
public abstract class VMouseScreen extends Screen {
    protected VMouseScreen(Text title) {
        super(title);
    }

    /**
     * Vanillas eigener Weichzeichner ueber dem Panorama. Bis 1.21.5 ohne
     * Argument, ab 1.21.6 mit DrawContext - deshalb steht er hier und nicht
     * im gemeinsamen Teil.
     */
    protected void weichzeichnen(DrawContext ctx) {
        applyBlur(ctx);
    }

    /**
     * Groesser zeichnen: alles zwischen skalieren und zurueck wird um
     * {@code faktor} vergroessert, Ursprung ist (x, y). Der Matrizen-Typ
     * wechselte mit 1.21.6, deshalb steht das hier.
     */
    protected void skalieren(DrawContext ctx, float x, float y, float faktor) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(faktor, faktor);
    }

    protected void zurueckskalieren(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    /** Feste Arbeitsflaeche statt Vanilla-GUI-Skalierung. Der HUD-Editor will das nicht. */
    protected boolean festeGroesse() {
        return true;
    }

    @Override
    protected void init() {
        if (festeGroesse()) VScale.anwenden(this);
    }

    @Override
    public void removed() {
        VScale.zuruecksetzen();
        super.removed();
    }

    protected boolean handlePress(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean handleDrag(double mouseX, double mouseY) {
        return false;
    }

    protected boolean handleRelease() {
        return false;
    }

    protected boolean handleScroll(double amount) {
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (handlePress(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (handleDrag(click.x(), click.y())) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (handleRelease()) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (handleScroll(vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }
}
