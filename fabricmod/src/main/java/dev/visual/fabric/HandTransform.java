package dev.visual.fabric;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

/**
 * Verschiebung und Größe der Ego-Perspektive-Hände. Wird vom
 * HeldItemRenderer-Mixin vor dem Zeichnen angewandt; der MatrixStack-Typ ist
 * in allen unterstützten Versionen derselbe, daher gemeinsamer Code.
 */
public final class HandTransform {
    private HandTransform() {
    }

    public static void apply(MatrixStack matrices, Hand hand) {
        VConfig c = VConfig.get();
        boolean main = hand == Hand.MAIN_HAND;
        float x = main ? c.mainHandX : c.offHandX;
        float y = main ? c.mainHandY : c.offHandY;
        float z = main ? c.mainHandZ : c.offHandZ;
        float scale = main ? c.mainHandScale : c.offHandScale;
        if (x != 0f || y != 0f || z != 0f) {
            matrices.translate(x, y, z);
        }
        if (scale != 1f) {
            matrices.scale(scale, scale, scale);
        }
    }
}
