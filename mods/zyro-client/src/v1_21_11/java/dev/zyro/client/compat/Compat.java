package dev.zyro.client.compat;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** 1.21.11 implementations of the few calls that differ between Minecraft versions. */
public final class Compat {
    private Compat() {}

    public static final com.mojang.blaze3d.platform.InputConstants.Type KEYBOARD =
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM;

    public static net.minecraft.client.KeyMapping registerKey(net.minecraft.client.KeyMapping key) {
        return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(key);
    }

    public static boolean hudHidden(Minecraft mc) {
        return mc.options.hideGui;
    }

    public static Screen screen(Minecraft mc) {
        return mc.screen;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    public static void registerHud(Identifier id, Consumer<Gfx> draw) {
        HudElementRegistry.addLast(id, (graphics, tickCounter) -> draw.accept(new Gfx(graphics)));
    }

    public static void applyWindowIcon(Minecraft mc) {
        WindowIcon.apply(mc);
    }
}
