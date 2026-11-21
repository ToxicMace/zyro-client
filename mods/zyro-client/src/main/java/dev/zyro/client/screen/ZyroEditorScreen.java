package dev.zyro.client.screen;

import dev.zyro.client.ZyroClient;
import dev.zyro.client.config.ElementConfig;
import dev.zyro.client.config.HudConfig;
import dev.zyro.client.element.HudElement;
import dev.zyro.client.render.Colors;
import dev.zyro.client.render.Draw;
import dev.zyro.client.render.HudRenderer.Placed;
import dev.zyro.client.compat.Gfx;
import net.minecraft.client.gui.screens.Screen;
import dev.zyro.client.compat.ZyroScreenBase;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Right Shift editor: drag elements, scroll to resize, and theme everything from a side panel.
 * The panel is a tiny immediate-mode UI: every frame draws controls and records their hit boxes.
 */
public final class ZyroEditorScreen extends ZyroScreenBase {
    private static final int PANEL_W = 168;
    private static final int ROW = 13;
    private static final int SNAP = 5;

    private static final int[] ACCENTS = {
            0xFF5B7CFF, 0xFF22D3EE, 0xFF8B5CF6, 0xFF2DD4BF, 0xFF4ADE80,
            0xFFFACC15, 0xFFFF6B3D, 0xFFF43F5E, 0xFFF472B6, 0xFFE2E8F0,
    };
    private static final int[] TEXTS = {
            0xFFFFFFFF, 0xFFE6EAFF, 0xFFDDF4EE, 0xFFFBE9E2, 0xFFF7E6F1, 0xFFB8C0D6, 0xFF141A2E,
    };
    private static final int[] BACKGROUNDS = {
            0xFF0B0E17, 0xFF000000, 0xFF081114, 0xFF120D0C, 0xFF140F15, 0xFF1F2937, 0xFFF4F6FB,
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private List<Placed> placed = List.of();

    private String selected;
    private String dragging;
    private double dragOffX;
    private double dragOffY;
    private boolean snappedX;
    private boolean snappedY;
    private int snapLineX = -1;
    private int snapLineY = -1;

    private int panelScroll;
    private int panelContentH;
    private int mouseX;
    private int mouseY;
    private float appear;

    public ZyroEditorScreen(Screen parent) {
        super(Component.literal("Zyro Settings"));
        this.parent = parent;
    }

    private HudConfig cfg() {
        return ZyroClient.CONFIG;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        cfg().save();
        dev.zyro.client.compat.Compat.setScreen(minecraft, parent);
    }

    @Override
    protected boolean drawCustomBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible: a soft dim instead of the vanilla blur.
        if (minecraft.level == null) {
            return false; // on the main menu keep the panorama
        }
        g.fill(0, 0, width, height, 0x66000000);
        return true;
    }

    @Override
    protected void drawScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        appear = Math.min(1f, appear + 0.12f);

        // subtle grid so alignment is easy to judge
        int grid = 0x0DFFFFFF;
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, grid);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, grid);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);

        placed = ZyroClient.RENDERER.render(g, true);

        int accent = Colors.opaque(cfg().accent);
        for (Placed p : placed) {
            boolean isSel = p.element().id.equals(selected);
            boolean hover = p.contains(mouseX, mouseY) && !overPanel(mouseX);
            if (isSel || hover) {
                Draw.outline(g, p.x() - 2, p.y() - 2, p.w() + 4, p.h() + 4,
                        isSel ? accent : Colors.withAlpha(0xFFFFFF, 110));
            }
            if (hover && dragging == null) {
                String tip = p.element().name + "  " + Math.round(p.config().scale * 100) + "%";
                int tw = font.width(tip) + 8;
                int ty = p.y() - 14 < 0 ? p.y() + p.h() + 4 : p.y() - 14;
                Draw.panel(g, p.x(), ty, tw, 11, 0xE0101420, true);
                g.text(font, tip, p.x() + 4, ty + 2, 0xFFFFFFFF, false);
            }
        }

        if (dragging != null) {
            if (snapLineX >= 0) g.fill(snapLineX, 0, snapLineX + 1, height, accent);
            if (snapLineY >= 0) g.fill(0, snapLineY, width, snapLineY + 1, accent);
        }

        drawPanel(g);
    }

    // ---------------------------------------------------------------- panel

    private int panelX() {
        return width - PANEL_W - 8 + Math.round((1f - easeOut(appear)) * 40);
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private boolean overPanel(double mx) {
        return mx >= panelX() - 4;
    }

    private void drawPanel(Gfx g) {
        hits.clear();
        int px = panelX();
        int top = 8;
        int bottom = height - 8;
        Draw.panel(g, px, top, PANEL_W, bottom - top, 0xEE0D111B, true);
        Draw.outline(g, px, top, PANEL_W, bottom - top, 0x1FFFFFFF);
        g.fill(px + 1, top + 1, px + PANEL_W - 1, top + 2, Colors.opaque(cfg().accent));

        g.scissor(px + 1, top + 2, px + PANEL_W - 1, bottom - 1);
        int x = px + 8;
        int w = PANEL_W - 16;
        int y = top + 8 - panelScroll;
        int start = y;

        g.text(font, "ZYRO", x, y, Colors.opaque(cfg().accent), false);
        g.text(font, "SETTINGS", x + font.width("ZYRO "), y, 0xFFE6EAFF, false);
        y += 11;
        g.text(font, "Drag to move, scroll to resize", x, y, 0xFF7D8499, false);
        y += 16;

        if (selected == null) {
            y = globalControls(g, x, y, w);
        } else {
            y = elementControls(g, x, y, w, cfg().element(selected));
        }
        panelContentH = y - start + 8;
        g.noScissor();
    }

    private int section(Gfx g, String title, int x, int y) {
        g.text(font, title.toUpperCase(), x, y, 0xFF7D8499, false);
        return y + 11;
    }

    private int globalControls(Gfx g, int x, int y, int w) {
        HudConfig c = cfg();

        y = section(g, "Theme", x, y);
        HudConfig.Preset[] presets = HudConfig.Preset.values();
        int chipW = (w - 4) / 3;
        for (int i = 0; i < presets.length; i++) {
            HudConfig.Preset p = presets[i];
            int cx = x + (i % 3) * (chipW + 2);
            int cy = y + (i / 3) * (ROW + 2);
            boolean active = p.id.equals(c.preset);
            chip(g, cx, cy, chipW, ROW, p.label, active, p.accent, () -> c.applyPreset(p));
        }
        y += ((presets.length + 2) / 3) * (ROW + 2) + 6;

        y = section(g, "Accent", x, y);
        y = swatches(g, x, y, w, ACCENTS, c.accent, v -> { c.accent = v; c.preset = "custom"; }, null);
        y = section(g, "Text", x, y);
        y = swatches(g, x, y, w, TEXTS, c.text, v -> { c.text = v; c.preset = "custom"; }, null);
        y = section(g, "Background", x, y);
        y = swatches(g, x, y, w, BACKGROUNDS, c.background | 0xFF000000,
                v -> { c.background = v & 0xFFFFFF; c.preset = "custom"; }, null);
        y = stepper(g, x, y, w, "Opacity", Math.round(c.backgroundOpacity / 2.55f) + "%",
                () -> c.backgroundOpacity = Math.max(0, c.backgroundOpacity - 25),
                () -> c.backgroundOpacity = Math.min(255, c.backgroundOpacity + 25));
        y += 4;

        y = section(g, "Style", x, y);
        y = toggle(g, x, y, w, "HUD enabled", c.enabled, v -> c.enabled = v);
        y = toggle(g, x, y, w, "Accent bar", c.accentBar, v -> c.accentBar = v);
        y = toggle(g, x, y, w, "Rounded corners", c.roundedCorners, v -> c.roundedCorners = v);
        y = toggle(g, x, y, w, "Text shadow", c.textShadow, v -> c.textShadow = v);
        y = toggle(g, x, y, w, "Chroma (all)", c.chroma, v -> c.chroma = v);
        y = stepper(g, x, y, w, "Padding", c.padding + "px",
                () -> c.padding = Math.max(1, c.padding - 1),
                () -> c.padding = Math.min(8, c.padding + 1));
        y += 4;

        y = section(g, "Modules", x, y);
        y = toggle(g, x, y, w, "Zyro main menu", c.customMenu, v -> c.customMenu = v);
        y = toggle(g, x, y, w, "Zoom (hold C)", c.zoomEnabled, v -> c.zoomEnabled = v);
        y = toggle(g, x, y, w, "Cinematic zoom", c.zoomSmoothCamera, v -> c.zoomSmoothCamera = v);
        y = stepper(g, x, y, w, "Zoom", Math.round(1f / c.zoomFactor) + "x",
                () -> c.zoomFactor = Math.min(0.5f, c.zoomFactor * 1.25f),
                () -> c.zoomFactor = Math.max(0.1f, c.zoomFactor / 1.25f));
        y = toggle(g, x, y, w, "Toggle sprint", c.toggleSprint, v -> {
            c.toggleSprint = v;
            ZyroClient.applyVanillaModules(minecraft);
        });
        y += 4;

        y = section(g, "Elements", x, y);
        for (HudElement el : ZyroClient.RENDERER.elements()) {
            ElementConfig ec = c.element(el.id);
            int rowY = y;
            boolean hover = inside(x, rowY, w - 26, ROW);
            if (hover) Draw.panel(g, x - 2, rowY, w - 24, ROW, 0x14FFFFFF, true);
            g.text(font, el.name, x + 2, rowY + 3, ec.enabled ? 0xFFE6EAFF : 0xFF5A6075, false);
            g.text(font, "›", x + w - 34, rowY + 3, 0xFF7D8499, false);
            hits.add(new Hit(x, rowY, w - 26, ROW, () -> selected = el.id));
            pill(g, x + w - 20, rowY + 2, ec.enabled, () -> ec.enabled = !ec.enabled);
            y += ROW + 1;
        }
        y += 6;
        y = button(g, x, y, w, "Reset layout", c::resetLayout);
        return y;
    }

    private int elementControls(Gfx g, int x, int y, int w, ElementConfig ec) {
        HudElement el = ZyroClient.RENDERER.elements().stream().filter(e -> e.id.equals(selected)).findFirst().orElse(null);
        if (el == null) {
            selected = null;
            return y;
        }
        y = button(g, x, y, w, "‹  All settings", () -> selected = null);
        y += 4;
        g.text(font, el.name, x, y, 0xFFFFFFFF, false);
        y += 14;

        y = toggle(g, x, y, w, "Visible", ec.enabled, v -> ec.enabled = v);
        y = toggle(g, x, y, w, "Background", ec.background, v -> ec.background = v);
        y = toggle(g, x, y, w, "Chroma", ec.chroma, v -> ec.chroma = v);
        y = stepper(g, x, y, w, "Scale", Math.round(ec.scale * 100) + "%",
                () -> ec.scale = Math.max(0.5f, Math.round((ec.scale - 0.1f) * 10) / 10f),
                () -> ec.scale = Math.min(3f, Math.round((ec.scale + 0.1f) * 10) / 10f));
        y += 4;
        y = section(g, "Accent", x, y);
        y = swatches(g, x, y, w, ACCENTS, ec.accentColor == null ? 0 : ec.accentColor,
                v -> ec.accentColor = v, () -> ec.accentColor = null);
        y = section(g, "Text", x, y);
        y = swatches(g, x, y, w, TEXTS, ec.textColor == null ? 0 : ec.textColor,
                v -> ec.textColor = v, () -> ec.textColor = null);
        y += 4;
        y = button(g, x, y, w, "Reset position", () -> {
            ElementConfig d = ElementConfig.defaultsFor(el.id);
            ec.x = d.x;
            ec.y = d.y;
            ec.scale = d.scale;
        });
        return y;
    }

    // ---------------------------------------------------------------- controls

    private boolean inside(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void chip(Gfx g, int x, int y, int w, int h, String label, boolean active, int color, Runnable action) {
        boolean hover = inside(x, y, w, h);
        int bg = active ? Colors.withAlpha(color, 60) : (hover ? 0x22FFFFFF : 0x12FFFFFF);
        Draw.panel(g, x, y, w, h, bg, true);
        if (active) Draw.outline(g, x, y, w, h, Colors.opaque(color));
        g.fill(x + 4, y + h / 2 - 2, x + 8, y + h / 2 + 2, Colors.opaque(color));
        g.text(font, label, x + 11, y + 3, active ? 0xFFFFFFFF : 0xFFC8CEE0, false);
        hits.add(new Hit(x, y, w, h, action));
    }

    private int swatches(Gfx g, int x, int y, int w, int[] colors, int current, IntConsumer pick, Runnable auto) {
        int size = 12;
        int gap = 2;
        int cx = x;
        if (auto != null) {
            boolean sel = current == 0;
            Draw.panel(g, cx, y, 26, size, sel ? 0x40FFFFFF : 0x14FFFFFF, true);
            g.text(font, "Auto", cx + 3, y + 2, 0xFFE6EAFF, false);
            hits.add(new Hit(cx, y, 26, size, auto));
            cx += 26 + gap;
        }
        for (int color : colors) {
            if (cx + size > x + w) {
                cx = x;
                y += size + gap;
            }
            boolean sel = (current | 0xFF000000) == (color | 0xFF000000) && current != 0;
            if (sel) Draw.outline(g, cx - 1, y - 1, size + 2, size + 2, 0xFFFFFFFF);
            Draw.panel(g, cx, y, size, size, Colors.opaque(color), true);
            int c = color;
            hits.add(new Hit(cx, y, size, size, () -> pick.accept(c)));
            cx += size + gap;
        }
        return y + size + 6;
    }

    private void pill(Gfx g, int x, int y, boolean on, Runnable action) {
        int accent = Colors.opaque(cfg().accent);
        Draw.panel(g, x, y, 18, 9, on ? accent : 0xFF2A3042, true);
        int knob = on ? x + 10 : x + 1;
        Draw.panel(g, knob, y + 1, 7, 7, on ? Colors.onColor(accent) : 0xFF8890A6, true);
        hits.add(new Hit(x - 2, y - 2, 22, 13, action));
    }

    private int toggle(Gfx g, int x, int y, int w, String label, boolean value, Consumer<Boolean> set) {
        boolean hover = inside(x, y, w, ROW);
        if (hover) Draw.panel(g, x - 2, y, w + 4, ROW, 0x10FFFFFF, true);
        g.text(font, label, x + 2, y + 3, 0xFFD5DBF0, false);
        pill(g, x + w - 20, y + 2, value, () -> set.accept(!value));
        hits.add(new Hit(x, y, w - 24, ROW, () -> set.accept(!value)));
        return y + ROW + 1;
    }

    private int stepper(Gfx g, int x, int y, int w, String label, String value, Runnable minus, Runnable plus) {
        g.text(font, label, x + 2, y + 3, 0xFFD5DBF0, false);
        int bx = x + w - 54;
        smallButton(g, bx, y, "-", minus);
        int vw = font.width(value);
        g.text(font, value, bx + 27 - vw / 2, y + 3, 0xFFFFFFFF, false);
        smallButton(g, bx + 42, y, "+", plus);
        return y + ROW + 1;
    }

    private void smallButton(Gfx g, int x, int y, String label, Runnable action) {
        boolean hover = inside(x, y, 12, ROW - 1);
        Draw.panel(g, x, y, 12, ROW - 1, hover ? 0x33FFFFFF : 0x18FFFFFF, true);
        g.text(font, label, x + 6 - font.width(label) / 2, y + 2, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, 12, ROW - 1, action));
    }

    private int button(Gfx g, int x, int y, int w, String label, Runnable action) {
        boolean hover = inside(x, y, w, ROW + 2);
        int accent = Colors.opaque(cfg().accent);
        Draw.panel(g, x, y, w, ROW + 2, hover ? Colors.withAlpha(accent, 90) : Colors.withAlpha(accent, 40), true);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 4, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, ROW + 2, action));
        return y + ROW + 6;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (overPanel(mx)) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.contains(mx, my)) {
                    hit.action().run();
                    cfg().save();
                    return true;
                }
            }
            return true;
        }
        for (int i = placed.size() - 1; i >= 0; i--) {
            Placed p = placed.get(i);
            if (p.contains(mx, my)) {
                selected = p.element().id;
                if (event.button() == 0) {
                    dragging = p.element().id;
                    dragOffX = mx - p.x();
                    dragOffY = my - p.y();
                } else if (event.button() == 1) {
                    p.config().enabled = !p.config().enabled;
                    cfg().save();
                }
                return true;
            }
        }
        selected = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        Placed p = placed.stream().filter(e -> e.element().id.equals(dragging)).findFirst().orElse(null);
        if (p == null) {
            return true;
        }
        double nx = event.x() - dragOffX;
        double ny = event.y() - dragOffY;
        snapLineX = -1;
        snapLineY = -1;

        // snap to screen edges and centre lines
        double centerX = nx + p.w() / 2.0;
        double centerY = ny + p.h() / 2.0;
        if (Math.abs(nx) < SNAP) { nx = 0; snapLineX = 0; }
        else if (Math.abs(nx + p.w() - width) < SNAP) { nx = width - p.w(); snapLineX = width - 1; }
        else if (Math.abs(centerX - width / 2.0) < SNAP) { nx = width / 2.0 - p.w() / 2.0; snapLineX = width / 2; }
        if (Math.abs(ny) < SNAP) { ny = 0; snapLineY = 0; }
        else if (Math.abs(ny + p.h() - height) < SNAP) { ny = height - p.h(); snapLineY = height - 1; }
        else if (Math.abs(centerY - height / 2.0) < SNAP) { ny = height / 2.0 - p.h() / 2.0; snapLineY = height / 2; }

        // snap to other elements' edges
        for (Placed o : placed) {
            if (o == p) continue;
            if (snapLineX < 0 && Math.abs(nx - o.x()) < SNAP) { nx = o.x(); snapLineX = o.x(); }
            if (snapLineY < 0 && Math.abs(ny - (o.y() + o.h() + 2)) < SNAP) { ny = o.y() + o.h() + 2; snapLineY = o.y() + o.h() + 1; }
        }

        p.config().x = Math.clamp(nx / width, 0.0, 1.0);
        p.config().y = Math.clamp(ny / height, 0.0, 1.0);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            snapLineX = -1;
            snapLineY = -1;
            cfg().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (overPanel(mx)) {
            int max = Math.max(0, panelContentH - (height - 16));
            panelScroll = Math.clamp(panelScroll - (int) Math.round(scrollY * 14), 0, max);
            return true;
        }
        for (int i = placed.size() - 1; i >= 0; i--) {
            Placed p = placed.get(i);
            if (p.contains(mx, my)) {
                ElementConfig ec = p.config();
                ec.scale = Math.clamp(Math.round((ec.scale + (float) scrollY * 0.1f) * 10) / 10f, 0.5f, 3f);
                selected = p.element().id;
                cfg().save();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
