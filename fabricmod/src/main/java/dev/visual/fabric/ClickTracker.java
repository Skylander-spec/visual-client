package dev.visual.fabric;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CPS-Zähler: pollt die Maustasten jeden Client-Tick per GLFW und zählt
 * Klick-Flanken der letzten Sekunde. Mixin-frei.
 */
public final class ClickTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static boolean wasDown = false;

    private ClickTracker() {
    }

    public static void tick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            LEFT.addLast(System.currentTimeMillis());
        }
        wasDown = down;
    }

    public static int left() {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!LEFT.isEmpty() && LEFT.peekFirst() < cutoff) LEFT.pollFirst();
        return LEFT.size();
    }
}
