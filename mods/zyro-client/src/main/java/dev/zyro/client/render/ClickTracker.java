package dev.zyro.client.render;

import dev.zyro.client.compat.Compat;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;

/**
 * Counts clicks per second from the attack/use key states, sampled every rendered frame.
 * Works the same on GLFW (1.21.x) and SDL (26.x) builds, and follows the player's key binds.
 */
public final class ClickTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean leftDown;
    private static boolean rightDown;

    private ClickTracker() {}

    public static void sample(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean inGame = Compat.screen(mc) == null;

        boolean l = mc.options.keyAttack.isDown();
        boolean r = mc.options.keyUse.isDown();
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
