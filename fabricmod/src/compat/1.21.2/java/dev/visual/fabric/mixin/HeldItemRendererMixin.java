package dev.visual.fabric.mixin;

import dev.visual.fabric.HandTransform;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Position und Größe der Hände in der Ego-Perspektive.
 * RETURN statt TAIL, damit push/pop auch bei frühen Returns paarig bleiben.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void visualsfabric$handPre(AbstractClientPlayerEntity player, float tickDelta,
                                       float pitch, Hand hand, float swingProgress, ItemStack item,
                                       float equipProgress, MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers, int light,
                                       CallbackInfo ci) {
        matrices.push();
        HandTransform.apply(matrices, hand);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void visualsfabric$handPost(AbstractClientPlayerEntity player, float tickDelta,
                                        float pitch, Hand hand, float swingProgress, ItemStack item,
                                        float equipProgress, MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers, int light,
                                        CallbackInfo ci) {
        matrices.pop();
    }
}
