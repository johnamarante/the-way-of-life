# Codex prompt: Rosary Path Android app (v0.1)

Build a simple native Android app (Kotlin + Jetpack Compose) that shows a painted rosary board on the phone and lets the user light up the beads one at a time with two arrow buttons. This is the foundation for a board game that comes later, so keep the code clean and the bead/position logic separate from the UI.

## Input files (in the repo root)

- `rosary_board_4x_complete_latest.png`: the board image, 2392 x 3840 px, portrait. A rosary path winds from the bottom of the image up a mountain to the top.
- `rosary_beads.json`: the position of every bead on the path, already measured and ordered. 58 beads, index 0 to 57. Index 0 is the start bead at the bottom of the board, index 57 is the last bead at the top. Each bead has:
  - `type`: `"regular"` (53 beads) or `"glory"` (5 larger gold beads, at indexes 10, 21, 32, 43, 54)
  - `x`, `y`: bead center, normalized (x divided by image width, y divided by image height)
  - `r`: bead radius, normalized to image width
- `rosary_bead_index_reference.png`: a half-size copy of the board with every bead numbered. For reference only; do not ship it in the app.

Do not hardcode the bead coordinates in Kotlin. Copy `rosary_beads.json` into the app's assets and load it at startup, so the image and bead map can be swapped later.

## What the app does

1. **Background:** Show the board image full screen in portrait, scaled with "fit" so the whole board is visible and its aspect ratio is kept. Fill any letterbox area with a color that matches the board (a warm parchment tone, around `#EFE3C2`). No cropping, scrolling or zooming in this version.
2. **Two arrow buttons:** Small round buttons (about 48 dp) near the bottom of the screen, one at the left edge and one at the right edge. The left button shows an arrow pointing left; the right button shows an arrow pointing right. Make them semi-transparent so they don't hide the art, and give each a content description ("Back one bead", "Forward one bead").
3. **Position:** The player has a current position, an integer from 0 to 57. The app starts at 0.
4. **Lit beads:** Every bead from index 0 up to and including the current position is lit. Beads after the current position are not lit. Regular and glory beads light up the same way.
5. **Right arrow (advance):** Move forward one bead, lighting it up.
   - If the current position is already 57 (the last bead), pressing right wraps around: the position goes back to 0, so every bead turns off except bead 0.
6. **Left arrow (fall back):** Move back one bead, turning off the bead you were on.
   - Bead 0 is always lit; it never turns off.
   - If the current position is 0, pressing left wraps around the other way: the position goes to 57, so every bead is lit.

Movement is circular in both directions. Example: start at 0 (only bead 0 lit). Press right 3 times: beads 0 to 3 lit. Press left once: beads 0 to 2 lit. From position 57, press right: only bead 0 lit. From position 0, press left: all 58 beads lit (position 57).

## Lighting effect

Make lit beads look like a soft spotlight is shining on them: the bead and a small area around it appear brighter, as if light falls on that spot of the painting. Keep it simple and cheap to draw.

Suggested approach:
- Draw the board image, then draw on top of it with a Compose `Canvas` in the same coordinate space.
- For each lit bead, draw a circle with a radial gradient (warm white, for example `#FFF6D8`, at around 70% alpha in the center fading to fully transparent at the edge). Make the circle about 1.8x the bead radius. Use an additive or screen blend (`BlendMode.Screen` or `BlendMode.Plus`, drawn into an offscreen layer if needed) so the area under it gets brighter instead of being covered by a flat color.
- Give the current bead (the one the player is on) a slightly larger or stronger glow, so it's clear where the player is.
- Animate the change: fade a bead's glow in or out over about 200 ms when it turns on or off.
- Unlit beads stay exactly as they look in the image.

Map bead coordinates to the screen from the actual drawn image rectangle (after "fit" scaling and letterboxing), not from the full screen size: `screenX = imageLeft + x * drawnWidth`, `screenY = imageTop + y * drawnHeight`, `screenR = r * drawnWidth`. Check that glows line up with the painted beads on both tall and wide screens.

## Technical requirements

- Kotlin, Jetpack Compose, Material 3, single activity. Current stable Android Gradle Plugin and Compose BOM.
- `minSdk` 24, `targetSdk` and `compileSdk` set to the current stable API level.
- Lock the activity to portrait.
- App name: "The Way of Life". Package: `com.thewayoflife.rosary` (easy to change later).
- Create the Android project in a new `android/` folder inside this repo. Don't modify or move the original image or JSON files in the repo root; copy what the app needs into its assets.
- **Image memory:** The PNG is large (about 19 MB on disk, about 37 MB once decoded). Don't decode it at full size on the main thread. Either convert a copy to WebP or JPEG at high quality for the app's assets, or decode it with a sample size that fits the screen. Don't change the image's aspect ratio, or the bead coordinates won't line up.
- **Structure for the future game:**
  - A plain Kotlin data model for the board (`Bead` with index, type, x, y, r; `BoardLayout` loaded from JSON).
  - The position and wrap-around rules in a small pure-Kotlin class with `advance()` and `retreat()` functions and no Android dependencies.
  - A `ViewModel` holds the current position so it survives configuration changes.
  - The drawing code only reads the state; it never changes it.
- Unit tests for the position rules: starting state, advance, retreat, retreat from 0 wraps to 57 with all beads lit, advance from 57 wraps to 0, and the lit set after each step.

## Build and deliver

1. Build a debug APK with `./gradlew assembleDebug` (use `gradlew.bat` on Windows) and confirm the build succeeds.
2. Run the unit tests and confirm they pass.
3. Tell me the path of the APK (normally `android/app/build/outputs/apk/debug/app-debug.apk`) and how to install it on a phone with `adb install`.
4. Add a short `android/README.md` covering how to build, how to install, and where the bead data comes from.

If something in these instructions conflicts with what you find, or a requirement can't be met, say so instead of guessing.
