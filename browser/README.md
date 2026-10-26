# The Way of Life: browser version

A single web page version of the rosary board for people who can't install the Android APK (iPhone, iPad, Mac, Windows, Linux). It behaves the same as the Android app:

- Right arrow (or the keyboard Right key) lights the next bead. From the last bead it wraps back to only the first bead lit.
- Left arrow (or the keyboard Left key) turns off the current bead. From the first bead it wraps to the last bead with every bead lit.
- Bead 0 at the bottom is always lit.
- The current position is saved in the browser's `localStorage` after every move and restored when the page is opened again.

## Files

| File | Purpose |
|---|---|
| `index.html` | The page to share or host. Fully self-contained: the board image and bead map are embedded, so it is the only file needed. Generated; don't edit by hand. |
| `template.html` | Source for `index.html` (layout, styles, glow effect, position rules). |
| `build.py` | Regenerates `index.html` from the template, `../rosary_board_4x_complete_latest.png` and `../rosary_beads.json`. |

## Rebuilding

Run this after changing `template.html`, the board image or the bead map:

```
python -m pip install pillow
python browser/build.py
```

The build shrinks the board to 1800 px wide JPEG (about 1.1 MB) and embeds it. Bead coordinates are fractions of the image size, so they stay aligned as long as the image keeps the same aspect ratio. If the board art changes and beads move, `rosary_beads.json` has to be measured again.

## Sharing it

**Hosting it on a website is the recommended option, and the only reliable one for iPhone and iPad.** Upload `index.html` to any static host (GitHub Pages, Netlify, your own site) and share the link. Saved progress then works in every browser. On iPhone, users can tap Share, then "Add to Home Screen", to get an app-like icon that opens full screen.

**Sending the file itself** works on desktop browsers: double-click `index.html` to open it. Saved progress works in Chrome, Edge and Firefox for files opened this way; test Safari on a Mac before relying on it. On iPhone and iPad, opening an HTML file from Files or Mail usually shows a preview where the buttons and saving may not work (not yet tested on a device), so share a link there instead.

## Limits of saved progress

- Saved per browser, per device. Progress doesn't follow a player from Safari to Chrome or from a phone to a laptop.
- Cleared if the user clears website data, and not kept in private or incognito windows.
- For a page opened from disk, moving or renaming the file can lose the saved position.
