package dev.zyro.hud.element;

import dev.zyro.hud.render.ClickTracker;
import dev.zyro.hud.render.Colors;
import dev.zyro.hud.render.Ctx;
import dev.zyro.hud.render.Draw;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

/** WASD + mouse buttons + space, with smooth press fades. */
public final class KeystrokesElement extends HudElement {
    private static final int KEY = 22;
    private static final int GAP = 2;
    /** Press animation, 0..1, per key: W A S D LMB RMB SPACE */
    private final float[] anim = new float[7];

    public KeystrokesElement() {
        super("keystrokes", "Keystrokes");
    }

    @Override
    public int[] measure(Ctx ctx) {
        int w = KEY * 3 + GAP * 2;
        int h = KEY * 2 + GAP + GAP + 18 + GAP + 10;
        return new int[] {w, h};
    }

    private static boolean down(KeyMapping key) {
        return key.isDown();
    }

    @Override
    public void draw(Ctx ctx, int w, int h) {
        Options o = ctx.mc.options;
        boolean[] pressed = {
                down(o.keyUp), down(o.keyLeft), down(o.keyDown), down(o.keyRight),
                ClickTracker.leftDown() && ctx.mc.screen == null,
                ClickTracker.rightDown() && ctx.mc.screen == null,
                down(o.keyJump),
        };
        float step = Math.min(1f, ctx.dt * 16f);
        for (int i = 0; i < anim.length; i++) {
            anim[i] += ((pressed[i] ? 1f : 0f) - anim[i]) * step;
        }

        int col1 = 0, col2 = KEY + GAP, col3 = (KEY + GAP) * 2;
        key(ctx, col2, 0, KEY, KEY, "W", anim[0]);
        key(ctx, col1, KEY + GAP, KEY, KEY, "A", anim[1]);
        key(ctx, col2, KEY + GAP, KEY, KEY, "S", anim[2]);
        key(ctx, col3, KEY + GAP, KEY, KEY, "D", anim[3]);

        int rowY = (KEY + GAP) * 2;
        int half = (w - GAP) / 2;
        mouse(ctx, 0, rowY, half, 18, "LMB", ClickTracker.left(), anim[4]);
        mouse(ctx, half + GAP, rowY, w - half - GAP, 18, "RMB", ClickTracker.right(), anim[5]);

        int spaceY = rowY + 18 + GAP;
        box(ctx, 0, spaceY, w, 10, anim[6]);
        int bar = Colors.lerp(ctx.text, Colors.onColor(ctx.accent), anim[6]);
        ctx.g.fill(w / 2 - 12, spaceY + 4, w / 2 + 12, spaceY + 5, Colors.opaque(bar));
    }

    private void box(Ctx ctx, int x, int y, int w, int h, float t) {
        int idle = ctx.ec.background ? ctx.cfg.backgroundArgb() : 0x00000000;
        int active = Colors.withAlpha(ctx.accent, 225);
        Draw.panel(ctx.g, x, y, w, h, Colors.lerp(idle, active, t), ctx.cfg.roundedCorners);
    }

    private void key(Ctx ctx, int x, int y, int w, int h, String label, float t) {
        box(ctx, x, y, w, h, t);
        int color = Colors.lerp(ctx.text, Colors.onColor(ctx.accent), t);
        ctx.text(label, x + (w - ctx.width(label)) / 2 + 1, y + (h - 8) / 2, Colors.opaque(color));
    }

    private void mouse(Ctx ctx, int x, int y, int w, int h, String label, int cps, float t) {
        box(ctx, x, y, w, h, t);
        int color = Colors.opaque(Colors.lerp(ctx.text, Colors.onColor(ctx.accent), t));
        String s = cps > 0 ? cps + " CPS" : label;
        ctx.text(s, x + (w - ctx.width(s)) / 2 + 1, y + (h - 8) / 2, color);
    }
}
