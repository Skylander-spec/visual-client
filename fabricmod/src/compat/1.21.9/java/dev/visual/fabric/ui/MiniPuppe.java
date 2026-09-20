package dev.visual.fabric.ui;

import dev.visual.fabric.MiniSkin;
import dev.visual.fabric.VConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Der Mini-Me als eigene Puppe.
 *
 * Frueher wurde derselbe Renderer ein zweites Mal aufgerufen. Das ging bis
 * 1.21.8 gut, seit 1.21.9 sammelt Minecraft die Zeichenbefehle aber erst
 * ein und fuehrt sie am Bildende aus - und {@code submitModel} merkt sich
 * Modell und Zustand als Referenz. Zwei Einreichungen desselben Modells
 * koennen darum nie verschiedene Posen haben: die zuletzt gesetzte gilt
 * fuer beide, und die Sitzhaltung des Kleinen klebte am grossen Spieler.
 *
 * Deshalb hier ein eigenes Modell und ein eigener Zustand. Das kostet den
 * Mitlauf von Ruestung und Handgegenstand - die zeichnen Feature-Renderer,
 * die nur im vollen Durchlauf mitlaufen - bringt dafuer aber eine eigene
 * Haltung, und genau die war gewuenscht.
 */
public final class MiniPuppe {
    private static PlayerEntityModel modell;
    private static boolean modellSchlank;
    private static final PlayerEntityRenderState EIGEN = new PlayerEntityRenderState();

    private MiniPuppe() {
    }

    private static PlayerEntityModel modell(boolean schlank) {
        if (modell == null || modellSchlank != schlank) {
            // Das Modell wird selbst gebaut statt ueber EntityModelLayers:
            // dort tragen zwei verschiedene Felder den Namen PLAYER_SLIM
            // (Modell und Ausruestung), das laesst sich im Quelltext nicht
            // aufloesen. getTexturedModelData nimmt den Schlank-Schalter
            // ohnehin direkt entgegen.
            modell = new PlayerEntityModel(TexturedModelData
                    .of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, schlank), 64, 64)
                    .createModel(), schlank);
            modellSchlank = schlank;
        }
        return modell;
    }

    /** Zeichnet den Kleinen an der Stelle, auf die die Matrix schon zeigt. */
    public static void zeichnen(PlayerEntityRenderState quelle, MatrixStack matrizen,
                                OrderedRenderCommandQueue schlange, int licht) {
        try {
            boolean schlank = quelle.skinTextures != null
                    && quelle.skinTextures.model() == net.minecraft.entity.player.PlayerSkinType.SLIM;
            PlayerEntityModel m = modell(schlank);

            // Nur uebernehmen, was das Modell selbst liest
            EIGEN.skinTextures = quelle.skinTextures;
            EIGEN.hatVisible = quelle.hatVisible;
            EIGEN.jacketVisible = quelle.jacketVisible;
            EIGEN.leftSleeveVisible = quelle.leftSleeveVisible;
            EIGEN.rightSleeveVisible = quelle.rightSleeveVisible;
            EIGEN.leftPantsLegVisible = quelle.leftPantsLegVisible;
            EIGEN.rightPantsLegVisible = quelle.rightPantsLegVisible;
            EIGEN.baseScale = 1.0f;

            m.setAngles(EIGEN);
            haltung(m, quelle);

            Identifier haut = MiniSkin.textur();
            if (haut == null && quelle.skinTextures != null) {
                haut = quelle.skinTextures.body().texturePath();
            }
            if (haut == null) return;

            // Minecraft legt vor jedem Entity-Modell eine Spiegelung und
            // eine Verschiebung an; beim direkten Einreichen faellt das
            // sonst weg, und der Kleine steht kopfueber am Boden statt
            // dort, wohin die Matrix zeigt.
            matrizen.push();
            try {
                matrizen.scale(-1.0f, -1.0f, 1.0f);
                matrizen.translate(0.0f, -1.501f, 0.0f);
                schlange.submitModel(m, EIGEN, matrizen, m.getLayer(haut), licht,
                        OverlayTexture.DEFAULT_UV, 0, null);
            } finally {
                matrizen.pop();
            }
        } catch (Throwable t) {
            // Lieber kein Kleiner als ein zerrissenes Bild
        }
    }

    /**
     * Unsere eigene Haltung. Sitzend streckt er die Beine nach vorn und legt
     * die Arme dazwischen; stehend laeuft er mit, wenn er hinterherlaeuft.
     */
    private static void haltung(PlayerEntityModel m, PlayerEntityRenderState quelle) {
        VConfig c = VConfig.get();
        boolean sitzt = c.miniMeSitzt && c.miniMePos != 3;

        if (sitzt) {
            // Beine waagerecht nach vorn
            m.rightLeg.pitch = -1.55f;
            m.leftLeg.pitch = -1.55f;
            m.rightLeg.yaw = 0.12f;
            m.leftLeg.yaw = -0.12f;
            m.rightLeg.roll = 0.0f;
            m.leftLeg.roll = 0.0f;
            // Arme zwischen den Beinen, leicht nach vorn abgestuetzt
            m.rightArm.pitch = -0.55f;
            m.leftArm.pitch = -0.55f;
            m.rightArm.yaw = 0.0f;
            m.leftArm.yaw = 0.0f;
            m.rightArm.roll = 0.22f;
            m.leftArm.roll = -0.22f;
            // Kopf leicht geneigt, das macht ihn freundlicher
            m.head.pitch = 0.14f;
            m.head.yaw = 0.0f;
            m.head.roll = 0.10f;
        } else {
            // Hinterherlaufen: die Beine schwingen mit dem eigenen Schritt
            float schritt = quelle.limbSwingAnimationProgress;
            float staerke = Math.min(0.6f, quelle.limbSwingAmplitude);
            m.rightLeg.pitch = (float) Math.cos(schritt * 0.6662f) * 1.4f * staerke;
            m.leftLeg.pitch = (float) Math.cos(schritt * 0.6662f + Math.PI) * 1.4f * staerke;
            m.rightArm.pitch = (float) Math.cos(schritt * 0.6662f + Math.PI) * 1.4f * staerke;
            m.leftArm.pitch = (float) Math.cos(schritt * 0.6662f) * 1.4f * staerke;
            m.head.pitch = 0.0f;
            m.head.yaw = 0.0f;
        }
        m.hat.setAngles(m.head.pitch, m.head.yaw, m.head.roll);
    }
}
