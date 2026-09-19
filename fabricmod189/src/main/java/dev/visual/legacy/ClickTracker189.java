package dev.visual.legacy;

import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Klicks pro Sekunde. 1.8.9 nutzt LWJGL 2 direkt, deshalb reicht das
 * Abfragen der Maustasten — kein Mixin noetig.
 *
 * Gezaehlt werden Flanken (nicht gedrueckt -> gedrueckt) in einem
 * Ein-Sekunden-Fenster.
 */
public final class ClickTracker189 {
    private static final Deque<Long> LEFT = new ArrayDeque<Long>();
    private static final Deque<Long> RIGHT = new ArrayDeque<Long>();
    private static boolean leftWas;
    private static boolean rightWas;

    private ClickTracker189() {
    }

    /** Jeden Client-Tick aufrufen. */
    public static void tick() {
        boolean l = Mouse.isButtonDown(0);
        boolean r = Mouse.isButtonDown(1);
        long now = System.currentTimeMillis();
        if (l && !leftWas) LEFT.add(now);
        if (r && !rightWas) RIGHT.add(now);
        leftWas = l;
        rightWas = r;
        prune(LEFT, now);
        prune(RIGHT, now);
    }

    private static void prune(Deque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) {
            q.pollFirst();
        }
    }

    public static int left() {
        prune(LEFT, System.currentTimeMillis());
        return LEFT.size();
    }

    public static int right() {
        prune(RIGHT, System.currentTimeMillis());
        return RIGHT.size();
    }
}
