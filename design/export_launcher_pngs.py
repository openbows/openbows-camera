"""Export openbows_camera_logo.svg look-alike PNGs for store / dist."""
from __future__ import annotations

import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "dist" / "icons"
SVG = ROOT / "design" / "openbows_camera_logo.svg"

GREEN = (46, 125, 50, 255)  # #2e7d32
WHITE = (255, 255, 255, 255)

SIZES = {
    "ic_launcher_48.png": 48,
    "ic_launcher_72.png": 72,
    "ic_launcher_96.png": 96,
    "ic_launcher_108.png": 108,
    "ic_launcher_144.png": 144,
    "ic_launcher_192.png": 192,
    "ic_launcher_512.png": 512,
    "ic_launcher.png": 512,
}


def draw_logo(size: int) -> Image.Image:
    s = size / 24.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def px(v: float) -> float:
        return v * s

    stroke = max(1, round(1.5 * s))
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=px(4), fill=GREEN)
    d.rounded_rectangle(
        [px(3), px(5), px(21), px(19)],
        radius=px(2),
        outline=WHITE,
        width=stroke,
    )
    cx, cy = px(12), px(12)
    for r in (4.5, 2.0):
        rr = px(r)
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=WHITE, width=stroke)
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, sz in SIZES.items():
        draw_logo(sz).save(OUT / name, "PNG")
        print("wrote", name, sz)
    if SVG.exists():
        shutil.copy2(SVG, OUT / "openbows_camera_logo.svg")
        print("copied openbows_camera_logo.svg")


if __name__ == "__main__":
    main()
