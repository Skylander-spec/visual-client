package dev.visual.fabric.ui;

import net.minecraft.client.gui.Click;
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
