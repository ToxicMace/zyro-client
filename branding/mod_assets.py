"""Textures for the Zyro Client game mod: menu wordmark, window icons, mod icon.
Run from repo root:  python -I branding/mod_assets.py
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
import generate as gen  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "zyro-client" / "src" / "main" / "resources" / "assets" / "zyro"
POPPINS = ROOT / "packages" / "zyro_app" / "assets" / "fonts" / "Poppins" / "Poppins-SemiBold.ttf"


def wordmark(w=512, h=128) -> Image.Image:
    """Gradient Z-bolt mark + white ZYRO, transparent background."""
    ss = 4
    W, H = w * ss, h * ss
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    mark_h = H * 0.86
    scale = mark_h / 64.0
    ox = -22 * scale + H * 0.02
    oy = (H - mark_h) / 2 - 18 * scale
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).polygon(gen.mark_points(scale, ox, oy), fill=255)
    grad = gen.gradient(W, gen.CYAN, gen.VIOLET).resize((W, H)).convert("RGBA")
    img.paste(grad, (0, 0), mask)
    font = ImageFont.truetype(str(POPPINS), int(H * 0.9))
    d = ImageDraw.Draw(img)
    x = 56 * scale + H * 0.22
    tracking = H * 0.06
    bbox = d.textbbox((0, 0), "Z", font=font)
    y = (H - (bbox[3] - bbox[1])) / 2 - bbox[1]
    for ch in "ZYRO":
        d.text((x, y), ch, font=font, fill=(240, 243, 255, 255))
        x += d.textlength(ch, font=font) + tracking
    return img.resize((w, h), Image.LANCZOS)


def main():
    (RES / "textures" / "gui").mkdir(parents=True, exist_ok=True)
    (RES / "window").mkdir(parents=True, exist_ok=True)
    wordmark().save(RES / "textures" / "gui" / "wordmark.png")
    for s in (16, 32, 48, 128):
        gen.render_icon(s).save(RES / "window" / f"icon_{s}.png")
    gen.render_icon(128).save(RES / "icon.png")
    print("ok")


if __name__ == "__main__":
    main()
