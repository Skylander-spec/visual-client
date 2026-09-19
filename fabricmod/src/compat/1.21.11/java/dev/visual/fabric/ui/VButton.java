package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Knopf im Visual-Client-Stil.
 *
 * Ab 1.21.10 ist PressableWidget.renderWidget final und drawIcon abstrakt —
 * eine eigene Zeichnung ist darüber nicht mehr möglich, ohne den vanilla
 * Knopf-Hintergrund mitzunehmen. Deshalb hängt dieser Knopf direkt an
 * ClickableWidget, wo renderWidget weiterhin überschrieben werden darf.
 */
public class VButton extends ClickableWidget {
    private final Runnable action;
    private boolean primary;
    private boolean danger;

    public VButton(int x, int y, int w, int h, Text label, Runnable action) {
        super(x, y, w, h, label);
        this.action = action;
    }

    public VButton primary() {
        this.primary = true;
        return this;
    }

    public VButton danger() {
        this.danger = true;
        return this;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (active && visible) action.run();
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hov = isHovered() && active;
        int bg = hov ? VStyle.CARD_HOVER : (primary ? 0xE60D2A2F : VStyle.CARD);
        int textCol = !active ? VStyle.TEXT_FAINT
                : danger ? VStyle.DANGER
                : (primary || hov ? VStyle.ACCENT : VStyle.TEXT);
        VStyle.roundRect(ctx, getX(), getY(), width, height, VStyle.R_CARD, bg);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), getX() + width / 2,
                getY() + (height - 8) / 2, textCol);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
