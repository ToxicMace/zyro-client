package dev.zyro.hud.element;

import dev.zyro.hud.render.Ctx;

import java.util.List;

/**
 * Lines of label/value pairs. The label is drawn in the accent color and the value in the
 * text color, e.g. "144" + " FPS" or "X" + " 128".
 */
public abstract class TextElement extends HudElement {
    protected TextElement(String id, String name) {
        super(id, name);
    }

    public record Line(String value, String label, boolean labelFirst) {
        public static Line suffix(String value, String label) {
            return new Line(value, label, false);
        }

        public static Line prefix(String label, String value) {
            return new Line(value, label, true);
        }
    }

    protected abstract List<Line> lines(Ctx ctx);

    private int lineWidth(Ctx ctx, Line line) {
        return ctx.width(line.value()) + (line.label().isEmpty() ? 0 : ctx.width(line.label()) + ctx.width(" "));
    }

    @Override
    public int[] measure(Ctx ctx) {
        List<Line> lines = lines(ctx);
        int p = ctx.cfg.padding;
        int w = 0;
        for (Line line : lines) {
            w = Math.max(w, lineWidth(ctx, line));
        }
        int h = lines.size() * 10 - 1;
        return new int[] {w + p * 2 + barInset(ctx), h + p * 2};
    }

    @Override
    public void draw(Ctx ctx, int w, int h) {
        int p = ctx.cfg.padding;
        int x0 = p + barInset(ctx);
        int y = p;
        int space = ctx.width(" ");
        for (Line line : lines(ctx)) {
            int x = x0;
            if (line.label().isEmpty()) {
                ctx.text(line.value(), x, y, ctx.text);
            } else if (line.labelFirst()) {
                ctx.text(line.label(), x, y, ctx.accent);
                x += ctx.width(line.label()) + space;
                ctx.text(line.value(), x, y, ctx.text);
            } else {
                ctx.text(line.value(), x, y, ctx.text);
                x += ctx.width(line.value()) + space;
                ctx.text(line.label(), x, y, ctx.accent);
            }
            y += 10;
        }
    }
}
