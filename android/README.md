# The Way of Life: Android

Native Android version of the game (Kotlin, Jetpack Compose). The rules are in `../the-way-of-life-rules.md`. It requires JDK 17 and Android SDK 37.

## Code layout

| Path | What it holds |
|---|---|
| `app/src/main/java/.../data/GameData.kt` | Data model and loader for `game-data.json` |
| `app/src/main/java/.../game/Rules.kt` | Game rules: board, streak, cards, name matching, prayers, dilemmas, penance. Mirrors `browser/rules.js`. |
| `app/src/main/java/.../game/Persistence.kt` | Save slot, Codex and options in app-private storage |
| `app/src/main/java/.../ui/GameViewModel.kt` | Game flow: which challenge is open and what happens next |
| `app/src/main/java/.../ui/Challenges.kt` | Question, Saint Challenge, Prayer, Dilemma modals and the saint card bar |
| `app/src/main/java/.../ui/Panels.kt` | Menu, Options, Codex, About, Victory |
| `app/src/main/java/.../ui/Board.kt` | Board drawing and bead glows |
| `app/src/test/.../game/RulesTest.kt` | Unit tests for the rules, using the real game data |

## Game data

`app/src/main/assets/game-data.json` and `app/src/main/assets/portraits/` are generated from the CSVs, `rosary_beads.json`, `saint_portrait_sources.md` and `portraits/` in the repo root. After changing any of those, regenerate them from the repo root:

```
python tools/build_game_data.py
```

`rosary_board.jpg` is a 2000 px wide copy of the board image, large enough to stay sharp when the game zooms in; bead coordinates are normalized, so only the aspect ratio has to match.

## Build and test

From this `android` directory on Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The APK is copied to `dist/the-way-of-life-v-<version>.apk` (for example `dist/the-way-of-life-v-1-12-1.apk`). The version comes from the `VERSION` file in the repo root, shared with the browser build. When you change it, also raise `versionCode` in `app/build.gradle.kts` so phones accept the new APK over the old one.

## App icon

The launcher icon is generated from `app_icon.png` in the repo root. After replacing that image, run from the repo root:

```
python tools/make_app_icon.py
```

## Install

Enable USB debugging on the phone, connect it, and run:

```powershell
adb install -r dist/the-way-of-life-v-1-12-1.apk
```
