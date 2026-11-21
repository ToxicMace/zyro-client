"""Zyro home background: soft aurora blobs on deep night, faint Z-bolt watermark.
Run from repo root:  python -I branding/background.py
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).parent))
import generate as gen  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "packages" / "zyro_app" / "assets" / "backgrounds" / "ZyroAurora.jpg"
W, H = 2560, 1440


def main():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    base = np.zeros((H, W, 3), np.float32)
    top, bottom = np.array([14, 17, 32]), np.array([5, 6, 12])
    t = (yy / H)[..., None]
    base += top * (1 - t) + bottom * t

    def blob(cx, cy, rx, ry, color, strength):
        d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
        return np.exp(-d)[..., None] * np.array(color, np.float32) * strength

    base += blob(W * 0.78, H * 0.30, W * 0.30, H * 0.34, gen.VIOLET, 0.55)
    base += blob(W * 0.58, H * 0.62, W * 0.26, H * 0.22, gen.CYAN, 0.40)
    base += blob(W * 0.95, H * 0.85, W * 0.25, H * 0.25, (76, 110, 245), 0.35)
    base += blob(W * 0.15, H * 0.10, W * 0.30, H * 0.20, (60, 40, 140), 0.18)

    img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB")

    # faint oversized Z-bolt watermark on the right
    mark = Image.new("L", (W, H), 0)
    s = H * 1.1 / 64.0
    ImageDraw.Draw(mark).polygon(gen.mark_points(s, W * 0.60 - 22 * s, H * 0.5 - 50 * s), fill=255)
    mark = mark.filter(ImageFilter.GaussianBlur(3)).point(lambda v: v * 14 // 255)
    img = Image.composite(Image.new("RGB", (W, H), (190, 200, 255)), img, mark)

    # film grain to avoid banding
    noise = np.random.default_rng(7).normal(0, 3.2, (H, W, 1)).astype(np.float32)
    arr = np.clip(np.asarray(img, np.float32) + noise, 0, 255).astype(np.uint8)
    Image.fromarray(arr).save(OUT, quality=90, optimize=True)
    print(OUT)


if __name__ == "__main__":
    main()
