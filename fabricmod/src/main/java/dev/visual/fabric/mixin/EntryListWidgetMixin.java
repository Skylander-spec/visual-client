package dev.visual.fabric.mixin;

import dev.visual.fabric.VConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Die Listen in Mehrspieler, Welt waehlen und den Optionen bringen von Haus aus
 * einen Dreck-Streifen und zwei Trennlinien mit. Ueber unserem Hintergrund
 * sieht das nach Vanilla aus, also lassen wir beides weg.
 */
@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    private static boolean visualsfabric$aus() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            return VConfig.get().clientMenues && mc != null && mc.world == null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Inject(method = "drawMenuListBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void visualsfabric$keinDreck(DrawContext ctx, CallbackInfo ci) {
        if (visualsfabric$aus()) ci.cancel();
    }

    @Inject(method = "drawHeaderAndFooterSeparators", at = @At("HEAD"), cancellable = true, require = 0)
    private void visualsfabric$keineTrenner(DrawContext ctx, CallbackInfo ci) {
        if (visualsfabric$aus()) ci.cancel();
    }
}
