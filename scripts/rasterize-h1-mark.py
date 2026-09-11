#!/usr/bin/env python3
"""Rasterize locked H1 Stream Acrylic SVG into launcher mipmaps and the in-app mark."""

from __future__ import annotations

import subprocess
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "app/src/main/assets/brand"
APP_RES = ROOT / "app/src/main/res"
CORE_RES = ROOT / "core-ui/src/main/res"

MARK_SVG = BRAND / "confluence-mark.svg"
FG_SVG = BRAND / "ic_launcher_foreground.svg"
BG_SVG = BRAND / "ic_launcher_background.svg"

# Legacy launcher sizes (48dp). Adaptive layers use 108dp.
LAUNCHER_DP = 48
ADAPTIVE_DP = 108
ADAPTIVE_SAFE_DP = 72
SPLASH_ICON_DP = 240
SPLASH_MARK_DP = 108
DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def rsvg(svg: Path, dest: Path, size: int) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            "rsvg-convert",
            "-w",
            str(size),
            "-h",
            str(size),
            str(svg),
            "-o",
            str(dest),
        ],
        check=True,
    )


def circle_crop(src: Path, dest: Path) -> None:
    img = Image.open(src).convert("RGBA")
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, img.size[0] - 1, img.size[1] - 1), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, mask=mask)
    dest.parent.mkdir(parents=True, exist_ok=True)
    out.save(dest, "PNG")


def main() -> None:
    # Canonical 512 production raster (also the in-app HUD + window splash source).
    mark_512 = BRAND / "confluence-mark-512.png"
    rsvg(MARK_SVG, mark_512, 512)
    nodpi_mark = CORE_RES / "drawable-nodpi/confluence_mark.png"
    nodpi_mark.parent.mkdir(parents=True, exist_ok=True)
    Image.open(mark_512).save(nodpi_mark, "PNG")
    splash_mark = APP_RES / "drawable-nodpi/splash_mark.png"
    splash_mark.parent.mkdir(parents=True, exist_ok=True)
    Image.open(mark_512).save(splash_mark, "PNG")

    # Adaptive layers at xxxhdpi (108dp × 4). drawable-nodpi avoids mdpi scaling.
    # Foreground = H1 master inset to the 72dp safe zone on the 108dp canvas.
    rsvg(FG_SVG, APP_RES / "drawable-nodpi/ic_launcher_foreground.png", ADAPTIVE_DP * 4)
    rsvg(BG_SVG, APP_RES / "drawable-nodpi/ic_launcher_background.png", ADAPTIVE_DP * 4)

    # Android 12+ splash icon: 240dp canvas, 108dp H1 centered (96–120dp lock).
    splash_scale = 4
    splash_canvas = Image.new(
        "RGBA",
        (SPLASH_ICON_DP * splash_scale, SPLASH_ICON_DP * splash_scale),
        (0, 0, 0, 0),
    )
    splash_icon_mark = Image.open(mark_512).resize(
        (SPLASH_MARK_DP * splash_scale, SPLASH_MARK_DP * splash_scale),
        Image.Resampling.LANCZOS,
    )
    off = (SPLASH_ICON_DP - SPLASH_MARK_DP) * splash_scale // 2
    splash_canvas.paste(splash_icon_mark, (off, off), splash_icon_mark)
    splash_icon = APP_RES / "drawable-nodpi/splash_icon.png"
    splash_icon.parent.mkdir(parents=True, exist_ok=True)
    splash_canvas.save(splash_icon, "PNG")

    for name, scale in DENSITIES.items():
        launcher = APP_RES / f"mipmap-{name}/ic_launcher.png"
        rsvg(MARK_SVG, launcher, int(LAUNCHER_DP * scale))
        circle_crop(launcher, APP_RES / f"mipmap-{name}/ic_launcher_round.png")

    mark_512.unlink()
    print(
        "Rasterized H1 Stream Acrylic: launcher, "
        f"{ADAPTIVE_SAFE_DP}dp-safe adaptive, splash {SPLASH_MARK_DP}dp.",
    )


if __name__ == "__main__":
    main()
