package dev.visual.fabric.mixin;

import dev.visual.fabric.MiniMe;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zeichnet den Mini-Me: nach dem regulaeren Durchlauf derselbe Renderer
 * noch einmal, nur verschoben und verkleinert.
 *
 * Angesetzt wird an LivingEntityRenderer statt an PlayerEntityRenderer,
 * weil render() dort gar nicht steht - der Spieler-Renderer erbt es nur.
 * Die Pruefung auf den eigenen Render-Zustand steckt in MiniMe.beginnen.
 */
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("unchecked")
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void visualsfabric$miniMe(LivingEntityRenderState zustand, MatrixStack matrizen,
                                      VertexConsumerProvider abnehmer, int licht,
                                      CallbackInfo ci) {
        if (!MiniMe.beginnen(zustand)) return;
        // push vor dem try, pop im finally: wirft das innere Rendern,
        // bliebe die Matrix sonst verschoben - und zwar fuer alles,
        // was in diesem Bild noch danach kommt.
        matrizen.push();
        try {
            MiniMe.platzieren(matrizen, zustand.bodyYaw);
            // Roher Typ mit Absicht: mit ? statt Typvariable lehnt der
            // Compiler den Zustand ab, obwohl es derselbe ist.
            @SuppressWarnings("rawtypes")
            LivingEntityRenderer roh = (LivingEntityRenderer) (Object) this;
            roh.render(zustand, matrizen, abnehmer, licht);
        } catch (Throwable t) {
            // Ein Fehler hier wuerde jeden Frame kommen - lieber ohne Mini
        } finally {
            matrizen.pop();
            MiniMe.beenden();
        }
    }
}
