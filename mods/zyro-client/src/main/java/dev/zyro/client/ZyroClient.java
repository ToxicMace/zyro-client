package dev.zyro.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zyro.client.config.HudConfig;
import dev.zyro.client.module.Zoom;
import dev.zyro.client.render.ClickTracker;
import dev.zyro.client.render.HudRenderer;
import dev.zyro.client.screen.ZyroEditorScreen;
import dev.zyro.client.window.WindowBranding;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZyroClient implements ClientModInitializer {
    public static final String MOD_ID = "zyro";
    public static final Logger LOGGER = LoggerFactory.getLogger("Zyro Client");

    public static HudConfig CONFIG;
    public static final HudRenderer RENDERER = new HudRenderer();

    public static KeyMapping menuKey;
    public static KeyMapping zoomKey;
    private static KeyMapping toggleHudKey;

    @Override
    public void onInitializeClient() {
        CONFIG = HudConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(id("main"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.zyro.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.zyro.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.zyro.toggle_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            WindowBranding.apply(client);
            applyVanillaModules(client);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                client.setScreen(new ZyroEditorScreen(client.screen));
            }
            while (toggleHudKey.consumeClick()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
            }
            Zoom.tick(client);
        });

        HudElementRegistry.addLast(id("hud"), (graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            ClickTracker.sample(mc);
            if (!CONFIG.enabled || mc.options.hideGui || mc.screen instanceof ZyroEditorScreen) {
                return;
            }
            RENDERER.render(graphics, false);
        });

        LOGGER.info("Zyro Client {} ready on Minecraft {}", modVersion(), minecraftVersion());
    }

    /** Modules that map onto vanilla options. Called on start and whenever settings change. */
    public static void applyVanillaModules(Minecraft mc) {
        if (mc.options == null) {
            return;
        }
        mc.options.toggleSprint().set(CONFIG.toggleSprint);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    public static String modVersion() {
        String full = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");
        int plus = full.indexOf('+');
        return plus > 0 ? full.substring(0, plus) : full;
    }

    public static String windowTitle() {
        return "Zyro Client " + modVersion() + "  •  " + minecraftVersion();
    }
}
