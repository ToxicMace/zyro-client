"""Zyro Client branding generator.

Builds every logo/icon asset the launcher needs from one geometric source:
the "Z-bolt" mark (a bold Z whose diagonal is a lightning bolt) and a
Poppins SemiBold "ZYRO" wordmark.

Run:  python -I branding/generate.py   (from the repo root)
Needs: Pillow, fonttools
"""

from __future__ import annotations

import io
import re
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "packages" / "oneclient_app"
ICONS = APP / "icons"
ASSETS = APP / "assets"
OUT = ROOT / "branding" / "out"

# Brand colors
CYAN = (34, 211, 238)
VIOLET = (124, 58, 237)
NIGHT_TOP = (20, 24, 40)
NIGHT_BOTTOM = (9, 11, 20)

# Z-bolt mark, 100x100 design space.
MARK_PATH = "M22 18H78V32L55 50H66L43 68H78V82H22V68L45 50H34L57 32H22Z"


def mark_points(scale: float, ox: float = 0.0, oy: float = 0.0) -> list[tuple[float, float]]:
    pts: list[tuple[float, float]] = []
    x = y = 0.0
    for cmd, args in re.findall(r"([MLHVZ])([^MLHVZ]*)", MARK_PATH):
        nums = [float(n) for n in re.findall(r"-?\d+(?:\.\d+)?", args)]
        if cmd in "ML":
            x, y = nums[0], nums[1]
        elif cmd == "H":
            x = nums[0]
        elif cmd == "V":
            y = nums[0]
        else:
            continue
        pts.append((ox + x * scale, oy + y * scale))
    return pts


def gradient(size: int, a: tuple, b: tuple, diagonal: bool = True) -> Image.Image:
    """Top-left -> bottom-right linear gradient."""
    import numpy as np

    idx = np.arange(size, dtype=np.float32)
    if diagonal:
        t = (idx[None, :] + idx[:, None]) / (2 * (size - 1))
    else:
        t = np.repeat((idx / (size - 1))[:, None], size, axis=1)
    a_ = np.array(a, dtype=np.float32)
    b_ = np.array(b, dtype=np.float32)
    rgb = a_ + (b_ - a_) * t[..., None]
    return Image.fromarray(np.clip(rgb.round(), 0, 255).astype(np.uint8), "RGB")


def squircle_mask(size: int, radius_ratio: float = 0.225) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle(
        (0, 0, size - 1, size - 1), radius=int(size * radius_ratio), fill=255
    )
    return m


def render_icon(size: int, background: bool = True) -> Image.Image:
    """App icon: night squircle, gradient Z-bolt with a soft glow."""
    ss = 4
    big = size * ss
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))

    if background:
        bg = gradient(big, NIGHT_TOP, NIGHT_BOTTOM, diagonal=False).convert("RGBA")
        # faint inner rim light
        rim = Image.new("L", (big, big), 0)
        ImageDraw.Draw(rim).rounded_rectangle(
            (big * 0.012, big * 0.012, big * 0.988, big * 0.988),
            radius=int(big * 0.215),
            outline=255,
            width=max(1, int(big * 0.008)),
        )
        rim_layer = Image.new("RGBA", (big, big), (255, 255, 255, 0))
        rim_layer.putalpha(rim.point(lambda v: v * 22 // 255))
        bg = Image.alpha_composite(bg, rim_layer)
        canvas.paste(bg, (0, 0), squircle_mask(big))

    # Mark occupies ~66% of the tile when on a background, ~92% standalone.
    fill = 0.54 if background else 0.92
    scale = big * fill / 64.0  # mark spans 56..64 units of the 100 box
    span_w, span_h = 56 * scale, 64 * scale
    ox = (big - span_w) / 2 - 22 * scale
    oy = (big - span_h) / 2 - 18 * scale
    pts = mark_points(scale, ox, oy)

    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)

    grad = gradient(big, CYAN, VIOLET).convert("RGBA")

    if background:
        glow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
        glow.paste(grad, (0, 0), mask)
        glow = glow.filter(ImageFilter.GaussianBlur(big * 0.06))
        alpha = glow.getchannel("A").point(lambda v: int(v * 0.55))
        glow.putalpha(alpha)
        canvas = Image.alpha_composite(canvas, glow)

    mark = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    mark.paste(grad, (0, 0), mask)
    canvas = Image.alpha_composite(canvas, mark)

    if background:
        canvas.putalpha(ImageChops.multiply(canvas.getchannel("A"), squircle_mask(big)))

    return canvas.resize((size, size), Image.LANCZOS)


def render_tray(size: int) -> Image.Image:
    """Tray: standalone gradient mark, no tile, maximum legibility."""
    return render_icon(size, background=False)


# ---------- SVG outputs ----------


def svg_mark_currentcolor() -> str:
    # 80x80 viewBox like the original icon-logo.svg; mark scaled to ~70%.
    s = 0.875
    pts = mark_points(s, 40 - 50 * s, 40 - 50 * s)
    d = "M" + "L".join(f"{x:.2f} {y:.2f}" for x, y in pts) + "Z"
    return (
        '<svg width="80" height="80" viewBox="0 0 80 80" fill="none" '
        'xmlns="http://www.w3.org/2000/svg">\n'
        f'<path d="{d}" fill="currentColor"/>\n</svg>\n'
    )


def svg_mark_gradient(size: int = 100) -> str:
    return (
        f'<svg width="{size}" height="{size}" viewBox="0 0 100 100" fill="none" '
        'xmlns="http://www.w3.org/2000/svg">\n'
        '<defs><linearGradient id="z" x1="22" y1="18" x2="78" y2="82" gradientUnits="userSpaceOnUse">'
        f'<stop stop-color="#{CYAN[0]:02X}{CYAN[1]:02X}{CYAN[2]:02X}"/>'
        f'<stop offset="1" stop-color="#{VIOLET[0]:02X}{VIOLET[1]:02X}{VIOLET[2]:02X}"/>'
        "</linearGradient></defs>\n"
        f'<path d="{MARK_PATH}" fill="url(#z)"/>\n</svg>\n'
    )


def text_path(font_file: Path, text: str, size: float, x: float, baseline: float, tracking: float) -> tuple[str, float]:
    font = TTFont(font_file)
    gs = font.getGlyphSet()
    cmap = font.getBestCmap()
    upm = font["head"].unitsPerEm
    hmtx = font["hmtx"]
    k = size / upm
    parts = []
    cursor = x
    for ch in text:
        name = cmap[ord(ch)]
        pen = SVGPathPen(gs)
        gs[name].draw(TransformPen(pen, (k, 0, 0, -k, cursor, baseline)))
        parts.append(pen.getCommands())
        cursor += hmtx[name][0] * k + tracking
    return " ".join(parts), cursor - tracking - x


def svg_wordmark() -> str:
    """Text-only "ZYRO" wordmark; currentColor so the launcher can tint it."""
    poppins = ASSETS / "fonts" / "Poppins" / "Poppins-SemiBold.ttf"
    word_d, word_w = text_path(poppins, "ZYRO", 33.0, 0.5, 28.4, 4.0)
    width = word_w + 1
    print(f"wordmark ratio {width / 32:.4f}")
    return (
        f'<svg width="{width:.0f}" height="32" viewBox="0 0 {width:.0f} 32" fill="none" '
        'xmlns="http://www.w3.org/2000/svg">\n'
        f'<path d="{word_d}" fill="currentColor"/>\n</svg>\n'
    )

def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)

    master = render_icon(1024)
    master.save(OUT / "zyro-icon-1024.png")
    render_tray(512).save(OUT / "zyro-mark-512.png")
    (OUT / "zyro-mark.svg").write_text(svg_mark_gradient(), encoding="utf-8")

    for name, size in {
        "32x32.png": 32,
        "64x64.png": 64,
        "128x128.png": 128,
        "128x128@2x.png": 256,
        "512x512.png": 512,
        "icon.png": 512,
        "StoreLogo.png": 50,
    }.items():
        render_icon(size).save(ICONS / name)

    render_tray(64).save(ICONS / "tray.png")

    ico_sizes = [16, 24, 32, 48, 64, 128, 256]
    frames = [render_icon(s) for s in ico_sizes]
    frames[-1].save(ICONS / "icon.ico", format="ICO", sizes=[(s, s) for s in ico_sizes], append_images=frames[:-1])
    master.save(ICONS / "icon.icns", format="ICNS")

    (ASSETS / "icons" / "icon-logo.svg").write_text(svg_mark_currentcolor(), encoding="utf-8")
    (ASSETS / "logo.svg").write_text(svg_wordmark(), encoding="utf-8")

    bundle = ICONS / "OneClient.icon"
    if bundle.exists():
        assets = bundle / "Assets"
        for old in assets.glob("*.svg"):
            old.unlink()
        (assets / "zyro-mark.svg").write_text(svg_mark_gradient(), encoding="utf-8")
        manifest = (bundle / "icon.json").read_text(encoding="utf-8")
        manifest = manifest.replace("oneclient-mark.svg", "zyro-mark.svg").replace(
            '"oneclient-mark"', '"zyro-mark"'
        )
        (bundle / "icon.json").write_text(manifest, encoding="utf-8")

    print("ok")


if __name__ == "__main__":
    main()
