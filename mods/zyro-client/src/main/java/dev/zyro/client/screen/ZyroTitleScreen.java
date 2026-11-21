package dev.zyro.client.screen;

import dev.zyro.client.ZyroClient;
import dev.zyro.client.render.Colors;
import dev.zyro.client.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Zyro main menu: blurred panorama, a soft left-side scrim, the Zyro wordmark and a
 * vertical list of flat buttons with an accent bar that slides in on hover.
 */
public final class ZyroTitleScreen extends Screen {
    private static final Identifier WORDMARK = ZyroClient.id("textures/gui/wordmark.png");
    private static final int WORDMARK_TEX_W = 512;
    private static final int WORDMARK_TEX_H = 128;

    private static final int BUTTON_W = 156;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 3;

    private record Entry(String label, Runnable action) {}

    private final List<Entry> entries = new ArrayList<>();
    private float[] hover = new float[0];
    private float appear;
    private int mouseX;
    private int mouseY;

    public ZyroTitleScreen() {
        super(Component.literal("Zyro Client"));
    }

    @Override
    protected void init() {
        entries.clear();
        entries.add(new Entry("Singleplayer", () -> minecraft.setScreen(new SelectWorldScreen(this))));
        entries.add(new Entry("Multiplayer", () -> minecraft.setScreen(new JoinMultiplayerScreen(this))));
        entries.add(new Entry("Zyro Settings", () -> minecraft.setScreen(new ZyroEditorScreen(this))));
        entries.add(new Entry("Options", () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        entries.add(new Entry("Quit Game", () -> minecraft.stop()));
        if (hover.length != entries.size()) {
            hover = new float[entries.size()];
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int menuX() {
        return Math.max(24, width / 14);
    }

    private int menuTop() {
        int listH = entries.size() * (BUTTON_H + BUTTON_GAP);
        return height / 2 - listH / 2 + 26;
    }

    private static float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        appear = Math.min(1f, appear + 0.05f);
        float a = ease(appear);
        int accent = Colors.opaque(ZyroClient.CONFIG.accent);

        // left scrim: solid then a soft fade so the panorama stays visible on the right
        int solid = Math.min(width, menuX() + BUTTON_W + 40);
        g.fill(0, 0, solid, height, Colors.withAlpha(0x06080F, Math.round(150 * a)));
        int fade = 90;
        for (int i = 0; i < fade; i += 2) {
            int alpha = Math.round(150 * a * (1f - (float) i / fade));
            g.fill(solid + i, 0, solid + i + 2, height, Colors.withAlpha(0x06080F, alpha));
        }

        int x = menuX() - Math.round((1f - a) * 24);
        int top = menuTop();

        // wordmark
        int logoH = 28;
        int logoW = logoH * WORDMARK_TEX_W / WORDMARK_TEX_H;
        int logoY = top - logoH - 26;
        g.blit(RenderPipelines.GUI_TEXTURED, WORDMARK, x, logoY, 0f, 0f, logoW, logoH, logoW, logoH);
        String tag = "CLIENT  •  " + ZyroClient.minecraftVersion();
        g.drawString(font, tag, x + 2, logoY + logoH + 4, Colors.withAlpha(0xB8C0D6, Math.round(255 * a)), false);

        // buttons
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int by = top + i * (BUTTON_H + BUTTON_GAP);
            boolean over = mouseX >= x && mouseX < x + BUTTON_W && mouseY >= by && mouseY < by + BUTTON_H;
            hover[i] += ((over ? 1f : 0f) - hover[i]) * 0.3f;
            float h = hover[i];

            Draw.panel(g, x, by, BUTTON_W, BUTTON_H, Colors.withAlpha(0xFFFFFF, Math.round((10 + 22 * h) * a)), true);
            int barH = Math.round((BUTTON_H - 6) * h);
            if (barH > 0) {
                int barY = by + (BUTTON_H - barH) / 2;
                g.fill(x, barY, x + 2, barY + barH, accent);
            }
            int textX = x + 10 + Math.round(4 * h);
            int textColor = Colors.lerp(0xFFD5DBF0, 0xFFFFFFFF, h);
            boolean quit = i == entries.size() - 1;
            if (quit) {
                textColor = Colors.lerp(0xFF9AA1B5, 0xFFFF8A8A, h);
            }
            g.drawString(font, e.label(), textX, by + (BUTTON_H - 8) / 2,
                    Colors.withAlpha(textColor, Math.round(255 * a)), false);
        }

        // footer
        int muted = Colors.withAlpha(0x8A91A6, Math.round(255 * a));
        String left = "Zyro Client " + ZyroClient.modVersion() + "  •  Minecraft " + ZyroClient.minecraftVersion();
        g.drawString(font, left, x, height - 16, muted, false);
        String right = "Not affiliated with Mojang or Microsoft";
        g.drawString(font, right, width - font.width(right) - 10, height - 16, Colors.withAlpha(0x6B7186, Math.round(255 * a)), false);

        // signed-in player, top right
        String user = minecraft.getUser().getName();
        int uw = font.width(user) + 22;
        int ux = width - uw - 10;
        Draw.panel(g, ux, 10, uw, 16, Colors.withAlpha(0x06080F, Math.round(150 * a)), true);
        g.fill(ux + 7, 16, ux + 11, 20, accent);
        g.drawString(font, user, ux + 15, 14, Colors.withAlpha(0xE6EAFF, Math.round(255 * a)), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int x = menuX();
            int top = menuTop();
            for (int i = 0; i < entries.size(); i++) {
                int by = top + i * (BUTTON_H + BUTTON_GAP);
                if (event.x() >= x && event.x() < x + BUTTON_W && event.y() >= by && event.y() < by + BUTTON_H) {
                    entries.get(i).action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
