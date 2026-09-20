package dev.visual.fabric.mixin;

import dev.visual.fabric.VConfig;
import dev.visual.fabric.ui.VisualTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ersetzt Minecrafts Startbildschirm durch den eigenen im OneClient-Stil.
 *
 * Bewusst ein Austausch statt eines Umbaus der Vanilla-Knöpfe: so bleiben
 * die dahinterliegenden Bildschirme unverändert, und wenn etwas schiefgeht,
 * steht einfach wieder der normale Startbildschirm da — das Spiel stürzt
 * nicht ab. Abschaltbar über das Modul „Startbildschirm".
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void visualsfabric$ersetzen(CallbackInfo ci) {
        try {
            if (!VConfig.get().customTitle) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            // Der Wachposten muss beide Fassungen kennen: der Bildschirm
            // ist entweder VisualTitleScreen oder der von OneConfig
            // gezeichnete VTitleScreen. Frueher stand hier nur der erste -
            // nach dem Umstieg haette das bei jedem Durchlauf einen neuen
            // Bildschirm gesetzt. Ueber den Namen geprueft, weil
            // VTitleScreen ohne OneConfig gar nicht ladbar ist.
            if (mc == null || mc.currentScreen == null) return;
            String klasse = mc.currentScreen.getClass().getName();
            if (klasse.equals("dev.visual.fabric.ui.VisualTitleScreen")
                    || klasse.equals("dev.visual.fabric.ui.VTitleScreen")) return;
            mc.setScreen(VisualTitleScreen.oeffnen());
        } catch (Throwable t) {
            // Lieber der gewohnte Startbildschirm als ein Absturz beim Start
        }
    }
}
