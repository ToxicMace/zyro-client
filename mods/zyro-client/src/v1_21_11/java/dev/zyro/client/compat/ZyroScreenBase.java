package dev.zyro.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for Zyro screens: subclasses draw through {@link Gfx} and never touch GuiGraphics. */
public abstract class ZyroScreenBase extends Screen {
    protected ZyroScreenBase(Component title) {
        super(title);
    }

    /** Draw the screen contents (widgets and background are already drawn). */
    protected abstract void drawScreen(Gfx g, int mouseX, int mouseY, float partialTick);

    /** Return true after drawing a custom background to skip the vanilla one. */
    protected boolean drawCustomBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!drawCustomBackground(new Gfx(g), mouseX, mouseY, partialTick)) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawScreen(new Gfx(g), mouseX, mouseY, partialTick);
    }
}
