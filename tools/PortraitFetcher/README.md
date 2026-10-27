# PortraitFetcher

Downloads every saint portrait listed in `saint_portrait_sources.md` from Wikimedia Commons and saves it to `portraits/` as a 256 x 256 JPEG:

- The image is scaled (down or up) so its longer side is exactly 256 px, keeping its proportions.
- The shorter side is padded evenly with plain black (0, 0, 0). When the leftover space is an odd number of pixels, one side gets one extra pixel.
- Each file is named from the "Save as" column, which matches the `Portrait` column in `Catholic_Saints_Trivia.csv`.

It asks Commons for a 1024 px wide version of each image rather than the original, so the very large originals (one is about 850 megapixels) are never downloaded.

## Requirements

.NET SDK 10 or later. The only package is SkiaSharp (MIT license), restored automatically on first build.

## Usage

Run from anywhere inside the repo:

```
dotnet run --project tools/PortraitFetcher
```

| Option | What it does |
|---|---|
| (none) | Downloads only portraits that are not in `portraits/` yet. Safe to re-run after a failure. |
| `--force` | Downloads and overwrites every portrait. |
| `--only anne.jpg` | Downloads just that one portrait (combine with `--force` to replace it). |
| `--convert <image> <output.jpg>` | No download: letterboxes a local image file, for example a replacement you found yourself. |
| `--sources <file>` / `--out <folder>` | Use a different source list or output folder. |

To swap a portrait for a different image, change its link in `saint_portrait_sources.md` and run with `--only <name> --force`.

The app pauses one second between downloads and retries when Wikimedia asks it to slow down. Files are written to a `.part` file first, so an interrupted run never leaves a half-written JPEG.
