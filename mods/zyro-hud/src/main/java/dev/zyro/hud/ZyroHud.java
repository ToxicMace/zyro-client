package dev.zyro.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zyro.hud.config.HudConfig;
import dev.zyro.hud.render.ClickTracker;
import dev.zyro.hud.render.HudRenderer;
import dev.zyro.hud.screen.ZyroEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZyroHud implements ClientModInitializer {
    public static final String MOD_ID = "zyrohud";
    public static final Logger LOGGER = LoggerFactory.getLogger("Zyro HUD");

    public static HudConfig CONFIG;
    public static final HudRenderer RENDERER = new HudRenderer();

    private static KeyMapping editorKey;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        CONFIG = HudConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(id("main"));
        editorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.zyrohud.editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.zyrohud.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editorKey.consumeClick()) {
                client.setScreen(new ZyroEditorScreen(client.screen));
            }
            while (toggleKey.consumeClick()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
            }
        });

        HudElementRegistry.addLast(id("hud"), (graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            ClickTracker.sample(mc);
            if (!CONFIG.enabled || mc.options.hideGui || mc.screen instanceof ZyroEditorScreen) {
                return;
            }
            RENDERER.render(graphics, false);
        });

        LOGGER.info("Zyro HUD ready. Right Shift opens the editor.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
