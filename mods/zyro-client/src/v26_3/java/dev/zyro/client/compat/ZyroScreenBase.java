package dev.zyro.client.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for Zyro screens on 26.3, where screens extract render state instead of rendering. */
public abstract class ZyroScreenBase extends Screen {
    protected ZyroScreenBase(Component title) {
        super(title);
    }

    protected abstract void drawScreen(Gfx g, int mouseX, int mouseY, float partialTick);

    protected boolean drawCustomBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (!drawCustomBackground(new Gfx(g), mouseX, mouseY, partialTick)) {
            super.extractBackground(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        drawScreen(new Gfx(g), mouseX, mouseY, partialTick);
    }
}
