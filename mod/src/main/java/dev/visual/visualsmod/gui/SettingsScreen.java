package dev.visual.visualsmod.gui;

import dev.visual.visualsmod.config.VisualsConfig;
import dev.visual.visualsmod.sounds.Sounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** In-Game-Settings (Standard: Rechts-Shift). Alle Module einzeln schaltbar. */
public class SettingsScreen extends Screen {
    private static final String[] CROSSHAIR_STYLES = {"Kreuz", "Punkt", "Kreis"};
    private static final int[] COLORS = {0x22D3EE, 0xF45568, 0x34D399, 0xFACC15, 0xC084FC, 0xFFFFFF};
    private static final String[] COLOR_NAMES = {"Cyan", "Rot", "Grün", "Gelb", "Lila", "Weiß"};

    public SettingsScreen() {
        super(Component.literal("Visual Client — Einstellungen"));
    }

    @Override
    protected void init() {
        VisualsConfig c = VisualsConfig.INSTANCE;
        int w = 150, h = 20, gap = 4;
        int cx = this.width / 2;
        int x1 = cx - w - 6, x2 = cx + 6;
        int y = 40;

        toggle(x1, y, w, h, "Crosshair", () -> c.crosshairEnabled, v -> c.crosshairEnabled = v);
        cycle(x2, y, w, h, "Crosshair-Stil", () -> CROSSHAIR_STYLES[c.crosshairStyle],
                () -> c.crosshairStyle = (c.crosshairStyle + 1) % CROSSHAIR_STYLES.length);
        y += h + gap;
        cycle(x1, y, w, h, "Farbe", () -> COLOR_NAMES[colorIndex(c.crosshairColor)],
                () -> c.crosshairColor = COLORS[(colorIndex(c.crosshairColor) + 1) % COLORS.length]);
        toggle(x2, y, w, h, "Crosshair-Glow", () -> c.crosshairGlow, v -> c.crosshairGlow = v);
        y += h + gap;
        toggle(x1, y, w, h, "Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
        toggle(x2, y, w, h, "CPS-Anzeige", () -> c.showCps, v -> c.showCps = v);
        y += h + gap;
        toggle(x1, y, w, h, "FPS-Anzeige", () -> c.showFps, v -> c.showFps = v);
        toggle(x2, y, w, h, "Armor-Status", () -> c.armorStatus, v -> c.armorStatus = v);
        y += h + gap;
        toggle(x1, y, w, h, "Scoreboard-Restyle", () -> c.sidebarRestyle, v -> c.sidebarRestyle = v);
        toggle(x2, y, w, h, "Punktzahlen verstecken", () -> c.hideSidebarNumbers, v -> c.hideSidebarNumbers = v);
        y += h + gap;
        toggle(x1, y, w, h, "Tab-Restyle", () -> c.tabRestyle, v -> c.tabRestyle = v);
        toggle(x2, y, w, h, "Hit-Partikel", () -> c.hitParticles, v -> c.hitParticles = v);
        y += h + gap;
        toggle(x1, y, w, h, "Kill-Effekt", () -> c.killEffect, v -> c.killEffect = v);
        toggle(x2, y, w, h, "Kill-Sound", () -> c.killSound, v -> c.killSound = v);
        y += h + gap;
        toggle(x1, y, w, h, "Extra-Hitsound", () -> c.hitSound, v -> c.hitSound = v);
        toggle(x2, y, w, h, "Cape", () -> c.capeEnabled, v -> c.capeEnabled = v);
        y += h + gap;

        addRenderableWidget(new AbstractSliderButton(x1, y, w * 2 + 12, h,
                particleLabel(c.particleCount), c.particleCount / 16.0) {
            @Override
            protected void updateMessage() {
                setMessage(particleLabel(VisualsConfig.INSTANCE.particleCount));
            }

            @Override
            protected void applyValue() {
                VisualsConfig.INSTANCE.particleCount = (int) Math.round(this.value * 16);
                VisualsConfig.save();
            }
        });
        y += h + gap * 3;

        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(cx - 75, y, 150, h).build());
    }

    private static Component particleLabel(int count) {
        return Component.literal("Partikel-Menge: " + (count == 0 ? "aus" : count));
    }

    private int colorIndex(int color) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return i;
        return 0;
    }

    private void toggle(int x, int y, int w, int h, String label,
                        Supplier<Boolean> get, Consumer<Boolean> set) {
        Function<Boolean, Component> text = v ->
                Component.literal(label + ": " + (v ? "§bAn" : "§7Aus"));
        addRenderableWidget(Button.builder(text.apply(get.get()), b -> {
            set.accept(!get.get());
            b.setMessage(text.apply(get.get()));
            VisualsConfig.save();
            Sounds.playUi(Sounds.UI_CLICK, 1.0f);
        }).bounds(x, y, w, h).build());
    }

    private void cycle(int x, int y, int w, int h, String label,
                       Supplier<String> get, Runnable next) {
        addRenderableWidget(Button.builder(Component.literal(label + ": §b" + get.get()), b -> {
            next.run();
            b.setMessage(Component.literal(label + ": §b" + get.get()));
            VisualsConfig.save();
            Sounds.playUi(Sounds.UI_CLICK, 1.0f);
        }).bounds(x, y, w, h).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFF22D3EE);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        VisualsConfig.save();
        super.onClose();
    }
}
