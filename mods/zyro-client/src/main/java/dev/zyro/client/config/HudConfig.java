package dev.zyro.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.zyro.client.ZyroClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the player can tune, persisted to config/zyro-hud.json.
 * Colors are stored as ARGB ints so the launcher (or a human) can edit the file directly.
 */
public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public String preset = "zyro";

    /** Accent: labels, keystroke press fill, accent bar. */
    public int accent = 0xFF5B7CFF;
    /** Main value text. */
    public int text = 0xFFE6EAFF;
    /** Panel background RGB (alpha comes from backgroundOpacity). */
    public int background = 0x000B0E17;
    public int backgroundOpacity = 150;

    public boolean textShadow = false;
    public boolean accentBar = true;
    public boolean chroma = false;
    public float chromaSpeed = 1.0f;
    public boolean roundedCorners = true;
    public int padding = 4;

    // ---- Zyro Client modules
    /** Replace the vanilla title screen with the Zyro main menu. */
    public boolean customMenu = true;
    public boolean zoomEnabled = true;
    /** FOV multiplier while zooming (0.1 = strong, 0.5 = light). */
    public float zoomFactor = 0.25f;
    public boolean zoomSmoothCamera = true;
    public boolean toggleSprint = false;

    public Map<String, ElementConfig> elements = new LinkedHashMap<>();

    private transient Path path;

    public ElementConfig element(String id) {
        return elements.computeIfAbsent(id, key -> ElementConfig.defaultsFor(key));
    }

    public int backgroundArgb() {
        return (Math.clamp(backgroundOpacity, 0, 255) << 24) | (background & 0x00FFFFFF);
    }

    public void applyPreset(Preset p) {
        preset = p.id;
        accent = p.accent;
        text = p.text;
        background = p.background & 0x00FFFFFF;
    }

    public void resetLayout() {
        elements.clear();
        for (String id : ElementConfig.DEFAULT_ORDER) {
            element(id);
        }
    }

    public static HudConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("zyro-client.json");
        HudConfig config = null;
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, HudConfig.class);
            } catch (Exception e) {
                ZyroClient.LOGGER.warn("Zyro config is unreadable, using defaults", e);
            }
        }
        if (config == null) {
            config = new HudConfig();
        }
        if (config.elements == null) {
            config.elements = new LinkedHashMap<>();
        }
        for (String id : ElementConfig.DEFAULT_ORDER) {
            config.element(id);
        }
        config.path = path;
        config.save();
        return config;
    }

    public void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ZyroClient.LOGGER.warn("Could not save Zyro config", e);
        }
    }

    /** Matches the launcher's built-in theme families. */
    public enum Preset {
        ZYRO("zyro", "Zyro", 0xFF5B7CFF, 0xFFE6EAFF, 0x0B0E17),
        VOID("void", "Void", 0xFF22D3EE, 0xFFEDEFF7, 0x000000),
        AURORA("aurora", "Aurora", 0xFF2DD4BF, 0xFFDDF4EE, 0x081114),
        EMBER("ember", "Ember", 0xFFFF6B3D, 0xFFFBE9E2, 0x120D0C),
        SAKURA("sakura", "Sakura", 0xFFF472B6, 0xFFF7E6F1, 0x140F15),
        MONO("mono", "Mono", 0xFFE2E8F0, 0xFFFFFFFF, 0x000000);

        public final String id;
        public final String label;
        public final int accent;
        public final int text;
        public final int background;

        Preset(String id, String label, int accent, int text, int background) {
            this.id = id;
            this.label = label;
            this.accent = accent;
            this.text = text;
            this.background = background;
        }
    }
}
