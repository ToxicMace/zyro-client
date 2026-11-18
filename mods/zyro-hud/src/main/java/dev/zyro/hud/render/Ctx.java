package dev.zyro.hud.render;

import dev.zyro.hud.config.ElementConfig;
import dev.zyro.hud.config.HudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Everything an element needs for one frame, with its colors already resolved. */
public final class Ctx {
    public final GuiGraphics g;
    public final Minecraft mc;
    public final Font font;
    public final HudConfig cfg;
    public final ElementConfig ec;
    public final int text;
    public final int accent;
    /** true inside the editor: elements show sample data when the real value is missing. */
    public final boolean preview;
    public final float dt;

    public Ctx(GuiGraphics g, Minecraft mc, HudConfig cfg, ElementConfig ec, int index, boolean preview, float dt) {
        this.g = g;
        this.mc = mc;
        this.font = mc.font;
        this.cfg = cfg;
        this.ec = ec;
        this.preview = preview;
        this.dt = dt;
        boolean chroma = cfg.chroma || ec.chroma;
        int baseAccent = ec.accentColor != null ? ec.accentColor : cfg.accent;
        this.accent = Colors.opaque(chroma ? Colors.chroma(cfg.chromaSpeed, index * 0.08f) : baseAccent);
        this.text = Colors.opaque(ec.textColor != null ? ec.textColor : cfg.text);
    }

    public void text(String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, cfg.textShadow);
    }

    public int width(String s) {
        return font.width(s);
    }
}
