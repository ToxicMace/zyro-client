package dev.zyro.client.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/**
 * Counts clicks per second by watching mouse button edges every rendered frame.
 * No mixins needed, and at any playable framerate a click is never missed.
 */
public final class ClickTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean leftDown;
    private static boolean rightDown;

    private ClickTracker() {}

    public static void sample(Minecraft mc) {
        long window = mc.getWindow().handle();
        long now = System.currentTimeMillis();
        boolean inGame = mc.screen == null;

        boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (inGame && l && !leftDown) LEFT.addLast(now);
        if (inGame && r && !rightDown) RIGHT.addLast(now);
        leftDown = l;
        rightDown = r;

        prune(LEFT, now);
        prune(RIGHT, now);
    }

    private static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) {
            q.removeFirst();
        }
    }

    public static int left() {
        return LEFT.size();
    }

    public static int right() {
        return RIGHT.size();
    }

    public static boolean leftDown() {
        return leftDown;
    }

    public static boolean rightDown() {
        return rightDown;
    }
}
