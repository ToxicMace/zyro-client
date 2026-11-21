package dev.zyro.client.render;

import dev.zyro.client.ZyroClient;
import dev.zyro.client.config.ElementConfig;
import dev.zyro.client.config.HudConfig;
import dev.zyro.client.element.Elements;
import dev.zyro.client.element.HudElement;
import dev.zyro.client.element.KeystrokesElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    /** Where an element ended up on screen this frame (scaled size), for editor hit tests. */
    public record Placed(HudElement element, ElementConfig config, int x, int y, int w, int h) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<HudElement> elements = Elements.all();
    private long lastNanos = System.nanoTime();

    public List<HudElement> elements() {
        return elements;
    }

    public List<Placed> render(GuiGraphics g, boolean editor) {
        Minecraft mc = Minecraft.getInstance();
        HudConfig cfg = ZyroClient.CONFIG;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        List<Placed> placed = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            HudElement el = elements.get(i);
            ElementConfig ec = cfg.element(el.id);
            if (!ec.enabled && !editor) {
                continue;
            }
            Ctx ctx = new Ctx(g, mc, cfg, ec, i, editor, dt);
            if (!editor && !el.hasContent(ctx)) {
                continue;
            }

            int[] size = el.measure(ctx);
            float scale = Math.clamp(ec.scale, 0.5f, 3f);
            int sw = Math.round(size[0] * scale);
            int sh = Math.round(size[1] * scale);
            int x = Math.clamp(Math.round((float) ec.x * screenW), 0, Math.max(0, screenW - sw));
            int y = Math.clamp(Math.round((float) ec.y * screenH), 0, Math.max(0, screenH - sh));

            g.pose().pushMatrix();
            g.pose().translate((float) x, (float) y);
            g.pose().scale(scale, scale);
            if (ec.background && !(el instanceof KeystrokesElement)) {
                Draw.panel(g, 0, 0, size[0], size[1], cfg.backgroundArgb(), cfg.roundedCorners);
                if (cfg.accentBar) {
                    g.fill(0, 2, 2, size[1] - 2, ctx.accent);
                }
            }
            el.draw(ctx, size[0], size[1]);
            g.pose().popMatrix();

            if (editor && !ec.enabled) {
                g.fill(x, y, x + sw, y + sh, 0xB0000000);
            }
            placed.add(new Placed(el, ec, x, y, sw, sh));
        }
        return placed;
    }
}
