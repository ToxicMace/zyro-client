package dev.zyro.hud.config;

import java.util.List;

/** Per-element layout and overrides. Position is a fraction of the screen so it survives resizes. */
public final class ElementConfig {
    public static final List<String> DEFAULT_ORDER = List.of(
            "fps", "cps", "ping", "coords", "speed", "clock", "keystrokes", "armor");

    public boolean enabled = true;
    public double x;
    public double y;
    public float scale = 1.0f;
    /** null = use the global text color. */
    public Integer textColor;
    /** null = use the global accent. */
    public Integer accentColor;
    public boolean background = true;
    public boolean chroma = false;

    public static ElementConfig defaultsFor(String id) {
        ElementConfig c = new ElementConfig();
        switch (id) {
            case "fps" -> { c.x = 0.008; c.y = 0.012; }
            case "cps" -> { c.x = 0.008; c.y = 0.058; }
            case "ping" -> { c.x = 0.008; c.y = 0.104; }
            case "coords" -> { c.x = 0.008; c.y = 0.150; }
            case "speed" -> { c.x = 0.008; c.y = 0.300; c.enabled = false; }
            case "clock" -> { c.x = 0.935; c.y = 0.012; }
            case "keystrokes" -> { c.x = 0.008; c.y = 0.640; }
            case "armor" -> { c.x = 0.955; c.y = 0.560; c.background = false; }
            default -> { c.x = 0.5; c.y = 0.5; }
        }
        return c;
    }
}
