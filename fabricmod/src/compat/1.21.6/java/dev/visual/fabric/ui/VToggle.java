package dev.visual.fabric.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Schalter im Launcher-Stil: Label links, Switch-Pille rechts. */
public class VToggle extends PressableWidget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public VToggle(int x, int y, int w, int h, Text label,
                   BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, w, h, label);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onPress() {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean on = getter.getAsBoolean();
        boolean hov = isHovered();
        VStyle.card(ctx, getX(), getY(), width, height, hov);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawTextWithShadow(tr, getMessage(), getX() + 8,
                getY() + (height - 8) / 2, hov ? VStyle.TEXT : VStyle.TEXT_DIM);
        // Switch-Pille
        int sw = 22;
        int sh = 10;
        int sx = getX() + width - sw - 8;
        int sy = getY() + (height - sh) / 2;
        ctx.fill(sx, sy, sx + sw, sy + sh, on ? 0xFF0D4F49 : 0xFF12241F);
        VStyle.border(ctx, sx, sy, sw, sh, on ? VStyle.ACCENT : VStyle.BORDER_HI);
        int knob = sh - 4;
        int kx = on ? sx + sw - knob - 2 : sx + 2;
        ctx.fill(kx, sy + 2, kx + knob, sy + 2 + knob, on ? VStyle.ACCENT : VStyle.TEXT_FAINT);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
