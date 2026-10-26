# The Way of Life — Android

Native Android v0.1 of the Rosary Path board. It requires JDK 17 and Android SDK 37.

## Build and test

From this `android` directory on Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install

Enable USB debugging on the phone, connect it, and run:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Board assets

`app/src/main/assets/rosary_beads.json` is copied from the repository root. It contains the ordered, normalized bead centers and radii. The original board PNG is left untouched; the app ships a smaller, high-quality JPEG copy named `rosary_board.jpg` to reduce download and decoded-memory cost while preserving the original aspect ratio. Coordinates are mapped into the fitted image rectangle at runtime, including any letterboxing.
