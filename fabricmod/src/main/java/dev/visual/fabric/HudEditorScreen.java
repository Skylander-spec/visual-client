package dev.visual.fabric;

import dev.visual.fabric.ui.BlurGuard;
import dev.visual.fabric.ui.VButton;
import dev.visual.fabric.ui.VStyle;
import dev.visual.fabric.ui.VText;
import net.minecraft.client.gui.DrawContext;
import dev.visual.fabric.ui.VMouseScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * HUD-Editor: alle Bausteine als Kästen, die mit der Maus frei verschoben
 * werden. Positionen landen in der Config und gelten sofort im Spiel.
 */
public class HudEditorScreen extends VMouseScreen {
    /** Rasterweite beim Ziehen (mit Shift frei). */
    private static final int GRID = 4;
    private final Screen parent;
    private String dragging = null;
    private int dragDX;
    private int dragDY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal(VText.t("ui.hudedit")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BlurGuard.off();
        addDrawableChild(new VButton(width / 2 - 104, height - 28, 100, 20,
                Text.literal(VText.t("set.reset")), () -> {
            VConfig.get().hudPos.clear();
            VConfig.save();
        }));
        addDrawableChild(new VButton(width / 2 + 4, height - 28, 100, 20,
                Text.literal(VText.t("ui.done")), this::close).primary());
    }

    @Override
    public void close() {
        VConfig.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    protected boolean handlePress(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // von hinten nach vorne prüfen, damit oben liegende zuerst greifen
            for (int i = HudElements.ALL.length - 1; i >= 0; i--) {
                HudElements.Element e = HudElements.ALL[i];
                if (!enabled(e.key())) continue;
                int[] p = HudElements.pos(e.key(), width, height);
                int[] s2 = HudElements.size(e);
                if (mouseX >= p[0] && mouseX <= p[0] + s2[0]
                        && mouseY >= p[1] && mouseY <= p[1] + s2[1]) {
                    dragging = e.key();
                    dragDX = (int) mouseX - p[0];
                    dragDY = (int) mouseY - p[1];
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean handleDrag(double mouseX, double mouseY) {
        if (dragging != null) {
            int[] s = HudElements.size(element(dragging));
            int nx = (int) mouseX - dragDX;
            int ny = (int) mouseY - dragDY;
            // Am Raster einrasten; mit Shift frei positionieren
            if (!Compat.isKeyDown(MinecraftClient.getInstance(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                nx = Math.round(nx / (float) GRID) * GRID;
                ny = Math.round(ny / (float) GRID) * GRID;
            }
            nx = Math.max(0, Math.min(nx, width - s[0]));
            ny = Math.max(0, Math.min(ny, height - s[1]));
            HudElements.set(dragging, nx, ny);
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleRelease() {
        if (dragging != null) {
            dragging = null;
            VConfig.save();
            return true;
        }
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // heller Schleier statt fast schwarz — das Spiel bleibt erkennbar,
        // die Kästen heben sich trotzdem klar ab
        ctx.fill(0, 0, width, height, 0x66101820);

        // Raster, damit man sauber ausrichten kann
        for (int gx = 0; gx < width; gx += GRID) {
            ctx.fill(gx, 0, gx + 1, height, 0x14FFFFFF);
        }
        for (int gy = 0; gy < height; gy += GRID) {
            ctx.fill(0, gy, width, gy + 1, 0x14FFFFFF);
        }
        // Mittelachsen kräftiger
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x40FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x40FFFFFF);

        for (HudElements.Element e : HudElements.ALL) {
            boolean on = enabled(e.key());
            boolean drag = e.key().equals(dragging);
            int[] p = HudElements.pos(e.key(), width, height);
            int[] s = HudElements.size(e);
            int border = drag ? 0xFF7DE3FF : on ? VStyle.ACCENT : 0x80889099;
            // Vorschau des echten Inhalts, damit man sieht was man verschiebt
            if (!on) ctx.fill(p[0], p[1], p[0] + s[0], p[1] + s[1], 0x60000000);
            HudOverlay.preview(ctx, MinecraftClient.getInstance(), e.key(), p[0], p[1]);
            HudElements.outline(ctx, p[0], p[1], s[0], s[1], border);
            // Griff-Ecken zeigen, dass der Kasten ziehbar ist
            int hc = drag ? 0xFF7DE3FF : 0x90FFFFFF;
            ctx.fill(p[0], p[1], p[0] + 4, p[1] + 1, hc);
            ctx.fill(p[0], p[1], p[0] + 1, p[1] + 4, hc);
            ctx.fill(p[0] + s[0] - 4, p[1] + s[1] - 1, p[0] + s[0], p[1] + s[1], hc);
            ctx.fill(p[0] + s[0] - 1, p[1] + s[1] - 4, p[0] + s[0], p[1] + s[1], hc);

            // Beschriftung über den Kasten, damit sie die Vorschau nicht verdeckt
            String label = VText.t(e.label()) + (on ? "" : VText.t("ui.hudoff"));
            int lw = textRenderer.getWidth(label);
            int ly = p[1] >= 12 ? p[1] - 11 : p[1] + s[1] + 2;
            ctx.fill(p[0], ly - 1, p[0] + lw + 6, ly + 9, 0xB0000000);
            ctx.drawText(textRenderer, label, p[0] + 3, ly,
                    on ? 0xFFFFFFFF : 0xFF9AA6B2, false);
        }

        String hint = VText.t("ui.hudhint", GRID);
        int hw = textRenderer.getWidth(hint);
        ctx.fill((width - hw) / 2 - 6, 4, (width + hw) / 2 + 6, 20, 0xB0000000);
        ctx.drawText(textRenderer, hint, (width - hw) / 2, 8, 0xFFDDE6EE, true);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static HudElements.Element element(String key) {
        for (HudElements.Element e : HudElements.ALL) {
            if (e.key().equals(key)) return e;
        }
        return HudElements.ALL[0];
    }

    /** Ist der zugehörige Schalter an? Nur zur Darstellung im Editor. */
    private static boolean enabled(String key) {
        VConfig c = VConfig.get();
        return switch (key) {
            case "fps" -> c.fps;
            case "cps" -> c.cps;
            case "coords" -> c.coords;
            case "ping" -> c.ping;
            case "keystrokes" -> c.keystrokes;
            case "radar" -> c.radar;
            case "armor" -> c.armorHud;
            case "potions" -> c.potionHud;
            case "scoreboard" -> c.scoreboardClean;
            case "watermark" -> c.watermark;
            case "clock" -> c.clock;
            case "target" -> c.targetHud;
            case "items" -> c.itemCounter;
            case "vanilla_hotbar", "vanilla_status" -> c.vanillaHudMove;
            default -> false;
        };
    }

    @Override
    public void removed() {
        BlurGuard.restore();
        super.removed();
    }
}
