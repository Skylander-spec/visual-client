package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Schalter: Beschriftung links, Pille rechts. Siehe VButton zur Basisklasse. */
public class VToggle extends ClickableWidget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public VToggle(int x, int y, int w, int h, Text label,
                   BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, w, h, label);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (active && visible) setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean on = getter.getAsBoolean();
        boolean hov = isHovered();
        VStyle.card(ctx, getX(), getY(), width, height, hov);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawTextWithShadow(tr, getMessage(), getX() + 8,
                getY() + (height - 8) / 2, hov ? VStyle.TEXT : VStyle.TEXT_DIM);
        int sw = 24;
        int sh = 12;
        int sx = getX() + width - sw - 8;
        int sy = getY() + (height - sh) / 2;
        VStyle.roundRect(ctx, sx, sy, sw, sh, sh / 2, on ? VStyle.ACCENT : 0x33FFFFFF);
        int knob = sh - 4;
        int kx = on ? sx + sw - knob - 2 : sx + 2;
        VStyle.roundRect(ctx, kx, sy + 2, knob, knob, knob / 2, 0xFFFFFFFF);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
