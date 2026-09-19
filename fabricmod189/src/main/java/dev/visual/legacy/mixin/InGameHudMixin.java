package dev.visual.legacy.mixin;

import dev.visual.legacy.HudRenderer189;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zeichnet die Visual-Bausteine, nachdem Minecraft sein HUD gezeichnet hat. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void visual$renderOverlay(float delta, CallbackInfo ci) {
        HudRenderer189.render();
    }
}
