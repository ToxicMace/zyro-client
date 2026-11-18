package dev.zyro.hud.element;

import dev.zyro.hud.render.Ctx;

public abstract class HudElement {
    public final String id;
    public final String name;

    protected HudElement(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Unscaled size in GUI pixels, including padding. */
    public abstract int[] measure(Ctx ctx);

    /** Draws the content at the local origin; the panel background is already drawn. */
    public abstract void draw(Ctx ctx, int w, int h);

    /** Elements without data (e.g. armor with nothing equipped) can hide outside the editor. */
    public boolean hasContent(Ctx ctx) {
        return true;
    }

    /** Extra left inset taken by the accent bar. */
    protected static int barInset(Ctx ctx) {
        return ctx.cfg.accentBar && ctx.ec.background ? 3 : 0;
    }
}
