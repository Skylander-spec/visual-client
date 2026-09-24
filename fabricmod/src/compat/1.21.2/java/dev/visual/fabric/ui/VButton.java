package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/** Custom Button im Visual-Client-Stil (flaches Panel, Cyan-Hover-Glow). */
public class VButton extends PressableWidget {
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
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hov = isHovered() && active;
        int bg = hov ? VStyle.CARD_HOVER : (primary ? 0xE60D2A2F : VStyle.CARD);
        int borderCol = danger ? (hov ? VStyle.DANGER : 0xFF7A2B34)
                : (primary || hov ? VStyle.ACCENT : VStyle.BORDER_HI);
        int textCol = !active ? VStyle.TEXT_FAINT
                : danger ? VStyle.DANGER
                : (primary || hov ? VStyle.ACCENT : VStyle.TEXT);
        if (hov) {
            ctx.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, VStyle.ACCENT_BG);
        }
        ctx.fill(getX(), getY(), getX() + width, getY() + height, bg);
        VStyle.border(ctx, getX(), getY(), width, height, borderCol);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), getX() + width / 2,
                getY() + (height - 8) / 2, textCol);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
