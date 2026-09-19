package dev.visual.visualsmod.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Zählt Klicks der letzten Sekunde (links/rechts) für die CPS-Anzeige. */
public class CpsTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != InputConstants.PRESS) return;
        long now = System.currentTimeMillis();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT.addLast(now);
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT.addLast(now);
    }

    private static int count(Deque<Long> deque) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) deque.pollFirst();
        return deque.size();
    }

    public static int leftCps() {
        return count(LEFT);
    }

    public static int rightCps() {
        return count(RIGHT);
    }
}
