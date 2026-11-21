package dev.zyro.client.compat;

import dev.zyro.client.ZyroClient;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/** Swaps the game window icon for the Zyro mark (the title is handled by a mixin). */
final class WindowIcon {
    private static final String[] ICONS = {"/assets/zyro/window/icon_16.png", "/assets/zyro/window/icon_32.png",
            "/assets/zyro/window/icon_48.png", "/assets/zyro/window/icon_128.png"};

    private WindowIcon() {}

    public static void apply(Minecraft mc) {
        try {
            setIcon(mc.getWindow().handle());
            mc.updateTitle();
        } catch (Throwable t) {
            ZyroClient.LOGGER.warn("Could not apply Zyro window branding", t);
        }
    }

    private static void setIcon(long window) throws Exception {
        List<ByteBuffer> files = new ArrayList<>();
        List<ByteBuffer> pixels = new ArrayList<>();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer images = GLFWImage.malloc(ICONS.length, stack);
            int count = 0;
            for (String path : ICONS) {
                try (InputStream in = WindowIcon.class.getResourceAsStream(path)) {
                    if (in == null) continue;
                    byte[] bytes = in.readAllBytes();
                    ByteBuffer file = MemoryUtil.memAlloc(bytes.length);
                    file.put(bytes).flip();
                    files.add(file);
                    IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), c = stack.mallocInt(1);
                    ByteBuffer data = STBImage.stbi_load_from_memory(file, w, h, c, 4);
                    if (data == null) continue;
                    pixels.add(data);
                    images.position(count).width(w.get(0)).height(h.get(0)).pixels(data);
                    count++;
                }
            }
            if (count > 0) {
                images.position(0).limit(count);
                GLFW.glfwSetWindowIcon(window, images);
            }
        } finally {
            pixels.forEach(STBImage::stbi_image_free);
            files.forEach(MemoryUtil::memFree);
        }
    }
}
