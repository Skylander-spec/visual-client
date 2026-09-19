package dev.visual.legacy.mixin;

import dev.visual.legacy.ClickTracker189;
import dev.visual.legacy.VisualScreen189;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ein Tick-Haken erledigt beides: CPS zaehlen und die rechte Umschalttaste
 * abfragen. Fabric API ist hier nicht eingebunden, also kein Event-Callback.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private boolean visual$keyWas;

    @Inject(method = "tick", at = @At("HEAD"))
    private void visual$tick(CallbackInfo ci) {
        ClickTracker189.tick();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        // Nur die Flanke zaehlt, sonst oeffnet und schliesst es im Dauerlauf
        if (down && !visual$keyWas && mc.currentScreen == null && mc.player != null) {
            mc.setScreen(new VisualScreen189());
        }
        visual$keyWas = down;
    }
}
