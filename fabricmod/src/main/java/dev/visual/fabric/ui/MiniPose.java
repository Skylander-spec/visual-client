package dev.visual.fabric.ui;

import net.minecraft.client.render.entity.model.PlayerEntityModel;

/** One pose implementation for every supported modern renderer. */
public final class MiniPose {
    private MiniPose() { }
    public static void apply(PlayerEntityModel m, boolean sitzt, float age, float schritt, float staerke) {

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
            m.rightLeg.pitch = (float) Math.cos(schritt * 0.6662f) * 1.4f * staerke;
            m.leftLeg.pitch = (float) Math.cos(schritt * 0.6662f + Math.PI) * 1.4f * staerke;
            m.rightArm.pitch = (float) Math.cos(schritt * 0.6662f + Math.PI) * 1.4f * staerke;
            m.leftArm.pitch = (float) Math.cos(schritt * 0.6662f) * 1.4f * staerke;
            m.head.pitch = 0.0f;
            m.head.yaw = 0.0f;
        }
        m.head.pitch += dev.visual.fabric.MiniAnimation.headPitch(age);
        m.head.yaw = dev.visual.fabric.MiniAnimation.headYaw(age);
        m.head.roll = dev.visual.fabric.MiniAnimation.headRoll(age);
        if (sitzt) {
            float swing = dev.visual.fabric.MiniAnimation.legSwing(age);
            m.rightLeg.pitch += swing;
            m.leftLeg.pitch -= swing;
        }
        float wave = dev.visual.fabric.MiniAnimation.greeting(age) * (1f - Math.min(1f, staerke / 0.3f));
        m.rightArm.pitch += (-2.65f - m.rightArm.pitch) * wave;
        m.rightArm.roll += (-0.35f + (float) Math.sin(age * 0.65f) * 0.30f) * wave;
        m.hat.setAngles(m.head.pitch, m.head.yaw, m.head.roll);
    }
}
