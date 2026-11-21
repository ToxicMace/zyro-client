package dev.zyro.client.module;

import dev.zyro.client.ZyroClient;
import net.minecraft.client.Minecraft;

/** Hold-to-zoom with an eased transition and optional cinematic camera while zoomed. */
public final class Zoom {
    private static float current = 1f;
    private static float previous = 1f;
    private static boolean zooming;
    private static boolean savedSmooth;

    private Zoom() {}

    public static void tick(Minecraft mc) {
        boolean want = ZyroClient.CONFIG.zoomEnabled && dev.zyro.client.compat.Compat.screen(mc) == null && ZyroClient.zoomKey.isDown();
        if (want != zooming) {
            zooming = want;
            if (ZyroClient.CONFIG.zoomSmoothCamera) {
                if (want) {
                    savedSmooth = mc.options.smoothCamera;
                    mc.options.smoothCamera = true;
                } else {
                    mc.options.smoothCamera = savedSmooth;
                }
            }
        }
        previous = current;
        float target = zooming ? Math.clamp(ZyroClient.CONFIG.zoomFactor, 0.05f, 0.9f) : 1f;
        current += (target - current) * 0.45f;
        if (Math.abs(current - target) < 0.001f) {
            current = target;
        }
    }

    /** FOV multiplier for this frame, interpolated between ticks so the zoom stays smooth. */
    public static float factor(float partialTick) {
        return previous + (current - previous) * partialTick;
    }

    public static float factor() {
        return current;
    }
}
