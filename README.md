# Rehome: Launcher for Car

A minimal, car-first home screen for Android head units. Black, grey and cool-white chrome, sharp panels, a live map, one-tap calls and a tyre-pressure slot.

Built and tested on a Honda Vezel (2025) with a Nakamichi NAM5850 aftermarket unit. It should work on other landscape Android head units, but it has not been tested on them yet.

> **Status:** version 1.0. Tested on one car and one unit so far, so other head units may behave differently. Issues and ideas are welcome.

## Features

- **Home screen:** map or YouTube widget on top, a bar with the 360 camera window and three contact tiles, and a black dock with the clock, Radio, map/YouTube switch, theme toggle, app drawer and settings.
- **Map:** an optional 3D view (MapLibre, offline `.mbtiles` vector file) and the default flat offline Mapsforge `.map` map, in a thin "line art" style (or the standard style). Zoom, text size and vignette are adjustable. For turn-by-turn navigation use the Navigate button (Google Maps by default).
- **Navigation info:** reads the navigation notification (Google Maps, Waze, OsmAnd). Settings > Map has a test button.
- **360 camera:** opens automatically when slow (GPS speed), optional retry after boot.
- **YouTube widget:** notification or embedded page, thumbnail only, play/pause.
- **Calls:** one-tap contacts, account chooser (Bluetooth or SIM), steering-wheel voice button through "Rehome Call".
- **Floating buttons and frame:** neon Mute and Home buttons above every app, and a click-through 4-corner black frame.
- **Themes:** the whole look is one JSON file. See THEMING.md.
- **Day and night:** automatic from GPS sunrise and sunset, fixed hours, or always dark.

## Install

1. Open the **Releases** page of this repository and download `Rehome.apk` from the latest release.
2. On the head unit (or phone), allow installing from unknown sources for your file manager or browser, then open the APK. Play Protect on a phone may warn about the app because it asks for powerful permissions (display over other apps, notification access). Choose **Install anyway**.
3. Press Home and choose **Rehome**. Pick **Always**, if offered.
4. Allow **Location** and **Phone** permissions, and the "display over other apps" permission for the floating buttons and frame.
5. Grant **notification access** when asked. It is needed for navigation info and the YouTube thumbnail.
6. Open Settings (gear icon) and choose your radio app, your 360 app and your driver seat.

Updates install over the old version only when both were signed with the same key. If Android refuses an update, uninstall the old version first.

## Using it

| Control | Tap | Hold |
|---|---|---|
| Radio icon | Open the radio app | none |
| 360 window | Open the 360 camera app | Settings |
| Contact tile | Call | Edit contact, set primary |
| Map / YouTube icon | Switch the top area | Search destination |
| Sun / moon icon | Switch day, night, auto | none |
| Drawer icon | All apps | none |
| Gear icon | Settings | none |
| Floating Mute / Home | Mute, go Home (drag to move) | none |

## Themes

The whole look is one JSON file. Export it from Settings > Look, edit it, and import it back. See [THEMING.md](THEMING.md) and `themes/vezel-default-theme.json`.

## Building from source

Plain Android Views, Java 17, AGP 8.5.2, Mapsforge 0.20.0, min SDK 23. You need JDK 17 and Gradle 8.9, then run `gradle assembleRelease`. The repository also has a GitHub Actions workflow, **Build APK**, that builds the app and can publish a release.

## Known limits

- Media keys do not control many radio apps. Head-unit radios often ignore Android media sessions.
- The YouTube thumbnail depends on what the YouTube app shares. It may show only the title.
- Starting the radio from the background may be blocked on newer Android versions.
- The offline map uses the default Mapsforge style, filtered to greyscale. A calmer custom style is a future idea. Offline place search is not available yet.
- Only the Google Maps, Waze and OsmAnd notifications are read for navigation info.

## Privacy

Everything stays on the device. The map is fully offline. The only network request the launcher makes itself is the area-name lookup (your coordinates go to OpenStreetMap's Nominatim), and the YouTube widget loads YouTube. There is no analytics and no account.

## Credits

- [Claude](https://claude.ai) (Anthropic): code, build setup, design iterations, documentation.
- ChatGPT (OpenAI) and Gemini (Google): design ideas and early icons.
- Idea, direction, hardware testing and feedback: the project owner.

Map rendering by [Mapsforge](https://github.com/mapsforge/mapsforge). Map data © OpenStreetMap contributors.

## Disclaimer

An independent hobby project. It is not affiliated with Honda, Nakamichi or any other brand named here. Names and logos belong to their owners.

## License

No licence has been chosen yet, so all rights are reserved until one is added.
