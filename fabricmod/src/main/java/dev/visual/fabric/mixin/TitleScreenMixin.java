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
            if (mc == null || mc.currentScreen instanceof VisualTitleScreen) return;
            mc.setScreen(new VisualTitleScreen());
        } catch (Throwable t) {
            // Lieber der gewohnte Startbildschirm als ein Absturz beim Start
        }
    }
}
