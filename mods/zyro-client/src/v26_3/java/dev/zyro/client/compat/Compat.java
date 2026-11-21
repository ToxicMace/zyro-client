package dev.zyro.client.compat;

import dev.zyro.client.ZyroClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/** 26.3 implementations: screens live on mc.gui, the window is SDL3. */
public final class Compat {
    private Compat() {}

    public static final com.mojang.blaze3d.platform.InputConstants.Type KEYBOARD =
            com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD;

    public static net.minecraft.client.KeyMapping registerKey(net.minecraft.client.KeyMapping key) {
        return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(key);
    }

    /** 26.x skips HUD elements itself while the GUI is hidden (F1). */
    public static boolean hudHidden(Minecraft mc) {
        return false;
    }

    public static Screen screen(Minecraft mc) {
        return mc.gui.screen();
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    public static void registerHud(Identifier id, Consumer<Gfx> draw) {
        HudElementRegistry.addLast(id, (graphics, tickCounter) -> draw.accept(new Gfx(graphics)));
    }

    public static void applyWindowIcon(Minecraft mc) {
        ByteBuffer file = null;
        ByteBuffer pixels = null;
        try (InputStream in = Compat.class.getResourceAsStream("/assets/zyro/window/icon_128.png");
             MemoryStack stack = MemoryStack.stackPush()) {
            if (in == null) return;
            byte[] bytes = in.readAllBytes();
            file = MemoryUtil.memAlloc(bytes.length);
            file.put(bytes).flip();
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), c = stack.mallocInt(1);
            pixels = STBImage.stbi_load_from_memory(file, w, h, c, 4);
            if (pixels == null) return;
            SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(
                    w.get(0), h.get(0), SDLPixels.SDL_PIXELFORMAT_ABGR8888, pixels, w.get(0) * 4);
            if (surface == null) return;
            SDLVideo.SDL_SetWindowIcon(mc.getWindow().handle(), surface);
            SDLSurface.SDL_DestroySurface(surface);
        } catch (Throwable t) {
            ZyroClient.LOGGER.warn("Could not set the Zyro window icon", t);
        } finally {
            if (pixels != null) STBImage.stbi_image_free(pixels);
            if (file != null) MemoryUtil.memFree(file);
        }
    }
}
