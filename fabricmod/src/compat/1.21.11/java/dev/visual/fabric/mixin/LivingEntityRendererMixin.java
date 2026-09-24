package dev.visual.fabric.mixin;

import dev.visual.fabric.MiniMe;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
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
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void visualsfabric$miniMe(LivingEntityRenderState zustand, MatrixStack matrizen,
                                      OrderedRenderCommandQueue schlange,
                                      CameraRenderState kamera, CallbackInfo ci) {
        if (!MiniMe.beginnen(zustand)) return;
        // push vor dem try, pop im finally: wirft das innere Rendern,
        // bliebe die Matrix sonst verschoben - und zwar fuer alles,
        // was in diesem Bild noch danach kommt.
        matrizen.push();
        try {
            MiniMe.platzieren(matrizen, zustand.bodyYaw);
            if (dev.visual.fabric.Begleiter.custom(MiniMe.config().miniMeArt)) {
                var model = visualsfabric$models.computeIfAbsent(zustand, key -> new dev.visual.fabric.ui.FantasyModel[6]);
                int index = MiniMe.config().miniMeArt - 7;
                if (model[index] == null) model[index] = new dev.visual.fabric.ui.FantasyModel(index + 7);
                visualsfabric$fantasy(model[index], zustand, matrizen, schlange);
                return;
            }
            var pet = dev.visual.fabric.Begleiter.get();
            if (pet != null) {
                @SuppressWarnings("rawtypes")
                net.minecraft.client.render.entity.EntityRenderer renderer =
                        net.minecraft.client.MinecraftClient.getInstance()
                                .getEntityRenderDispatcher().getRenderer(pet);
                var petState = dev.visual.fabric.Begleiter.state(renderer, pet, zustand);
                if (petState instanceof LivingEntityRenderState living) {
                    living.bodyYaw = zustand.bodyYaw;
                    living.light = zustand.light;
                    living.limbSwingAnimationProgress = MiniMe.schritt();
                    living.limbSwingAmplitude = MiniMe.bewegung();
                }
                renderer.render(petState, matrizen, schlange, kamera);
                return;
            }

            // Eigene Puppe statt eines zweiten Durchlaufs desselben
            // Renderers: seit 1.21.9 wird erst am Bildende gezeichnet, und
            // dann haetten beide Einreichungen dieselbe Pose.
            if (zustand instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState p) {
                dev.visual.fabric.ui.MiniPuppe.zeichnen(p, matrizen, schlange, zustand.light);
                visualsfabric$accessories(zustand, matrizen, schlange);
            }
        } catch (Throwable t) {
            // Ein Fehler hier wuerde jeden Frame kommen - lieber ohne Mini
        } finally {
            matrizen.pop();
            MiniMe.beenden();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private final java.util.Map<LivingEntityRenderState, dev.visual.fabric.ui.FantasyModel[]> visualsfabric$models = new java.util.WeakHashMap<>();

    @org.spongepowered.asm.mixin.Unique
    private void visualsfabric$fantasy(dev.visual.fabric.ui.FantasyModel model, LivingEntityRenderState zustand,
            MatrixStack matrizen, OrderedRenderCommandQueue schlange) {
        model.capture(zustand);
        matrizen.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(
                dev.visual.fabric.MiniAnimation.modelYaw(zustand.bodyYaw)));
        matrizen.scale(-1f, -1f, 1f);
                schlange.submitModel(model, model.pose, matrizen, model.getLayer(model.texture),
                        zustand.light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 0, null);
    }

    @org.spongepowered.asm.mixin.Unique
    private final java.util.Map<LivingEntityRenderState, dev.visual.fabric.ui.MiniAccessories> visualsfabric$accessoryModels = new java.util.WeakHashMap<>();
    @org.spongepowered.asm.mixin.Unique
    private void visualsfabric$accessories(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue) {
        var c = MiniMe.config();
        if (c.miniMeHut == 0 && c.miniMeScarf == 0 && c.miniMeShoes == 0 && !c.miniMeFluegel) return;
        var model = visualsfabric$accessoryModels.computeIfAbsent(state, key -> new dev.visual.fabric.ui.MiniAccessories());
        model.capture(state);
        matrices.push();
        try {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(dev.visual.fabric.MiniAnimation.modelYaw(state.bodyYaw)));
            matrices.scale(-1,-1,1);
            matrices.translate(0,-1.501,0);
        queue.submitModel(model, model.pose, matrices, model.getLayer(dev.visual.fabric.ui.MiniAccessories.TEXTURE),
                state.light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 0, null);
        } finally { matrices.pop(); }
    }
}
