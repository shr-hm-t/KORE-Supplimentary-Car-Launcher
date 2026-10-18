# Vezel Launcher

A minimal, car-first home screen for Android head units. Black, grey and cool-white chrome, sharp panels, a live map, one-tap calls and a tyre-pressure slot.

Built and tested on a Honda Vezel (2025) with a Nakamichi NAM5850 aftermarket unit. It should work on other landscape Android head units, but it has not been tested on them yet.

> **Status:** early, personal project. Expect rough edges. Issues and ideas are welcome.

## Features

- **Live map:** dark, greyscale and low-detail (OpenStreetMap via osmdroid). It re-centres on the car after 10 seconds without a touch and never opens another app when tapped.
- **Offline map:** download the area around you once, then use it without internet.
- **Turn-by-turn info:** reads the Google Maps (also Waze and OsmAnd) navigation notification and shows the turn arrow and instruction in large text.
- **Place name:** city and area from OpenStreetMap (Nominatim).
- **Destination search:** hold the Map button, search, and start navigation in Google Maps.
- **Speed light:** a glow on the top edge of the map turns yellow at 80 km/h and red at 120 km/h. No speed number is shown.
- **One-tap calls:** three contact tiles. Choose direct call (Bluetooth or SIM), open the dial screen, or send a broadcast to a macro app such as MacroDroid.
- **Steering-wheel voice button:** can call your primary contact, depending on what the button sends.
- **YouTube widget:** swaps the map for the current video's thumbnail and title.
- **App picker and drawer:** choose your radio and 360-camera apps from a list. No package names to type.
- **Radio tile:** tap to mute or unmute, hold to open the radio.
- **Tyre-pressure slot:** placeholder, waiting for real data.
- **Display options:**
  - Software rounded screen corners with adjustable radius.
  - Optional status bar with a black background.

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
| Radio tile | Mute / unmute | Open radio |
| Previous, Play, Next | Media keys (Play also unmutes) | none |
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
- Offline download uses OpenStreetMap's public tile server. Keep downloads small and personal, and follow the [tile usage policy](https://operations.osmfoundation.org/policies/tiles/). For heavy use, host your own tiles.
- Only the Google Maps, Waze and OsmAnd notifications are read for navigation info.

## Privacy

Everything stays on the device. The only network requests are map tiles and place lookups to OpenStreetMap (location coordinates are sent for the lookup). There is no analytics and no account.

## Credits

Map data © OpenStreetMap contributors. Maps by [osmdroid](https://github.com/osmdroid/osmdroid).

## License

Add a licence before publishing (MIT is a common, simple choice).
