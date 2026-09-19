package dev.visual.fabric.mixin;

import dev.visual.fabric.VConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gibt Minecrafts eigenen Menues denselben Hintergrund wie unserem
 * Startbildschirm: Panorama plus dunkler Schleier statt Weichzeichner.
 *
 * Nur ausserhalb der Welt — das Pausenmenue soll weiter die Welt zeigen.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Shadow
    protected abstract void renderPanoramaBackground(DrawContext ctx, float delta);

    /** Einmal fehlgeschlagen heisst: nicht jeden Frame erneut versuchen. */
    private static boolean visualsfabric$panoramaAus = false;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void visualsfabric$clientHintergrund(DrawContext ctx, int mouseX, int mouseY,
                                                 float delta, CallbackInfo ci) {
        try {
            if (!VConfig.get().clientMenues) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.world != null) return;
            Screen selbst = (Screen) (Object) this;
            // Unsere eigenen Bildschirme malen ihren Hintergrund selbst.
            if (selbst.getClass().getName().startsWith("dev.visual.fabric.")) return;

            int b = selbst.width;
            int h = selbst.height;
            boolean gezeichnet = false;
            if (!visualsfabric$panoramaAus) {
                try {
                    renderPanoramaBackground(ctx, delta);
                    gezeichnet = true;
                } catch (Throwable t) {
                    visualsfabric$panoramaAus = true;
                }
            }
            if (!gezeichnet) ctx.fillGradient(0, 0, b, h, 0xFF0E1419, 0xFF11171C);
            ctx.fill(0, 0, b, h, 0x73060A0E);
            ctx.fillGradient(0, 0, b, 90, 0x4D00060A, 0x00000000);
            ctx.fillGradient(0, h - 90, b, h, 0x00000000, 0x4D00060A);
            ci.cancel();
        } catch (Throwable t) {
            // Lieber Vanilla-Hintergrund als gar keiner.
        }
    }
}
