package dev.visual.fabric.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Screen-Basis mit versionsunabhängigen Maus-Rückrufen. Bis 1.21.8 kommen
 * Maus-Ereignisse als (x, y, button); Unterklassen sehen nur handle*.
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
        applyBlur();
    }

    /**
     * Groesser zeichnen: alles zwischen skalieren und zurueck wird um
     * {@code faktor} vergroessert, Ursprung ist (x, y). Der Matrizen-Typ
     * wechselte mit 1.21.6, deshalb steht das hier.
     */
    protected void skalieren(DrawContext ctx, float x, float y, float faktor) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0.0f);
        ctx.getMatrices().scale(faktor, faktor, 1.0f);
    }

    protected void zurueckskalieren(DrawContext ctx) {
        ctx.getMatrices().pop();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handlePress(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (handleDrag(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (handleRelease()) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (handleScroll(vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }
}
