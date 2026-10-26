"""Builds browser/index.html: a single self-contained page with the board image and bead map embedded.

Usage (from the repo root or this folder):
    python -m pip install pillow
    python browser/build.py
"""
import base64
import io
import json
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
IMAGE = ROOT / "rosary_board_4x_complete_latest.png"
BEADS = ROOT / "rosary_beads.json"
TEMPLATE = HERE / "template.html"
OUTPUT = HERE / "index.html"

# Downscaled for the web; bead coordinates are normalized, so only the aspect ratio has to match.
WEB_WIDTH = 1800
JPEG_QUALITY = 85


def main():
    image = Image.open(IMAGE).convert("RGB")
    width, height = image.size
    web_height = round(WEB_WIDTH * height / width)
    image = image.resize((WEB_WIDTH, web_height), Image.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=JPEG_QUALITY, optimize=True, progressive=True)
    board_src = "data:image/jpeg;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")

    beads = json.loads(BEADS.read_text(encoding="utf-8"))["beads"]
    beads = [{"x": b["x"], "y": b["y"], "r": b["r"], "type": b["type"]} for b in beads]

    html = TEMPLATE.read_text(encoding="utf-8")
    html = html.replace("__BOARD_ASPECT__", f"{width} / {height}")
    html = html.replace("__BOARD_SRC__", board_src)
    html = html.replace("__BEADS_JSON__", json.dumps(beads, separators=(",", ":")))
    OUTPUT.write_text(html, encoding="utf-8")
    print(f"Wrote {OUTPUT} ({OUTPUT.stat().st_size // 1024} KB, {len(beads)} beads)")


if __name__ == "__main__":
    main()
