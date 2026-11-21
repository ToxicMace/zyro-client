package dev.zyro.client.render;

import net.minecraft.client.gui.GuiGraphics;

public final class Draw {
    private Draw() {}

    /** Filled rect with optional 1px "soft" corners, which reads as rounded at HUD scale. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int color, boolean rounded) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        if (!rounded || w < 3 || h < 3) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
