package dev.visual.fabric.mixin;

import dev.visual.fabric.HudOverlay;
import dev.visual.fabric.Compat;
import dev.visual.fabric.HudElements;
import dev.visual.fabric.VConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void visualsfabric$crosshair(DrawContext context, RenderTickCounter tickCounter,
                                         CallbackInfo ci) {
        if (VConfig.get().crosshair) {
            HudOverlay.drawCrosshair(context);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void visualsfabric$hud(DrawContext context, RenderTickCounter tickCounter,
                                   CallbackInfo ci) {
        HudOverlay.render(context);
    }

    // ── Vanilla-HUD frei verschieben ───────────────────────────────────────
    // Die Vanilla-Teile lassen sich nicht umpositionieren, also verschieben wir
    // die Zeichenmatrix um die im HUD-Editor gesetzte Differenz. RETURN statt
    // TAIL, damit push/pop auch bei frühen Returns paarig bleibt.

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void visualsfabric$hotbarPre(DrawContext ctx, RenderTickCounter tickCounter,
                                         CallbackInfo ci) {
        visualsfabric$shift(ctx, "vanilla_hotbar");
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void visualsfabric$hotbarPost(DrawContext ctx, RenderTickCounter tickCounter,
                                          CallbackInfo ci) {
        if (VConfig.get().vanillaHudMove) Compat.hudPop(ctx);
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"))
    private void visualsfabric$statusPre(DrawContext ctx, CallbackInfo ci) {
        visualsfabric$shift(ctx, "vanilla_status");
    }

    @Inject(method = "renderStatusBars", at = @At("RETURN"))
    private void visualsfabric$statusPost(DrawContext ctx, CallbackInfo ci) {
        if (VConfig.get().vanillaHudMove) Compat.hudPop(ctx);
    }

    private static void visualsfabric$shift(DrawContext ctx, String key) {
        if (!VConfig.get().vanillaHudMove) return;
        Compat.hudPush(ctx);
        int[] off = HudElements.offset(key, ctx.getScaledWindowWidth(),
                ctx.getScaledWindowHeight());
        Compat.hudTranslate(ctx, off[0], off[1]);
    }

    /**
     * Vanilla-Seitenleiste abschalten, wenn unser aufgeräumtes Scoreboard aktiv
     * ist — sonst würden beide übereinander liegen.
     */
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true)
    private void visualsfabric$scoreboard(DrawContext context, RenderTickCounter tickCounter,
                                          CallbackInfo ci) {
        if (VConfig.get().scoreboardClean) {
            ci.cancel();
        }
    }
}
