package dev.visual.fabric;

/** Small continuous, allocation-free animation curves; time is in Minecraft ticks. */
public final class MiniAnimation {
    private MiniAnimation() { }
    public static float headPitch(float age) { return 0.10f + (float) Math.sin(age * 0.075f) * 0.07f; }
    public static float headRoll(float age) { return (float) Math.sin(age * 0.045f) * 0.12f; }
    public static float headYaw(float age) { return (float) Math.sin(age * 0.033f) * 0.16f; }
    public static float legSwing(float age) { return (float) Math.sin(age * 0.14f) * 0.10f; }
    /** A short greeting every eleven seconds, with a smooth approach and release. */
    public static float greeting(float age) {
        float phase = ((age % 220f) + 220f) % 220f;
        if (phase >= 44f) return 0f;
        float envelope = (float) Math.sin(Math.PI * phase / 44f);
        return envelope * envelope;
    }
    public static float hop(float step, float movement) {
        return (float) Math.pow(Math.sin(step * 0.6662f), 2) * Math.min(1f, Math.max(0f, movement) / 0.6f) * 0.09f;
    }
    /** Vanilla applies this rotation before reflecting the model's coordinate axes. */
    public static float modelYaw(float bodyYaw) { return 180f - bodyYaw; }
}
