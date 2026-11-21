package dev.zyro.client.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Version-neutral drawing surface. 1.21.11: wraps GuiGraphics. */
public final class Gfx {
    private final GuiGraphics g;

    public Gfx(GuiGraphics g) {
        this.g = g;
    }

    public int width() { return g.guiWidth(); }
    public int height() { return g.guiHeight(); }

    public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }

    public void text(Font font, String s, int x, int y, int argb, boolean shadow) {
        g.drawString(font, s, x, y, argb, shadow);
    }

    /** Draws a whole texture scaled into the given rect. */
    public void blit(Identifier texture, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h);
    }

    public void item(ItemStack stack, int x, int y) { g.renderItem(stack, x, y); }

    public void push() { g.pose().pushMatrix(); }
    public void pop() { g.pose().popMatrix(); }
    public void translate(float x, float y) { g.pose().translate(x, y); }
    public void scale(float x, float y) { g.pose().scale(x, y); }

    public void scissor(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }
    public void noScissor() { g.disableScissor(); }
}
