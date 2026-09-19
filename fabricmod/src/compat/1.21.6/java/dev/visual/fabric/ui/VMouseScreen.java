package dev.visual.fabric.ui;

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
