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
    private static final java.util.Map<PlayerEntityRenderState, MiniState> STATES = new java.util.WeakHashMap<>();
    private static final PlayerEntityModel[] MODELS = new PlayerEntityModel[2];
    private static final class MiniState extends PlayerEntityRenderState {
        boolean seated;
        float step, movement;
    }
    private MiniPuppe() { }

    private static PlayerEntityModel modell(boolean slim) {
        int index = slim ? 1 : 0;
        if (MODELS[index] == null) {
            MODELS[index] = new PlayerEntityModel(TexturedModelData
                    .of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, slim), 64, 64)
                    .createModel(), slim) {
                @Override public void setAngles(PlayerEntityRenderState state) {
                    super.setAngles(state);
                    if (state instanceof MiniState mini) haltung(this, mini);
                }
            };
        }
        return MODELS[index];
    }

    /** Zeichnet den Kleinen an der Stelle, auf die die Matrix schon zeigt. */
    public static void zeichnen(PlayerEntityRenderState quelle, MatrixStack matrizen,
                                OrderedRenderCommandQueue schlange, int licht) {
        try {
            boolean schlank = quelle.skinTextures != null
                    && quelle.skinTextures.model() == net.minecraft.entity.player.PlayerSkinType.SLIM;
            PlayerEntityModel m = modell(schlank);

            // Deferred rendering needs a separate pose snapshot for every player.
            MiniState EIGEN = STATES.computeIfAbsent(quelle, key -> new MiniState());
            EIGEN.seated = dev.visual.fabric.MiniMe.sitzt();
            EIGEN.step = dev.visual.fabric.MiniMe.schritt();
            EIGEN.movement = dev.visual.fabric.MiniMe.bewegung();
            EIGEN.age = quelle.age;
            EIGEN.skinTextures = quelle.skinTextures;
            EIGEN.hatVisible = quelle.hatVisible;
            EIGEN.jacketVisible = quelle.jacketVisible;
            EIGEN.leftSleeveVisible = quelle.leftSleeveVisible;
            EIGEN.rightSleeveVisible = quelle.rightSleeveVisible;
            EIGEN.leftPantsLegVisible = quelle.leftPantsLegVisible;
            EIGEN.rightPantsLegVisible = quelle.rightPantsLegVisible;
            EIGEN.baseScale = 1.0f;


            Identifier haut = dev.visual.fabric.MiniMe.skin();
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
                matrizen.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(
                        dev.visual.fabric.MiniAnimation.modelYaw(quelle.bodyYaw)));
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
    private static void haltung(PlayerEntityModel m, MiniState state) {
        MiniPose.apply(m, state.seated, state.age, state.step, state.movement);
    }
}
