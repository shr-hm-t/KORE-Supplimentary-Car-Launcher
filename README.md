# Vezel Launcher

A minimal, car-first home screen for Android head units. Black, grey and cool-white chrome, sharp panels, a live map, one-tap calls and a tyre-pressure slot.

Built and tested on a Honda Vezel (2025) with a Nakamichi NAM5850 aftermarket unit. It should work on other landscape Android head units, but it has not been tested on them yet.

> **Status:** version 1.0, first release. Built and tested on one car, so expect rough edges. Issues and ideas are welcome.

## Features

- **Home screen:** map or YouTube widget on top, a bar with the 360 camera window and three contact tiles, and a black dock with the clock, Radio, map/YouTube switch, theme toggle, app drawer and settings.
- **Maps:** OpenStreetMap, offline Mapsforge `.map`, the Google Maps web page, or none. Zoom, text size and vignette are adjustable.
- **Navigation info:** reads the navigation notification (Google Maps, Waze, OsmAnd). Settings > Map has a test button.
- **360 camera:** opens automatically when slow (GPS speed), optional retry after boot.
- **YouTube widget:** notification or embedded page, thumbnail only, play/pause.
- **Calls:** one-tap contacts, account chooser (Bluetooth or SIM), steering-wheel voice button through "Vezel Call".
- **Floating buttons and frame:** neon Mute and Home buttons above every app, and a click-through 4-corner black frame.
- **Themes:** the whole look is one JSON file. See THEMING.md.
- **Day and night:** automatic from GPS sunrise and sunset, fixed hours, or always dark.

## Install

1. Download the APK from the **Actions** tab. Open the latest green run and download the **VezelLauncher-apk** artifact.
2. Unzip it and install `app-debug.apk` on the head unit.
3. Press Home and choose **Vezel Launcher**. Pick **Always**, if offered.
4. Allow **Location** and **Phone** permissions.
5. Tap the hint on the map to grant **notification access**. This is needed for navigation info and the YouTube thumbnail.
6. Tap the gear icon, then choose your radio app and your 360 app.

## Controls

| Control | Tap | Hold |
|---|---|---|
| Radio tile | Open radio | Settings |
| 360 tile | Open 360 app | Settings |
| Contact tile | Call | Edit contact, set primary |
| YouTube icon | Switch map and thumbnail | none |
| Drawer icon | All apps | none |
| Map icon | Open Google Maps | Search destination |
| Gear icon | Settings | none |

## Build it yourself

No computer is needed. Fork this repo and push a change, and GitHub Actions builds the APK. To build locally you need JDK 17 and Gradle 8.9, then run `gradle assembleDebug`.

Stack: plain Android Views, Java 17, AGP 8.5.2, osmdroid 6.1.18, min SDK 23.

## Known limits

- Media keys do not control many radio apps. Head-unit radios often ignore Android media sessions.
- The YouTube thumbnail depends on what the YouTube app shares. It may show only the title.
- Starting the radio from the background may be blocked on newer Android versions.
- The online map uses OpenStreetMap's public tile server. Follow the [tile usage policy](https://operations.osmfoundation.org/policies/tiles/), and use the offline map file for heavy or fully private use. Bulk tile downloading is not supported.
- The offline map uses the default Mapsforge style, filtered to greyscale. A calmer custom style is a future idea. Offline place search is not available yet.
- Only the Google Maps, Waze and OsmAnd notifications are read for navigation info.

## Privacy

Everything stays on the device. With the offline map option, the area name lookup and destination search are the only network requests, and you can avoid them by not using those features. With the online map, tile requests and place lookups go to OpenStreetMap (your coordinates are sent for the lookup). There is no analytics and no account.

## Credits

- [Claude](https://claude.ai) (Anthropic): code, build setup, design iterations, documentation.
- ChatGPT (OpenAI) and Gemini (Google): design ideas and early icons.
- Idea, direction, hardware testing and feedback: the project owner.

Map data © OpenStreetMap contributors. Maps by [osmdroid](https://github.com/osmdroid/osmdroid).

## License

Add a licence before publishing (MIT is a common, simple choice).
