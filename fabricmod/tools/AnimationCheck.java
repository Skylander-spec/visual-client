import dev.visual.fabric.MiniAnimation;

/** Standalone regression check: javac MiniAnimation.java AnimationCheck.java, then java AnimationCheck. */
public final class AnimationCheck {
    private static void near(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.00001) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        // A model looks down -Z; after vanilla's rotation it must face the player's compass direction.
        float[] yaw = {0, 90, 180, 270, -90, 450};
        double[][] expected = {{0,1}, {-1,0}, {0,-1}, {1,0}, {1,0}, {-1,0}};
        for (int i = 0; i < yaw.length; i++) {
            double angle = Math.toRadians(MiniAnimation.modelYaw(yaw[i]));
            near(-Math.sin(angle), expected[i][0]);
            near(-Math.cos(angle), expected[i][1]);
        }
        float previousWave = 0;
        for (int i = 0; i < 100_000; i++) {
            float time = i * 0.1f;
            float wave = MiniAnimation.greeting(time);
            if (!Float.isFinite(wave) || wave < 0 || wave > 1 || Math.abs(wave - previousWave) > 0.01)
                throw new AssertionError("Greeting jumps or leaves its range at " + time);
            previousWave = wave;
            near(MiniAnimation.hop(time, 0), 0);
            float hop = MiniAnimation.hop(time, 0.6f);
            if (hop < 0 || hop > 0.090001f) throw new AssertionError("Follower penetrates the ground or jumps too high");
        }
        near(MiniAnimation.greeting(44), 0);
        near(MiniAnimation.greeting(220), 0);
        System.out.println("PASS: compass headings, stationary feet, bounded hops and continuous greetings");
    }
}
