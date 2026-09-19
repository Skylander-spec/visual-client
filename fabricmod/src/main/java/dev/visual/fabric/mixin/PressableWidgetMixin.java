package dev.visual.fabric.mixin;

import dev.visual.fabric.VConfig;
import dev.visual.fabric.ui.VStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zeichnet Minecrafts eigene Knoepfe im Stil des Clients.
 *
 * Optionen, Mehrspieler und die Welt-Auswahl sind Minecrafts Bildschirme --
 * sie nachzubauen waere ein eigenes Projekt und wuerde bei jeder Version neu
 * brechen. Stattdessen wird hier nur das Aussehen der Knoepfe ersetzt: gleiche
 * Form, gleiche Farben, gleiche Rundung wie auf unseren Seiten. Der Rest der
 * Bildschirme bleibt Minecrafts, und damit auch ihre Funktion.
 *
 * Angesetzt an PressableWidget, weil dort die gemeinsame Zeichnung aller
 * gewoehnlichen Knoepfe liegt. Widgets mit eigenem Aussehen (Schloss-Symbol,
 * Bild-Knoepfe) ueberschreiben renderWidget selbst und kommen hier nie an --
 * ihre Symbole bleiben also unberuehrt.
 */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {
    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void visualsfabric$clientStil(DrawContext ctx, int mouseX, int mouseY, float delta,
                                          CallbackInfo ci) {
        try {
            if (!VConfig.get().clientMenues) return;
            ClickableWidget w = (ClickableWidget) (Object) this;
            VStyle.knopf(ctx, w.getX(), w.getY(), w.getWidth(), w.getHeight(),
                    w.isHovered(), w.active);

            // Die Beschriftung zeichnet sonst der abgebrochene Teil
            MinecraftClient mc = MinecraftClient.getInstance();
            int farbe = w.active ? VStyle.TEXT : VStyle.TEXT_FAINT;
            ctx.drawCenteredTextWithShadow(mc.textRenderer, w.getMessage(),
                    w.getX() + w.getWidth() / 2,
                    w.getY() + (w.getHeight() - 8) / 2, farbe);
            ci.cancel();
        } catch (Throwable t) {
            // Im Zweifel lieber Minecrafts Knopf als gar keinen
        }
    }
}
