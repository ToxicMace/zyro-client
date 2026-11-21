"""Zyro home backgrounds: aurora curtains over a night sky.

Two variants so each Zyro Client version has its own look:
  ZyroAurora.jpg  (26.x)    cyan -> blue -> violet
  ZyroNebula.jpg  (1.21.x)  violet -> magenta -> rose

Run from repo root:  python -I branding/background.py
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "packages" / "zyro_app" / "assets" / "backgrounds"
W, H = 2560, 1440
SW, SH = 1280, 720  # render scale; upscaled at the end for extra softness

VARIANTS = {
    "ZyroAurora.jpg": dict(
        seed=3,
        stops=[(34, 211, 238), (76, 110, 245), (124, 58, 237)],
        sky_top=(10, 12, 26), sky_bottom=(3, 4, 9),
    ),
    "ZyroNebula.jpg": dict(
        seed=11,
        stops=[(124, 58, 237), (217, 70, 239), (251, 113, 133)],
        sky_top=(16, 9, 26), sky_bottom=(5, 3, 9),
    ),
}


def lerp_stops(stops, t):
    t = np.clip(t, 0, 1) * (len(stops) - 1)
    i = np.minimum(t.astype(int), len(stops) - 2)
    f = (t - i)[..., None]
    a = np.array(stops, np.float32)[i]
    b = np.array(stops, np.float32)[i + 1]
    return a + (b - a) * f


def render(seed, stops, sky_top, sky_bottom):
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:SH, 0:SW].astype(np.float32)
    u, v = xx / SW, yy / SH

    sky = np.array(sky_top, np.float32) * (1 - v[..., None]) + np.array(sky_bottom, np.float32) * v[..., None]

    light = np.zeros((SH, SW), np.float32)
    color = np.zeros((SH, SW, 3), np.float32)
    ribbons = [
        # base height, amp1, len1, amp2, len2, up height, strength, x-start
        (0.58, 0.07, 1.35, 0.025, 0.42, 0.30, 1.00, 0.30),
        (0.66, 0.05, 0.95, 0.020, 0.33, 0.22, 0.70, 0.42),
        (0.50, 0.04, 1.80, 0.018, 0.55, 0.18, 0.45, 0.55),
    ]
    for k, (base, a1, l1, a2, l2, up, strength, start) in enumerate(ribbons):
        p1, p2, p3 = rng.uniform(0, 2 * np.pi, 3)
        centre = base + a1 * np.sin(2 * np.pi * u / l1 + p1) + a2 * np.sin(2 * np.pi * u / l2 + p2)
        d = v - centre
        below = np.exp(-(np.maximum(d, 0) / 0.018) ** 2)
        above = np.exp(-(np.maximum(-d, 0) / up) ** 1.4)
        band = np.where(d > 0, below, above)
        # curtain striations that drift slowly along the ribbon
        stria = 0.62 + 0.38 * (0.5 + 0.5 * np.sin(u * SW / (9 + 3 * k) + 3 * np.sin(u * 5 + p3)))
        stria *= 0.8 + 0.2 * np.sin(u * SW / 41 + p2)
        # fade in from the left so the hero card area stays calm
        envelope = np.clip((u - start) / 0.35, 0, 1) ** 1.5 * np.clip((1.08 - u) / 0.2, 0, 1)
        intensity = band * stria * envelope * strength
        hue = lerp_stops(stops, u * 0.9 + (v - centre) * 0.6 + 0.05 * k)
        light += intensity
        color += hue * intensity[..., None]

    color = color / np.maximum(light, 1e-4)[..., None]
    aurora = color * np.clip(light, 0, 1.6)[..., None] * 0.85

    img = sky + aurora
    # bloom
    bloom = Image.fromarray(np.clip(aurora, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(28))
    img += np.asarray(bloom, np.float32) * 0.55

    # stars, mostly in the upper sky and away from the brightest light
    stars = np.zeros((SH, SW), np.float32)
    n = 420
    sx = rng.integers(0, SW, n)
    sy = (rng.beta(1.2, 2.6, n) * SH * 0.85).astype(int)
    stars[sy, sx] = rng.uniform(0.25, 1.0, n) ** 2.2 * 255
    stars_img = Image.fromarray(stars.astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))
    star_layer = np.asarray(stars_img, np.float32)[..., None] * np.array([0.85, 0.9, 1.0])
    img += star_layer * (1 - np.clip(light, 0, 1))[..., None] * 1.6

    # vignette + calmer left side for the hero card
    r = np.sqrt(((u - 0.62) / 0.75) ** 2 + ((v - 0.5) / 0.65) ** 2)
    img *= np.clip(1.15 - 0.55 * r ** 1.6, 0.35, 1)[..., None]
    img *= (0.62 + 0.38 * np.clip(u / 0.45, 0, 1))[..., None]

    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).resize((W, H), Image.LANCZOS)
    arr = np.asarray(out, np.float32)
    arr += rng.normal(0, 2.6, (H, W, 1)).astype(np.float32)  # grain against banding
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, cfg in VARIANTS.items():
        render(**cfg).save(OUT_DIR / name, quality=90, optimize=True, progressive=True)
        print(OUT_DIR / name)


if __name__ == "__main__":
    main()
