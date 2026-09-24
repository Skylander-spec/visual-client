package dev.visual.fabric.mixin;

import dev.visual.fabric.ui.ScreenLifecycle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public class ScreenLifecycleMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen visualsfabric$freshScreen(Screen requested) {
        return ScreenLifecycle.prepare(((MinecraftClient) (Object) this).currentScreen, requested);
    }
}
