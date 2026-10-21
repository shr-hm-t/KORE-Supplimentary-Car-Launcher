# Release notes

## 1.1 (compared with 1.0)

- Offline Mapsforge map only (online maps removed), new thin "line art" map style, stronger vignette, tile gaps and icon glow (all adjustable in the theme).
- Configurable bottom bar (radio, map/YouTube, navigation, music, dialer, car app, day/night, apps, settings).
- PIP off = simple full-width layout: no 360 window, no black dock; radio and map/YouTube switch move into the contacts bar.
- Faster start: the launcher shows its background first, then builds the screen and loads the map.
- Floating second button can be Back (default; falls back to Home where the unit does not allow it) or Home.
- Per-app rules (Usage access, no accessibility service): choose the apps where the black frame and floating buttons show; optional touch shield for one app (e.g. the 360 app).
- Welcome guide: overlay permission, notification access, usage access.
- Navigation info in readable dark boxes; Navigate button; settings backup/restore; status bar looks; YouTube embed fixes (no more intent: errors); call hang-up; sea-level altitude.
- New defaults: 360 window 39.8 x 51.5, black bar 6.5, margin 8, corner 60, auto-360 speed 5-10 km/h, 360 retry 4 s, radio 1 s.

## 1.0 (compared with 0.2)

**Layout**
- New home screen: map / widget area on top, a 15% bar with the 360 camera and three contacts (right of the 360 window), and a black bottom dock (clock under the 360 window, icons: Radio, map/YouTube switch, theme sun/moon, app drawer, settings).
- The play button moved from the icon grid into the YouTube widget.
- The 360 camera window slot now shows a summary behind it: date, heading, altitude, GPS accuracy.
- Calibration screen with live guides: 360 window width and height, black bar height, bar height, frame margin, corner size.

**Screen frame and floating buttons**
- The black frame is now a real 4-corner overlay above every app (touches pass through).
- Bigger neon Mute and Home buttons. Default side follows the driver seat, optional lock, colours, size.
- Mute now has five methods; the default is "like the system mute key".

**Map**
- Engines: OpenStreetMap, offline Mapsforge, Google Maps web page, none.
- Map zoom, map text size, and a vignette with adjustable strength.
- Navigation info test (Settings > Map) shows the raw notification data for troubleshooting.

**YouTube widget**
- Choose notification mode (Open YouTube button) or embedded page. Thumbnail only, play/pause on the thumbnail, the thumbnail stays.

**360 camera**
- Opens automatically when you drive slowly (GPS speed), with time-gap or re-arm options and a touch pause. Optional second try after boot.

**Calling**
- Choose the account (Bluetooth or SIM) for calls.
- Steering-wheel voice button: pick "Vezel Call" in the unit's voice list to call the primary contact.

**Look**
- Radio-style look: gradient, soft colour blobs, vignette, dark translucent tiles, orange accent.
- Settings menus are now side-tab panels at the top right.
- Contacts show a phone icon with the letter; the primary is in the accent colour, the others grey.
- Everything is themable with one JSON file: export, edit, import (see THEMING.md).
- Settings > About with "Made with Claude".

**Start-up**
- The launcher is now direct-boot aware: at a cold start it shows a black screen straight away (instead of "Android is starting") and loads the full home screen as soon as the unit unlocks.

**Removed**: fancy look (replaced by the theme file), three-corner screen rounding.

Version is now 1.0 (versionCode 4).
