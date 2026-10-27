"""Builds browser/index.html: a single self-contained page with the board, game data, portraits and
scripts embedded.

Usage (from the repo root or this folder):
    python -m pip install pillow
    python browser/build.py
"""
import base64
import io
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT / "tools"))
from build_game_data import build_data  # noqa: E402

IMAGE = ROOT / "rosary_board_4x_complete_latest.png"
TEMPLATE = HERE / "template.html"
OUTPUT = HERE / "index.html"
PAGES = HERE.parent / "docs" / "index.html"  # the copy GitHub Pages publishes
VERSION = ROOT / "VERSION"  # shared with the Android build

# Downscaled for the web; bead coordinates are normalized, so only the aspect ratio has to match.
WEB_WIDTH = 1800
JPEG_QUALITY = 85


def data_uri(path, mime):
    return f"data:{mime};base64," + base64.b64encode(path.read_bytes()).decode("ascii")


def script_json(value):
    # Safe inside a <script> element.
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")


def main():
    image = Image.open(IMAGE).convert("RGB")
    width, height = image.size
    web_height = round(WEB_WIDTH * height / width)
    image = image.resize((WEB_WIDTH, web_height), Image.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=JPEG_QUALITY, optimize=True, progressive=True)
    board_src = "data:image/jpeg;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")

    data = build_data()
    portraits = {s["portrait"]: data_uri(ROOT / "portraits" / s["portrait"], "image/jpeg") for s in data["saints"]}

    html = TEMPLATE.read_text(encoding="utf-8")
    html = html.replace("__VERSION__", VERSION.read_text(encoding="utf-8").strip())
    html = html.replace("__BOARD_ASPECT__", f"{width} / {height}")
    html = html.replace("__BOARD_SRC__", board_src)
    html = html.replace("__GAME_DATA__", script_json(data))
    html = html.replace("__PORTRAITS__", script_json(portraits))
    html = html.replace("__RULES_JS__", (HERE / "rules.js").read_text(encoding="utf-8"))
    html = html.replace("__GAME_JS__", (HERE / "game.js").read_text(encoding="utf-8"))
    OUTPUT.write_text(html, encoding="utf-8")
    PAGES.parent.mkdir(exist_ok=True)
    PAGES.write_text(html, encoding="utf-8")
    print(f"Wrote {OUTPUT} and {PAGES} ({OUTPUT.stat().st_size // 1024} KB, {len(data['saints'])} saints, {len(portraits)} portraits)")


if __name__ == "__main__":
    main()
