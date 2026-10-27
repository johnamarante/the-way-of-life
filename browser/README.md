# The Way of Life: browser version

A single web page version of the game for people who can't install the Android APK (Mac, Windows, Linux, and iPhone or iPad at their own risk). It plays the same game as the Android app; the rules are in `../the-way-of-life-rules.md`.

The game is saved in the browser's `localStorage` after every move and can be resumed from the main menu. Three keys are used: `wayoflife.save` (the one save slot, cleared on victory), `wayoflife.codex` (Codex progress, kept across games) and `wayoflife.options` (age level, easy prayers, advanced questions, text size and challenge window size).

## Files

| File | Purpose |
|---|---|
| `index.html` | The page to share or host. Fully self-contained: the board, game data, saint portraits and scripts are embedded, so it is the only file needed (about 4.5 MB). Generated; don't edit by hand. |
| `template.html` | Page layout and styles. |
| `rules.js` | Game rules with no page code: board, streak, cards, name matching, prayers, dilemmas, penance. Mirrors the Android `Rules.kt`. |
| `rules.test.js` | Tests for `rules.js`, using the real game data. |
| `game.js` | Screens, modals, timers, saving and the Codex. |
| `build.py` | Regenerates `index.html` from the files above, the board image, the CSVs (through `../tools/build_game_data.py`) and `../portraits/`. |

## Rebuilding

Run this from the repo root after changing any of the files above, the CSVs, the bead map or the portraits:

```
python -m pip install pillow
python tools/build_game_data.py
python browser/build.py
node browser/rules.test.js
```

The first command refreshes the shared game data (also used by Android), the second builds the page, and the third runs the rules tests. The version shown on the main menu comes from the `VERSION` file in the repo root, shared with the Android build. The build shrinks the board to an 1800 px wide JPEG; bead coordinates are fractions of the image size, so they stay aligned as long as the image keeps the same aspect ratio.

## Sharing it

**Sending the file** works on desktop browsers: double-click `index.html` to open it. Saving worked in Chrome and Edge for files opened this way with the earlier bead-lighting version; Firefox should too; test Safari on a Mac before relying on it.

**Hosting it** on any static site (GitHub Pages, Netlify, your own site) is the most reliable option for every browser, including iPhone and iPad, where opening an HTML file from Files or Mail usually shows a preview that does not run the game.

## Limits of saved progress

- Saved per browser, per device. Progress doesn't follow a player from Safari to Chrome or from a phone to a laptop.
- Cleared if the user clears website data, and not kept in private or incognito windows.
- For a page opened from disk, moving or renaming the file can lose the saved game.

## Publishing on GitHub Pages

`build.py` also writes the same page to `docs/index.html`. GitHub Pages publishes the `docs/` folder of the `master` branch, so after rebuilding, commit and push `docs/index.html` and the site updates within a minute or two at https://johnamarante.github.io/the-way-of-life/.
