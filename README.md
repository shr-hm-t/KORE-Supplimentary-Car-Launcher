# Rehome: Launcher for Car

[](assets/image.png)

A simple home screen for Android car head units: offline map, 360 camera window, radio, one-tap contacts, and a dark glass look.

Built and tested on one car with a Nakamichi NAM5850 unit. Other landscape units may behave differently.

## Features

- Offline map (Mapsforge, optional 3D with MapLibre `.mbtiles`)
- 360 camera window, radio and media widget
- One-tap contacts, steering-wheel voice button through "Rehome: Call Plugin"
- Floating Home button and a black frame over other apps
- Speed glow along the top of the screen (yellow from 80 km/h, red from 120 km/h)
- Dark or light theme, theme file export and import (see [THEMING.md](THEMING.md))

## Install

1. Download `Rehome.apk` from the latest [release](../../releases).
2. Allow installing from unknown sources, open the APK and choose **Install anyway** if Play Protect warns.
3. Press Home and choose **Rehome**, then follow the welcome screen (home app, display over other apps, notification access).
4. Open settings (gear icon). The **Guide** tab explains the rest.

If an update is refused, uninstall the old version first.

## Build

Java 17, Gradle 8.9. Run `gradle assembleRelease`, or use the **Build APK** workflow in GitHub Actions.

## Extras

- [`boot`](boot): boot logo and boot animation preview
- [`docs/ASSET_PROMPTS.md`](docs/ASSET_PROMPTS.md) and [`docs/ASSET_PROMPT_ONE_SHEET.md`](docs/ASSET_PROMPT_ONE_SHEET.md): prompts for making icons and UI art
- [`docs/SHOWCASE_PROMPTS.md`](docs/SHOWCASE_PROMPTS.md): prompts for showcase pictures

## Privacy

Everything stays on the device. The only network request the launcher makes is the area-name lookup (OpenStreetMap Nominatim). No analytics, no account.

## Credits

Claude (Anthropic) is the lead builder and designer. Google AI creates images and media. ChatGPT and Grok helped in a minor way. Idea, direction and testing: the project owner. Map rendering by [Mapsforge](https://github.com/mapsforge/mapsforge); map data © OpenStreetMap contributors.

An independent hobby project, not affiliated with any brand named here. No licence has been chosen yet, so all rights are reserved.
