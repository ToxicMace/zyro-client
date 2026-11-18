package dev.zyro.hud.render;

public final class Colors {
    private Colors() {}

    public static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    public static int opaque(int argb) {
        return argb | 0xFF000000;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.clamp(t, 0f, 1f);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24)
                | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8)
                | Math.round(ab + (bb - ab) * t);
    }

    /** Smooth rainbow; offset lets neighbouring items run a little out of phase. */
    public static int chroma(float speed, float offset) {
        float hue = (float) ((System.currentTimeMillis() * speed / 4000.0 + offset) % 1.0);
        return hsv(hue, 0.55f, 1f);
    }

    public static int hsv(float h, float s, float v) {
        float r, g, b;
        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (Math.floorMod(i, 6)) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** Readable text color on top of a fill. */
    public static int onColor(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return lum > 150 ? 0xFF10131C : 0xFFFFFFFF;
    }

    public static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }
}
