"""Generates the Android launcher icons from app_icon.png (a transparent PNG) in the repo root.

    python -m pip install pillow
    python tools/make_app_icon.py

Writes an adaptive icon (Android 8+: parchment background plus the image as the foreground layer,
kept inside the safe zone so circle and squircle masks never clip it) and legacy square and round
icons for older devices.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app_icon.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"
BACKGROUND = (0xEF, 0xE3, 0xC2, 255)  # parchment, same as the game
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SAFE_ZONE = 66 / 108  # adaptive icon content must fit in the central 66 of 108 dp


def fitted(image, box):
    scale = box / max(image.size)
    return image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.LANCZOS)


def centered(canvas, image):
    canvas.alpha_composite(image, ((canvas.width - image.width) // 2, (canvas.height - image.height) // 2))
    return canvas


def main():
    source = Image.open(SOURCE).convert("RGBA")
    for density, factor in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)

        layer = round(108 * factor)
        foreground = centered(Image.new("RGBA", (layer, layer), (0, 0, 0, 0)), fitted(source, round(layer * SAFE_ZONE)))
        foreground.save(folder / "ic_launcher_foreground.png")

        size = round(48 * factor)
        art = fitted(source, round(size * 0.78))
        square = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(square).rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.18), fill=BACKGROUND)
        centered(square, art).save(folder / "ic_launcher.png")

        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(round_icon).ellipse((0, 0, size - 1, size - 1), fill=BACKGROUND)
        centered(round_icon, fitted(source, round(size * 0.72))).save(folder / "ic_launcher_round.png")

    adaptive = RES / "mipmap-anydpi-v26"
    adaptive.mkdir(parents=True, exist_ok=True)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/parchment" />\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
        '</adaptive-icon>\n'
    )
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (adaptive / name).write_text(xml, encoding="utf-8")
    print(f"Wrote launcher icons for {', '.join(DENSITIES)} and the adaptive icon.")


if __name__ == "__main__":
    main()
